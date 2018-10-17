-- This Source Code Form is subject to the terms of the bCDDL, v. 1.1.
-- If a copy of the bCDDL was not distributed with this
-- file, You can obtain one at http://beamng.com/bCDDL-1.1.txt

local M = {}

local max = math.max
local min = math.min

local wheelsL = {}
M.damage = 0
M.damageExt = 0
M.lowpressure = false
M.deformGroupDamage = {}

local delayedPrecompBeams = {}
local initTimer = 0

local collTriState = {}

local wheelBrokenBeams = {}

local beamDamageTracker = {}
local beamDamageTrackerDirty = false

local breakGroupCache = {}
local triangleBreakGroupCache = {}
local couplerCache = {}

local autoCouplingActive = false
local autoCouplingNodeTag = nil
local autoCouplingTimer = 0
local autoCouplingTimeoutTimer = 0

local attachedCouplers = {}
local transmitCouplers = {}
local recievedElectrics = {}
local hasActiveCoupler = false
local soundTimer = 0
local skeletonStateTimer = 0.25

local beamBodyPartLookup = nil
local invBodyPartBeamCount = nil
local bodyPartDamageTracker = nil

local function breakMaterial(beam)
  material.switchBrokenMaterial(beam)
end

local function broadcastCouplerVisibility(visible)
  BeamEngine:queueAllObjectLua("beamstate.setCouplerVisiblityExternal(" .. tostring(obj:getID()) .. "," .. tostring(visible) .. ")")
end

M.debugDraw = nop
local function debugDraw(focusPos)
  -- highlight all coupling nodes
  for _, c in pairs(couplerCache) do
    obj.debugDrawProxy:drawSphere(0.15, obj:getPosition() + obj:getNodePosition(c.cid), c.couplerColor)
  end
end

local function setCouplerVisiblityExternal(sourceObjectID, visible)
  if visible then
    M.debugDraw = debugDraw
  else
    M.debugDraw = nop
  end
end

local function activateAutoCoupling(_nodetag)
  if not hasActiveCoupler then
    return
  end
  autoCouplingNodeTag = _nodetag
  autoCouplingActive = true
  autoCouplingTimeoutTimer = 0
  autoCouplingTimer = 0
  broadcastCouplerVisibility(true)
end

local function disableAutoCoupling()
  autoCouplingActive = false
  autoCouplingNodeTag = nil
  autoCouplingTimeoutTimer = 0
  autoCouplingTimer = 0
  broadcastCouplerVisibility(false)
end

local function sendObjectCouplingChange()
  obj:queueGameEngineLua(string.format("onObjectCouplingChange(%s,%s)", objectId, serialize(attachedCouplers)))
end

local function attachCouplers(_nodetag)
  local nodetag = _nodetag or ""
  for _, val in pairs(couplerCache) do
    if (val.couplerWeld ~= true and val.couplerTag and (_nodetag == nil or val.couplerTag == nodetag)) and val.cid then
      obj:attachCoupler(val.cid, val.couplerTag or "", val.couplerStrength or 1000000, val.couplerRadius or 0.2, val.couplerTargets or 0)
    end
  end
end

local function detachCouplers(_nodetag)
  local nodetag = _nodetag or ""
  for _, val in pairs(couplerCache) do
    if (val.couplerLock ~= true and val.couplerWeld ~= true and val.couplerTag and (_nodetag == nil or val.couplerTag == nodetag)) and val.cid then
      obj:detachCoupler(val.cid)
      obj:queueGameEngineLua(string.format("onCouplerDetach(%s,%s)", obj:getID(), val.cid))
    end
  end
end

-- this is called on keypress (L)
local function toggleCouplers()
  if autoCouplingActive then
    obj:stopLatching()
    disableAutoCoupling()
  else
    if tableSize(attachedCouplers) > 0 then
      detachCouplers()
    else
      activateAutoCoupling()
    end
  end
end

