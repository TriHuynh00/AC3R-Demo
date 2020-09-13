import os
from beamngpy import BeamNGpy, Scenario, Vehicle
from beamngpy.sensors import Damage

def main():
	# Get environment variables
	BNG_HOME = os.getenv('BNG_HOME')
	BNG_RESEARCH = os.getenv('BNG_RESEARCH')
	host = 'localhost'
	port = 64256

	# Instantiates a BeamNGpy instance
	bng = BeamNGpy(host, port, BNG_HOME, BNG_RESEARCH)

	# Find a scenario in the smallgrid level called 'Case4'
	level = 'smallgrid'
	scenario_name = 'Case0'
	scenario = Scenario(level, scenario_name)
	# Update the scenario instance with its path
	scenario.find(bng)

	bng.open()
	bng.load_scenario(scenario)
	bng.start_scenario()

	# Gets vehicles placed in the scenario
	vehicle_bng = 'BeamNGVehicle'
	model_code = 'JBeam'
	vehicle_obj = bng.find_objects_class(vehicle_bng)[0] # Gets the first vehicle obj
	id = str(vehicle_obj.id)
	model = vehicle_obj.opts[model_code]

	# Creates vehicle and attaches damage sensor
	vehicle = Vehicle(id, model)
	damage = Damage()
	vehicle.attach_sensor('damage', damage)

	# Set simulator to be deterministic mode
	bng.set_deterministic()
	bng.connect_vehicle(vehicle)
	assert vehicle.skt

	# Waiting for 600 steps and get sensor data
	bng.step(600)
	sensor = bng.poll_sensors(vehicle)

	print(sensors['damage'])

if __name__ == '__main__':
	main()