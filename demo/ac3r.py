import os
import sys
from beamngpy import BeamNGpy, Scenario, Vehicle, setup_logging
from beamngpy.sensors import Damage, Electrics, GForces


def beamng():
	setup_logging()
	# Get environment variables
	BNG_HOME = os.getenv('BNG_HOME')
	BNG_RESEARCH = os.getenv('BNG_RESEARCH')
	host = '127.0.0.1'
	port = 64256
	# Instantiates a BeamNGpy instance
	beamng = BeamNGpy(host, port, BNG_HOME, BNG_RESEARCH)
	return beamng

def main(beamng, scenario_name):
	# Find a scenario in the smallgrid level called 'Case4'
	level = 'smallgrid'
	scenario = Scenario(level, scenario_name)
	# Update the scenario instance with its path
	scenario.find(beamng)

	bng = beamng.open(launch=True)
	bng.hide_hud()
	bng.set_deterministic() # Set simulator to be deterministic mode

	# Load and start the scenario
	bng.load_scenario(scenario)
	
	# Gets vehicles placed in the scenario
	vehicle_bng = 'BeamNGVehicle'
	vehicles = create_vehicle(bng.find_objects_class(vehicle_bng))

	# bng.set_steps_per_second(10)
	for vehicle in vehicles:
		bng.connect_vehicle(vehicle)
		assert vehicle.skt

	bng.step(75, wait=True)
	bng.start_scenario()
	for _ in range(64):
		bng.step(30)
		s1 = bng.poll_sensors(vehicles[0])
		s2 = bng.poll_sensors(vehicles[1])
		print(s1)
		print(s2)
		print('------')

def create_vehicle(vehicle_objs):
	vehicles = []
	for v in vehicle_objs:
		# Creates vehicle with associated id and attaches damage sensor to each vehicle
		vehicle = Vehicle(str(v.id))
		damage = Damage()
		vehicle.attach_sensor('damage', damage) # Attach Sensor
		vehicles.append(vehicle)
	return vehicles


if __name__ == '__main__':
	beamng = beamng()
	main(beamng, sys.argv[1]) # Ex: python3 ac3r.py Case0