local function couplerFound(nodeId, obj2id, obj2nodeId)
  --print(string.format("coupler found %s.%s->%s.%s", obj:getID(),nodeId,obj2id, obj2nodeId))
end

M.updateRemoteElectrics = nop
local function updateRemoteElectrics()
  for i = 1, #recievedElectrics do
    tableMerge(electrics.values, recievedElectrics[i])
  end
  recievedElectrics = {}
end

local function couplerAttached(nodeId, obj2id, obj2nodeId)
  disableAutoCoupling()
  attachedCouplers[nodeId] = {obj2id = obj2id, obj2nodeId = obj2nodeId}

  -- figure out the electrics state
  local node = v.data.nodes[nodeId]
  if node and (node.importElectrics or node.importInputs) then
    local data = {electrics = node.importElectrics, inputs = node.importInputs}
    --print("couplerAttached -> beamstate.exportCouplerData("..tostring(obj2nodeId)..", "..serialize(data)..")")
    obj:queueObjectLuaCommand(obj2id, "beamstate.exportCouplerData(" .. tostring(obj2nodeId) .. ", " .. serialize(data) .. ")")
    M.updateRemoteElectrics = updateRemoteElectrics
  end

  --print(string.format("coupler attached %s.%s->%s.%s", obj:getID(),nodeId,obj2id, obj2nodeId))
  if objectId <= obj2id then
    obj:queueGameEngineLua(string.format("onCouplerAttached(%s,%s,%s,%s)", objectId, obj2id, nodeId, obj2nodeId))
  end
end

local function couplerDetached(nodeId, obj2id, obj2nodeId)
  --print(string.format("coupler detached %s.%s->%s.%s", obj:getID(),nodeId,obj2id, obj2nodeId))
  attachedCouplers[nodeId] = nil
  transmitCouplers[nodeId] = nil
  if objectId <= obj2id then
    obj:queueGameEngineLua(string.format("onCouplerDetached(%s,%s)", objectId, obj2id))
  end
end

