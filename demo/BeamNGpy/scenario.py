import os
import sys
import time
from beamngpy import BeamNGpy, Scenario, Vehicle, setup_logging
from beamngpy.sensors import Damage
from BeamNGpy.car import Car

class BeamNg:
	def __init__(self, bng_log, scenario):
		self.bng_home = os.getenv('BNG_HOME')
		self.bng_research = os.getenv('BNG_RESEARCH')
		self.host = '127.0.0.1'
		self.port = 64257
		self.cars = []
		self.bng_log = bng_log
		self.scenario = scenario
		self.level = 'smallgrid'
		self.timeout = 90
		self.isCrash = False

	def create_vehicle(self, vehicles):
		for i in range(len(vehicles)):
			vehicle = Vehicle(str(vehicles[i].id), model='etk800')
			vehicle.attach_sensor('damage', Damage())
			self.cars.append(Car(i+1, vehicle)) # Vehicle name is v1, v2,...

	def write_log(self, damage):
		with open(self.bng_log, 'w') as f:
			print(damage, file=f)
	
	def close_scenario(self, bng):
		bng.kill_beamng()
		return
	
	def start_beamng(self):
		# Instantiates a BeamNGpy instance
		return BeamNGpy(self.host, self.port, self.bng_home, self.bng_research)

	def execute_scenario(self, bng):
		scenario = Scenario(self.level, self.scenario)
		# Update the scenario instance with its path
		scenario.find(bng)

		# Configures BeamNG client
		bng.open(launch=True)
		bng.hide_hud()
		bng.set_deterministic() # Set simulator to be deterministic mode

		# Load and start the scenario
		bng.load_scenario(scenario)
		
		# Gets vehicles placed in the scenario
		vehicle_bng = 'BeamNGVehicle'
		self.create_vehicle(bng.find_objects_class(vehicle_bng))

		# bng.set_steps_per_second(10)
		for car in self.cars:
			vehicle = car.vehicle
			bng.connect_vehicle(vehicle)
			assert vehicle.skt

		# Wait for 50 steps before start
		waiting_steps = 50
		bng.step(waiting_steps, wait=True)
		bng.start_scenario() # Start scenario

		accident_log = {}
		vehicle_points = [[], []]
		start_time = time.time()
		while time.time() < (start_time + 25):
			# Collects sensor data every 10 steps
			bng.step(10, True)

			empty = not bool(accident_log)
			if empty:
				for i, car in enumerate(self.cars):
					vehicle = car.vehicle
					sensor = bng.poll_sensors(vehicle)['damage']
					if (sensor['damage'] != 0): # Crash detected
						print("Crash detected!")
						self.isCrash = True
						accident_log.update( { vehicle.vid: sensor['part_damage'] } )
						car.damage =  sensor['part_damage']
					current_position = (vehicle.state['pos'][0], vehicle.state['pos'][1])
					vehicle_points[i].append(current_position)

			else:
				self.write_log(accident_log) # Write log file
				waiting_steps = 175
				bng.step(waiting_steps, wait=True)
				print("Within time!")
				break

		for i, points in enumerate(vehicle_points):
			for i, p in enumerate(points):
				if i+1 == len(points):
					break
				p1 = points[i]
				p2 = points[i+1]
				print('{')
				print('\t"name":"follow",')
				print(f'\t"trajectory":[[[{p1[0]}, {p1[1]}, 0.0], [{p2[0]}, {p2[1]}, 0.0]]],')
				print('\t"speed":20')
				print('},')

			print("==============\n")

		# Timeout
# 		if not self.isCrash:
# 			print("Timed out!")
		self.close_scenario(bng)