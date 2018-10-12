local M = {}

nolfSocketQueue = {} -- We want this to be global for the car communication

local socket = require("socket.socket")

local bindhost = "localhost"
local bindport = 23512

local tcp_socket = nil

local clients_read = {}
local clients_write = {}

local handlers = {}

local _log = log
local function log(level, message)
	_log(level, 'nolf', message)
end

log('I', 'Starting up AI testing extension.')

local function sendVehicleCommand(command)
	log('D', 'Sending car command: '..command)
	be:getPlayerVehicle(0):queueLuaCommand(command)
end

local function steer(command, param, read, write)
	local carCommand = "input.event('steering', "..param..", 1)"
	sendVehicleCommand(carCommand)
end

local function throttle(command, param, read, write)
	local carCommand = "input.event('throttle', "..param..", 2)"
	sendVehicleCommand(carCommand)
end

local function getSteering(command, param, read, write)
	local carCommand = "obj:queueGameEngineLua('table.insert(nolfSocketQueue, \"steering:'..input.state.steering.val..'\")')"
	sendVehicleCommand(carCommand)
end

local function getThrottle(command, param, read, write)
	local carCommand = "obj:queueGameEngineLua('table.insert(nolfSocketQueue, \"throttle:'..input.state.throttle.val..'\")')"
	sendVehicleCommand(carCommand)
end

local function clientPostStartMission(mission)
	log('I', 'Map and car loaded, binding local socket.')

	handlers["steer"] = steer
	handlers["throttle"] = throttle
	handlers["getsteering"] = getSteering
	handlers["getthrottle"] = getThrottle
	local handler = handlers["throttle"]

	tcp_socket = socket.tcp()
	res, err = tcp_socket:bind(bindhost, bindport)
	if res == nil then
		log('E', 'Error creating socket: '..err)
	end
	tcp_socket:settimeout(0, 't')
	tcp_socket:listen()
	log('I', 'Bound socket to local port: '..bindport)
end

local function receive(c)
	local line, err = c:receive()
	if err then
		log('E', 'Error whilst reading from socket: '..tostring(error))
		return
	end

	log('D', 'Got line from socket: "'..line..'"')
	return line
end

local function checkNewConnections()
	while true do
		local new_client = tcp_socket:accept()
		if new_client then
			log('D', 'Accepted new client on socket: '..tostring(new_client))
			table.insert(clients_read, new_client)
			table.insert(clients_write, new_client)
			return true
		else
			return false
		end
	end
end

local function checkSocketCommands()
	local read, write, _ = socket.select(clients_read, clients_write, 0)

	for _, c in ipairs(read) do
		if write[c] == nil then
			goto continue
		end

		c:settimeout(0.1, 't')

		local message = receive(c)
		local command = message
		local param = nil
		local split = string.find(message, ':')
		if split ~= nil then
			command = string.sub(message, 0, split - 1)
			param = string.sub(message, split + 1)
		end

		local handler = handlers[command]
		if handler ~= nil then
			handler(command, param, read, write)
		else
			log('W', 'No handler known for socket command: "'..command..'"')
		end

		::continue::
	end
end

local function checkPendingSocketOutput()
	for i, m in ipairs(nolfSocketQueue) do
		for _, c in ipairs(clients_write) do
			log('D', 'Sending pending socket message: '..m)
			c:send(m..'\n')
		end
		table.remove(nolfSocketQueue, i)
	end
end

local function update(dt)
	log('I', 'Updating Nolf!')
	if tcp_socket then
		accepted = checkNewConnections()
		checkSocketCommands()
		checkPendingSocketOutput()
	end
end

M.onInit = init
M.onClientPostStartMission = clientPostStartMission
M.onUpdate = update


return M