local function getCouplerOffset()
  local couplerCacheSize = (couplerCache and #couplerCache) or 0
  if couplerCacheSize == 0 then
    return
  end
  local pos = v.data.nodes[couplerCache[couplerCacheSize].cid].pos --obj:getNodePosition(couplerCache[1].cid) this is live position
  local ref = v.data.nodes[v.data.refNodes[0].ref].pos
  local cOff = {}
  for _, c in pairs(couplerCache) do
    pos = v.data.nodes[c.cid].pos
    cOff[c.cid] = {x = pos.x - ref.x, y = pos.y - ref.y, z = pos.z - ref.z}
  end
  obj:queueGameEngineLua(string.format("core_trailerRespawn.addCouplerOffset(%s,%s)", obj:getId(), dumps(cOff)))
end

-- called from the vehicle that wants to import electrics
local function exportCouplerData(nodeid, dataList)
  --print(obj:getID().."<-exportCouplerData("..nodeid..','..dumps(dataList)..')')
  if attachedCouplers[nodeid] == nil then
    log("E", "beamstate.exportCouplerElectrics", "unable to export electrics: known coupled node: " .. tostring(nodeId))
    return
  end
  transmitCouplers[nodeid] = attachedCouplers[nodeid]
  transmitCouplers[nodeid].exportElectrics = dataList.electrics
  transmitCouplers[nodeid].exportInputs = dataList.inputs
end

-- called by the host that provides the electrics
local function importCouplerData(nodeId, data)
  --print(obj:getID().."<-importCouplerData("..nodeId..','..dumps(data)..')')
  if data.electrics then
    table.insert(recievedElectrics, data.electrics)
  end
  if data.inputs then
    for k, v in pairs(data.inputs) do
      input.event(k, v, 2)
    end
  end
end

-- local helper function
local function luaBreakBeam(id)
  beamDamageTracker[id] = 1
  beamDamageTrackerDirty = true
end

local function sendUISkeletonState()
  if not playerInfo.firstPlayerSeated then
    return
  end
  guihooks.trigger("VehicleSkeletonState", beamDamageTracker)
end

local function deflateTire(wheelid, energy)
  local wheel = wheelsL[wheelid]
  M.lowpressure = true

  --local i, beam
  --if wheel.reinforcementBeams ~= nil then
  --    for i, beam in pairs (wheel.reinforcementBeams) do
  --        obj:setBeamSpringDamp(beam.cid, beam.beamSpring * 0.5, -1, beam.beamSpring * 0.02, -1)
  --    end
  --end
  local brokenBeams = wheelBrokenBeams[wheelid] or 1
  if wheel.pressureGroup ~= nil then
    if v.data.pressureGroups[wheel.pressureGroup] ~= nil then
      if brokenBeams > 4 then
        obj:deflatePressureGroup(v.data.pressureGroups[wheel.pressureGroup])
        obj:changePressureGroupDrag(v.data.pressureGroups[wheel.pressureGroup], 0)
      elseif brokenBeams == 1 then
        obj:setGroupPressure(v.data.pressureGroups[wheel.pressureGroup], (0.2 * 6894.757 + 101325))
      end
    end
  end

  if brokenBeams == 1 then
    if wheels.wheels[wheelid] then
      wheels.wheels[wheelid].isTireDeflated = true
    end
    gui.message({txt = "vehicle.beamstate.tireDeflated", context = {wheelName = wheel.name}}, 5, "vehicle.damage.deflated." .. wheel.name)
    damageTracker.setDamage("wheels", "tire" .. wheel.name, true)

    sounds.playSoundOnceAtNode("event:>Vehicle>Tire_Burst", wheel.node1, 1)
    M.damageExt = M.damageExt + 1000
    if wheel.treadBeams ~= nil then
      for _, beam in pairs(wheel.treadBeams) do
        obj:setBeamSpringDamp(beam.cid, beam.beamSpring * 0.05, 0, -1, -1)
      end
    end

    if wheel.sideBeams ~= nil then
      for _, beam in pairs(wheel.sideBeams) do
        obj:setBeamSpringDamp(beam.cid, beam.beamSpring * 0.05, 0, -1, -1)
      end
    end

    if wheel.reinfBeams ~= nil then
      for _, beam in pairs(wheel.reinfBeams) do
        obj:setBeamSpringDamp(beam.cid, 0, 0, 0, 0)
      end
    end

    if wheel.pressuredBeams ~= nil then
      for _, beam in pairs(wheel.pressuredBeams) do
        obj:setBeamPressureRel(beam.cid, 0, math.huge, -1, -1)
      end
    end
  end

  wheelBrokenBeams[wheelid] = brokenBeams + 1
end

local function updateGFX(dt)
  M.damage = obj:getDissipatedEnergy() + M.damageExt

  --crash sounds
  soundTimer = soundTimer + dt
  if soundTimer > 0.05 then
    local impactEnergy, breakEnergy, breakNode = obj:getImpactDeformEnergyNode()
    if impactEnergy > breakEnergy then
      if impactEnergy > 0.001 then
        local vol = math.sqrt(impactEnergy) * 0.2
		--print (math.sqrt(impactEnergy) * 0.2)
        if vol > 0.01 then
          sounds.playSoundOnceFollowNode("event:>Destruction>crash_generic", breakNode, vol)
        end
      end
    else
      if breakEnergy > 0.001 then
        --local vol = (math.sqrt(math.sqrt(breakEnergy)) * 0.075)
		--print (math.sqrt(math.sqrt(breakEnergy)) * 0.075)
		local vol = math.log(breakEnergy + 1) * 0.08
		--print (math.log(breakEnergy + 1) * 0.08)
        if vol > 0.01 then
          sounds.playSoundOnceFollowNode("event:>Destruction>Vehicle>impact_vehicle_generic", breakNode, vol)
        end
      end
    end
    soundTimer = 0
  end

  if beamDamageTrackerDirty then
    skeletonStateTimer = skeletonStateTimer - dt
    if skeletonStateTimer < 0 then
      sendUISkeletonState()
      skeletonStateTimer = 0.25
      beamDamageTrackerDirty = false
    end
  end

  if autoCouplingActive then
    autoCouplingTimeoutTimer = autoCouplingTimeoutTimer + dt
    if autoCouplingTimeoutTimer > 60 then
      disableAutoCoupling()
    end
    autoCouplingTimer = (autoCouplingActive and autoCouplingTimer <= 0.5) and autoCouplingTimer + dt or 0
    if autoCouplingTimer > 0.5 then
      attachCouplers(autoCouplingNodeTag)
    end
  end

  -- transmit data
  for _, coupler in pairs(transmitCouplers) do
    local data = {}
    if coupler.exportElectrics then
      data.electrics = {}
      for _, v in pairs(coupler.exportElectrics) do
        data.electrics[v] = electrics.values[v]
      end
    end
    if coupler.exportInputs then
      data.inputs = {}
      for _, v in pairs(coupler.exportInputs) do
        data.inputs[v] = electrics.values[v] or input[v]
      end
    end
    obj:queueObjectLuaCommand(coupler.obj2id, string.format("beamstate.importCouplerData(%s, %s)", coupler.obj2nodeId, serialize(data)))
  end
end

M.update = nop
local function update(dtSim)
  local finished_precomp = true
  initTimer = initTimer + dtSim
  for _, b in ipairs(delayedPrecompBeams) do
    local tratio = initTimer / b.beamPrecompressionTime
    if tratio < 1 then
      finished_precomp = false
    end
    obj:setPrecompressionRatio(b.cid, 1 + (b.beamPrecompression - 1) * min(tratio, 1))
  end

  if finished_precomp then
    M.update = nop
    delayedPrecompBeams = {}
  end
end

local function beamBroken(id, energy)
  beamDamageTracker[id] = 0
  beamDamageTrackerDirty = true

  local bodyPart = beamBodyPartLookup[id]
  if bodyPart then
    bodyPartDamageTracker[bodyPart] = bodyPartDamageTracker[bodyPart] + 1
    local damage = bodyPartDamageTracker[bodyPart] * invBodyPartBeamCount[bodyPart]
    if damage > 0.001 then
      damageTracker.setDamage("body", bodyPart, damage)
    end
  end

  luaBreakBeam(id)
  if v.data.beams[id] ~= nil then
    local beam = v.data.beams[id]

    -- Break coll tris
    if beam.collTris and not beam.disableTriangleBreaking then --allow beams to disable triangle breaking
      for _, ctid in pairs(beam.collTris) do
        if collTriState[ctid] then
          collTriState[ctid] = collTriState[ctid] - 1
          if collTriState[ctid] <= 1 then
            obj:breakCollisionTriangle(ctid)
          end
        end
      end
    end

    -- Break the meshes
    if beam.disableMeshBreaking == nil or beam.disableMeshBreaking == false then
      obj:breakMeshes(id)
    end

    -- Break rails
    obj:breakRails(id)

    -- Check for punctured tire
    if beam.wheelID ~= nil then
      deflateTire(beam.wheelID, energy)
    elseif beam.pressureGroupId then
      obj:deflatePressureGroup(v.data.pressureGroups[beam.pressureGroupId])
    end

    -- breakgroup handling
    if beam.breakGroup then
      if type(beam.breakGroup) ~= "table" and breakGroupCache[beam.breakGroup] == nil then
        -- shortcircuit in case of broken single breakGroup
      else
        local breakGroups = type(beam.breakGroup) == "table" and beam.breakGroup or {beam.breakGroup}
        for _, g in ipairs(breakGroups) do
          if breakGroupCache[g] then
            -- hide props if they use
            props.hidePropsInBreakGroup(g)

            -- breakGroupType = 0 breaks the group
            -- breakGroupType = 1 does not break the group but will be broken by the group
            if beam.breakGroupType == 0 or beam.breakGroupType == nil then
              -- break all beams in that group
              local copy = shallowcopy(breakGroupCache[g])
              breakGroupCache[g] = nil
              for _, bcid in ipairs(copy) do
                obj:breakBeam(bcid)
                luaBreakBeam(bcid)
              end

              --break triangle breakgroups matching the beam breakgroup
              if triangleBreakGroupCache[g] then
                for _, ctid in ipairs(triangleBreakGroupCache[g]) do
                  obj:breakCollisionTriangle(ctid)
                  collTriState[ctid] = nil
                end
                triangleBreakGroupCache[g] = nil
              end
            end
          end
        end
      end
    end

    if beam.deformSwitches then
      breakMaterial(beam)
    end
  else
    --print ("beam "..id.." just broke")
  end
end

local function init()
  M.damage = 0
  M.damageExt = 0
  wheelBrokenBeams = {}

  beamDamageTracker = {}
  skeletonStateTimer = 0.25
  beamDamageTrackerDirty = false

  if v.data.wheels then
    wheelsL = v.data.wheels
    for _, wheel in pairs(wheelsL) do
      wheel.pressureCoef = 1
    end
  end

  triangleBreakGroupCache = {}
  local pressureBeams = {}

  -- Reset colltris
  if v.data.triangles then
    collTriState = {}
    for _, t in pairs(v.data.triangles) do
      if t.cid and t.beamCount then
        collTriState[t.cid] = t.beamCount
        --handle triangle breakgroups
        if t.breakGroup then
          local breakGroups = type(t.breakGroup) == "table" and t.breakGroup or {t.breakGroup}
          for _, g in pairs(breakGroups) do
            if not triangleBreakGroupCache[g] then
              triangleBreakGroupCache[g] = {}
            end
            table.insert(triangleBreakGroupCache[g], t.cid)
          end
        end
        if t.pressureGroup then
          pressureBeams[min(t.id1, t.id2) .. "\0" .. max(t.id1, t.id2)] = t.pressureGroup
          pressureBeams[min(t.id1, t.id3) .. "\0" .. max(t.id1, t.id3)] = t.pressureGroup
          pressureBeams[min(t.id2, t.id3) .. "\0" .. max(t.id2, t.id3)] = t.pressureGroup
        end
      end
    end
  end

  breakGroupCache = {}
  M.deformGroupDamage = {}
  delayedPrecompBeams = {}
  initTimer = 0
  M.update = update

  autoCouplingActive = false
  autoCouplingNodeTag = nil
  autoCouplingTimer = 0
  autoCouplingTimeoutTimer = 0

  attachedCouplers = {}
  transmitCouplers = {}
  recievedElectrics = {}
  M.updateRemoteElectrics = nop

  couplerCache = {}
  hasActiveCoupler = false

  local xMin, xMax, yMin, yMax = 0, 0, 0, 0

  for _, n in pairs(v.data.nodes) do
    xMin = min(n.pos.x, xMin)
    xMax = max(n.pos.x, xMax)
    yMin = min(n.pos.y, yMin)
    yMax = max(n.pos.y, yMax)

    if (n.couplerTag or n.tag) and n.cid then
      local data = shallowcopy(n)
      local c = getContrastColor(stringHash(data.couplerTag or data.tag))
      data.couplerColor = color(c.r, c.g, c.b, 150)
      table.insert(couplerCache, data)
      hasActiveCoupler = n.couplerTag ~= nil or hasActiveCoupler
    end
  end

  for _, c in pairs(couplerCache) do
    if c.couplerStartRadius and c.cid then
      obj:attachCoupler(c.cid, c.couplerTag or "", c.couplerStrength or 1000000, c.couplerStartRadius, c.couplerTargets or 0)
    end
  end

  beamBodyPartLookup = {}
  bodyPartDamageTracker = {FL = 0, FR = 0, ML = 0, MR = 0, RL = 0, RR = 0}
  invBodyPartBeamCount = {FL = 0, FR = 0, ML = 0, MR = 0, RL = 0, RR = 0}

  local xRange = xMax - xMin
  local yRange = yMax - yMin
  local yRangeThird = yRange / 3
  local xRangeHalf = xRange * 0.5
  local yGroup1 = yMin + yRangeThird
  local yGroup2 = yGroup1 + yRangeThird
  local xGroup1 = xMin + xRangeHalf

  if v.data.beams then
    for _, b in pairs(v.data.beams) do
      local pid = min(b.id1, b.id2) .. "\0" .. max(b.id1, b.id2)
      if pressureBeams[pid] and v.data.pressureGroups[pressureBeams[pid]] then
        b.pressureGroupId = pressureBeams[pid]
      end

      if b.breakGroup then
        local breakGroups = type(b.breakGroup) == "table" and b.breakGroup or {b.breakGroup}
        for _, g in pairs(breakGroups) do
          if not breakGroupCache[g] then
            breakGroupCache[g] = {}
          end
          table.insert(breakGroupCache[g], b.cid)
        end
      end

      if b.deformGroup then
        local deformGroups = type(b.deformGroup) == "table" and b.deformGroup or {b.deformGroup}
        for _, g in pairs(deformGroups) do
          local group = M.deformGroupDamage[g]
          if not group then
            M.deformGroupDamage[g] = {}
            group = M.deformGroupDamage[g]
            group.eventCount = 0
            group.damage = 0
            group.maxEvents = 0
            group.invMaxEvents = 0
          end
          group.maxEvents = group.maxEvents + 1 / (max(b.deformationTriggerRatio or 1, 0.01))
          group.invMaxEvents = 1 / group.maxEvents
        end
      end

      if type(b.beamPrecompressionTime) == "number" and b.beamPrecompressionTime > 0 then
        table.insert(delayedPrecompBeams, b)
      end

      if not b.wheelID then
        local beamNode1Pos = v.data.nodes[b.id1].pos
        local beamNode2Pos = v.data.nodes[b.id2].pos
        local beamPosX = (beamNode1Pos.x + beamNode2Pos.x) * 0.5
        local beamPosY = (beamNode1Pos.y + beamNode2Pos.y) * 0.5
        local xChar, yChar

        if beamPosY <= yGroup1 then
          yChar = "F"
        elseif beamPosY <= yGroup2 then
          yChar = "M"
        else
          yChar = "R"
        end

        if beamPosX <= xGroup1 then
          xChar = "R"
        else
          xChar = "L"
        end

        local bodyPart = yChar .. xChar
        beamBodyPartLookup[b.cid] = bodyPart
        invBodyPartBeamCount[bodyPart] = invBodyPartBeamCount[bodyPart] + 1
      end
    end
  end

  for k, v in pairs(invBodyPartBeamCount) do
    invBodyPartBeamCount[k] = 1 / v
    damageTracker.setDamage("body", k, 0)
  end
end

-- only being called if the beam has deform triggers
local function beamDeformed(id, ratio)
  --log('D', "beamstate.beamDeformed","beam "..id.." deformed with ratio "..ratio)
  beamDamageTracker[id] = ratio
  beamDamageTrackerDirty = true

  local bodyPart = beamBodyPartLookup[id]
  if bodyPart then
    bodyPartDamageTracker[bodyPart] = bodyPartDamageTracker[bodyPart] + ratio
    local damage = bodyPartDamageTracker[bodyPart] * invBodyPartBeamCount[bodyPart]
    if damage > 0.001 then
      damageTracker.setDamage("body", bodyPart, damage)
    end
  end

  if v.data.beams[id] then
    local b = v.data.beams[id]
    if b.deformSwitches then
      breakMaterial(b)
    end

    if b.deformGroup then
      if type(b.deformGroup) == "table" then
        for _, g in ipairs(b.deformGroup) do
          local group = M.deformGroupDamage[g]
          group.eventCount = group.eventCount + 1
          group.damage = group.eventCount * group.invMaxEvents
        end
      else
        local group = M.deformGroupDamage[b.deformGroup]
        group.eventCount = group.eventCount + 1
        group.damage = group.eventCount * group.invMaxEvents
      end
    end
  end
end

local function reset()
  init()
  M.lowpressure = false
end

local function breakAllBreakgroups()
  for _, b in pairs(v.data.beams) do
    if b.breakGroup ~= nil then
      obj:breakBeam(b.cid)
    end
  end
end

local function breakHinges()
  for _, b in pairs(v.data.beams) do
    if b.breakGroup ~= nil then
      local breakGroups = type(b.breakGroup) == "table" and b.breakGroup or {b.breakGroup}
      -- multiple break groups
      for _, g in pairs(breakGroups) do
        if type(g) == "string" and (string.find(g, "hinge") ~= nil or string.find(g, "latch") ~= nil) then
          --log('D', "beamstate.breakHinges","  breaking hinge beam "..k.. " as in breakgroup ".. b.breakGroup)
          obj:breakBeam(b.cid)
          break
        end
      end
    end
  end
end

local function deflateTires()
  for i, _ in pairs(wheelsL) do
    deflateTire(i, 0)
  end
end

local function breakBreakGroup(group)
  if group == nil then
    return
  end
  for _, b in pairs(v.data.beams) do
    if b.breakGroup ~= nil then
      local breakGroups = type(b.breakGroup) == "table" and b.breakGroup or {b.breakGroup}
      for _, g in pairs(breakGroups) do
        if g == group then
          obj:breakBeam(b.cid)
          break
        end
      end
    end
  end
end

local function triggerDeformGroup(group)
  if group == nil then
    return
  end
  for _, b in pairs(v.data.beams) do
    if b.deformSwitches ~= nil then
      local deformSwitchesT = type(b.deformSwitches) == "table" and b.deformSwitches or {b.deformSwitches}
      for _, g in pairs(deformSwitchesT) do
        if g.deformGroup == group then
          breakMaterial(b)
          return
        end
      end
    end
  end
end

local function addDamage(damage)
  M.damageExt = M.damageExt + damage
end

local function sendUISkeleton()
  local data = {}
  for _, beam in pairs(v.data.beams) do
    local n1 = v.data.nodes[beam.id1]
    local n2 = v.data.nodes[beam.id2]
    -- only beams with deformationTriggerRatio will actually change ...
    --if beam.deformationTriggerRatio then
    data[beam.cid + 1] = {n1.pos, n2.pos}
    --end
  end
  if not playerInfo.firstPlayerSeated then
    return
  end
  guihooks.trigger("VehicleSkeleton", data)
  sendUISkeletonState()
end

local function hasCouplers()
  local couplerCount = 0
  for _, val in pairs(couplerCache) do
    if (val.couplerWeld ~= true and val.couplerTag) and val.cid then
      couplerCount = couplerCount + 1
    end
  end

  return couplerCount > 0
end

local function save(filename)
  if filename == nil then
    filename = v.vehicleDirectory .. "/vehicle.save.json"
  end
  -- TODO: color
  local save = {}
  save.format = "v2"
  save.model = v.vehicleDirectory:gsub("vehicles/", ""):gsub("/", "")
  save.parts = v.userPartConfig
  save.vars = v.userVars
  save.vehicleDirectory = v.vehicleDirectory
  save.nodeCount = #v.data.nodes
  save.beamCount = #v.data.beams
  save.luaState = serializePackages("save")
  save.hydros = {}
  for _, h in pairs(hydros.hydros) do
    table.insert(save.hydros, h.state)
  end

  save.nodes = {}
  for _, node in pairs(v.data.nodes) do
    local d = {
      vec3(obj:getNodePosition(node.cid)):toTable()
    }
    if math.abs(obj:getOriginalNodeMass(node.cid) - obj:getNodeMass(node.cid)) > 0.1 then
      table.insert(d, obj:getNodeMass(node.cid))
    end
    save.nodes[node.cid + 1] = d
  end
  save.beams = {}
  for _, beam in pairs(v.data.beams) do
    local d = {
      obj:getBeamRestLength(beam.cid),
      obj:beamIsBroken(beam.cid),
      obj:getBeamDeformation(beam.cid)
    }
    save.beams[beam.cid + 1] = d
  end
  writeJsonFile(filename, save, true)
end

local function load(filename)
  if filename == nil then
    filename = v.vehicleDirectory .. "/vehicle.save.json"
  end

  local save = readJsonFile(filename)

  -- satefy checks
  if not save or save.nodeCount ~= #v.data.nodes or save.beamCount ~= #v.data.beams or save.vehicleDirectory ~= v.vehicleDirectory or save.format ~= "v2" then
    log("E", "save", "unable to load vehicle: invalid vehicle loaded?")
    return
  end

  importPersistentData(save.luaState)

  for k, h in pairs(save.hydros) do
    hydros.hydros[k].state = h
  end

  for cid, node in pairs(save.nodes) do
    cid = tonumber(cid) - 1
    obj:setNodePosition(cid, vec3(node[1]):toFloat3())
    if #node > 1 then
      obj:setNodeMass(cid, node[2])
    end
  end

  for cid, beam in pairs(save.beams) do
    cid = tonumber(cid) - 1
    obj:setBeamLength(cid, beam[1])
    if beam[2] == true then
      obj:breakBeam(cid)
    end
    if beam[3] > 0 then
      -- deformation: do not call c++ at all, its just used on the lua side anyways
      --print('deformed: ' .. tostring(cid) .. ' = ' .. tostring(beam[3]))
      beamDeformed(cid, beam[3])
    end
  end

  obj:commitLoad()
end

local function getBrokenData(vid, scenarioName, impactedComponentsTable)
	print("VID")
	print(vid)
	print("Scenario_Name")
	print(scenarioName)
	
	--io.output(outfile)
	carDamageRecord = "v" .. vid 
	local hasDamage = false
	
	for k,val in pairs(bodyPartDamageTracker) do
		print("k is ")
		print(k)
		print("v is ")
		print(val)
		if val ~= 0 then
			hasDamage = true
			carDamageRecord = carDamageRecord .. ':' .. k .. '-' .. val
		end
			--for k1,v1 in pairs(v) do
				--print("k1 is " .. k1 .. " v1 is ")
				--print(v1)
			--end
		
	end
	
	local filePath = 'levels\\smallgrid\\damageRecord\\' .. scenarioName .. '-v' .. vid .. '-damageLog.log'
	local outfile = io.open(filePath, "a+")
	print("File is ")
	print(outfile)
	
	if not hasDamage then
		carDamageRecord = "v" .. vid .. ":NA-0"
	end
	
	outfile:write(carDamageRecord)
	outfile:close()
	print(carDamageRecord)
	
end

-- public interface
M.beamBroken = beamBroken
M.reset = reset
M.init = init
M.deflateTire = deflateTire
M.updateGFX = updateGFX
M.beamDeformed = beamDeformed
M.breakAllBreakgroups = breakAllBreakgroups
M.breakHinges = breakHinges
M.deflateTires = deflateTires
M.breakBreakGroup = breakBreakGroup
M.triggerDeformGroup = triggerDeformGroup
M.addDamage = addDamage
M.activateAutoCoupling = activateAutoCoupling
M.couplerFound = couplerFound
M.couplerAttached = couplerAttached
M.couplerDetached = couplerDetached
M.getCouplerOffset = getCouplerOffset
M.setCouplerVisiblityExternal = setCouplerVisiblityExternal
M.exportCouplerData = exportCouplerData
M.importCouplerData = importCouplerData
M.objectCollision = objectCollision
M.hasCouplers = hasCouplers

M.load = load
M.save = save

-- Input
M.toggleCouplers = toggleCouplers
M.attachCouplers = attachCouplers
M.detachCouplers = detachCouplers

-- for the UI
M.requestSkeletonState = sendUISkeletonState
M.requestSkeleton = sendUISkeleton

-- Custom Func
M.getBrokenData = getBrokenData

return M
