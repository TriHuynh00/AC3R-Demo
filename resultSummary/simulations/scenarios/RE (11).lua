local M = {}

local helper = require('scenario/scenariohelper')

local running = false

local checkForscenario_player1Crash = false
local playerInstance = 'scenario_player0'
local scenario_player1CrashTimeout = 0
local scenario_player1CrashMaxTimeout = 7

local scenarioTimeout = 120 -- each unit equivalent to 250ms -> 40 = 10 sec

local scenarioName = scenario_scenarios.getScenario().name

local scenario_player1MinimumSpeed = 1.4 --m/s

local targetSetAlready = false

local function reset()
    checkForscenario_player1Crash = false
    scenario_player1CrashTimeout = 0
	scenarioTimeout = 120
    running = false
end

-- called when countdown finished
local function onRaceStart()
	--be.physicsMaxSpeed = true
    reset()
    running = true
    math.randomseed(os.time())
	
		local arg1 = {vehicleName = 'scenario_player1',
				  waypoints = {'wp1_1','wp_crash'},
				  routeSpeed = 14.304,
				  routeSpeedMode = "limit",
				  aggression = 1, -- aggression here acts as a multiplier to the Ai default aggression i.e. 0.7.
				  aggressionMode = 'normal' -- Aggression decreases with distance from opponent
				  }

	helper.setAiPath(arg1)



	local arg2 = {vehicleName = 'scenario_player2',
				  waypoints = {'wp1_2','wp_crash'},
				  routeSpeed = 9.387,
				  routeSpeedMode = "limit",
				  aggression = 1, -- aggression here acts as a multiplier to the Ai default aggression i.e. 0.7.
				  aggressionMode = 'normal' -- Aggression decreases with distance from opponent
				  }

	helper.setAiPath(arg2)




	
end

-- data { vehicleName = 'string', waypointName = 'string' }
local function onRaceWaypointReached( data )
  local playerVehicleId = be:getPlayerVehicleID(0)
  --if data.vehicleId == playerVehicleId and data.waypointName == 'brwpstartai' then
	helper.flashUiMessage('Reach waypoint ', 2)
	Name('scenario_player1', 'ai.setState({debugMode="speeds"})')
	--helper.trackVehicle('scenario_player1', 'scenario_player1')
  --end
end

local function fail(reason)
  scenario_scenarios.finish({failed = reason})
  reset()
end

local function success()
  local scenario = scenario_scenarios.getScenario()
  if not scenario then return end

  local finalTime = scenario.timer
  local minutes = math.floor(finalTime / 60);
  local seconds = finalTime - (minutes * 60);
  local timeStr = ''
  if minutes > 0 then
      timeStr = string.format("%02.0f:%05.2f", minutes, seconds)
  else
      timeStr = string.format("%0.2f", seconds) .. 's'
  end

  local result = {msg = {txt = 'You win', context={timeLimit=timeStr}}}
  scenario_scenarios.finish(result)
  reset()
end

-- called every 250ms. Use "status.setScenarioFailed()" for return failed test
-- data { dTime = number }
local function onRaceTick(raceTickTime)
	scenarioTimeout = scenarioTimeout - 1

    if not running then
        return true
    end

    local player1Data = map.objects[map.objectNames['scenario_player1']]
	local player2Data = map.objects[map.objectNames['scenario_player2']]
	
	print("Scenario timeout " .. scenarioTimeout)
	
	if scenarioTimeout < 0 then 
		
		
		if not isCrash then
			helper.recordNoCrash(string.format("%s", scenarioName))
			fail('Timed out, Fail!')
			
		else
			success()
		end
	end

	--$OtherVehicleStartToRunCode
	
	local player1Damage = player1Data.damage
	local player2Damage = player2Data.damage
	
    local distanceToscenario_player1 = math.abs((player2Data.pos - player1Data.pos):length())
    local scenario_player1Speed = player1Data.vel:length()
	
	if distanceToscenario_player1 <= 5 then
		
		
		if player1Damage > 100 or player2Damage > 100 then
			--print("player1Damage " .. player1Damage)
			helper.setAiMode('scenario_player1', 'stop')
			helper.setAiMode('scenario_player2', 'stop')
			
			if not isCrash then
				--helper.queueLuaCommandByName('scenario_player1', 'damageTracker.getDamageData()')
				helper.queueLuaCommandByName('scenario_player1', 'beamstate.getBrokenData(1, "' .. scenarioName .. '")')
				helper.queueLuaCommandByName('scenario_player2', 'beamstate.getBrokenData(2, "' .. scenarioName .. '")')
				
				isCrash = true
			end
			
			--success()
		end
	elseif distanceToscenario_player1 > 5 then
		isCrash = false
		--helper.queueLuaCommandByName('scenario_player1', 'damageTracker.getDamageData()')
	end
	
end

M.onRaceStart = onRaceStart
M.onRaceWaypointReached = onRaceWaypointReached
M.onRaceTick = onRaceTick

return M

