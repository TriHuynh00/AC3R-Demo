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
	port = 64257
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

	# Wait for 75 steps before start
	bng.step(75, wait=True)
	bng.start_scenario() # Start scenario


	for _ in range(20):
		# Collects sensor data every 30 steps
		bng.step(30)

		accident_log = {}
		for vehicle in vehicles:
			sensor = bng.poll_sensors(vehicle)['damage']
			if (sensor['damage'] != 0): # Crash detected
				print("Crash detected!")
				accident_log.update( { vehicle.vid: sensor['partDamage'] } )
		
		empty = not bool(accident_log)
		if not empty:
			write_log(accident_log)
			bng.step(150, wait=True)
			print("Within time!")
			close_scenario(bng)

	# Timeout
	print("Timed out!")
	close_scenario(bng)

def close_scenario(beamng):
	beamng.kill_beamng()
	sys.exit('----- Close BeamNG connection -----')

def write_log(damage):
	with open('accidentLog.txt', 'w') as f:
		print(damage, file=f)

	

def create_vehicle(vehicle_objs):
	vehicles = []
	for v in vehicle_objs:
		# Creates vehicle with associated id and attaches damage sensor to each vehicle
		vehicle = Vehicle(str(v.id))
		damage = Damage()
		electrics = Electrics()
		gForces = GForces()

		# Attach Sensor
		vehicle.attach_sensor('damage', damage)
		# vehicle.attach_sensor('electrics', electrics)
		# vehicle.attach_sensor('gForces', gForces)
		vehicles.append(vehicle)
	return vehicles


if __name__ == '__main__':
	beamng = beamng()

	# Creates empty log file
	with open('accidentLog.txt', 'w') as fp: 
		pass
	main(beamng, sys.argv[1]) # Ex: python C:\Users\Harvey\Documents\AC3R-Demo\demo\ac3r.py Case0

