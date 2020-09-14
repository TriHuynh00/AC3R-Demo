import os
import sys
from beamngpy import BeamNGpy, Scenario, Vehicle
from beamngpy.sensors import Damage

def main(scenario_name):
	# Get environment variables
	BNG_HOME = os.getenv('BNG_HOME')
	BNG_RESEARCH = os.getenv('BNG_RESEARCH')
	host = '127.0.0.1'
	port = 64256

	# Instantiates a BeamNGpy instance
	bng = BeamNGpy(host, port, BNG_HOME, BNG_RESEARCH)

	# Find a scenario in the smallgrid level called 'Case4'
	level = 'smallgrid'
	scenario = Scenario(level, scenario_name)
	# Update the scenario instance with its path
	scenario.find(bng)

	bng.open()
	print("------- Load Scenario -------")
	bng.load_scenario(scenario)
	print("------- Start Scenario -------")
	bng.start_scenario()


	# Gets vehicles placed in the scenario
	vehicle_bng = 'BeamNGVehicle'
	vehicles = create_vehicle(bng.find_objects_class(vehicle_bng))

	# Set simulator to be deterministic mode
	bng.set_deterministic()
	print("------- Connects Vehicle to Scenario -------")
	for vehicle in vehicles:
		bng.connect_vehicle(vehicle)
		assert vehicle.skt
		print(vehicle.skt)

	# # Waiting for 600 steps and get sensor data
	bng.step(600)
	# print("------- Print Sensor Data -------")
	for vehicle in vehicles:
		sensor = bng.poll_sensors(vehicle)
	# 	print(sensors)

def create_vehicle(vehicle_objs):
	vehicles = []
	for v in vehicle_objs:
		print("------- Create New Vehicle -------")
		# Creates vehicle with associated id and attaches damage sensor to each vehicle
		vehicle = Vehicle(str(v.id))
		print("-------  Attach Sensor -------")
		damage = Damage()
		vehicle.attach_sensor('damage', damage)
		print(vehicle)
		vehicles.append(vehicle)
	return vehicles


if __name__ == '__main__':
	main(sys.argv[1]) # Ex: python3 ac3r.py Case0