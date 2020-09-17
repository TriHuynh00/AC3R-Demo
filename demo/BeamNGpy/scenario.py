import os
import sys
from beamngpy import BeamNGpy, Scenario, Vehicle, setup_logging
from beamngpy.sensors import Damage

class BeamNg:
	def __init__(self, bng_log, scenario):
		self.bng_home = os.getenv('BNG_HOME')
		self.bng_research = os.getenv('BNG_RESEARCH')
		self.host = '127.0.0.1'
		self.port = 64257
		self.vehicles = []
		self.bng_log = bng_log
		self.scenario = scenario
		self.level = 'smallgrid'
		self.timeout = 20

	def create_vehicle(self, vehicle_objs):
		for v in vehicle_objs:
			# Creates vehicle with associated id
			vehicle = Vehicle(str(v.id))
			damage = Damage()
			# Attach Sensor
			vehicle.attach_sensor('damage', damage)
			self.vehicles.append(vehicle)

	def write_log(self, damage):
		with open(self.bng_log, 'w') as f:
			print(damage, file=f)
	
	def close_scenario(self, bng):
		bng.kill_beamng()
		sys.exit('----- Close BeamNG connection -----')
	
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
		for vehicle in self.vehicles:
			bng.connect_vehicle(vehicle)
			assert vehicle.skt

		# Wait for 50 steps before start
		waiting_steps = 50
		bng.step(waiting_steps, wait=True)
		bng.start_scenario() # Start scenario

		for _ in range(self.timeout):
			# Collects sensor data every 30 steps
			bng.step(30)

			accident_log = {}
			for vehicle in self.vehicles:
				sensor = bng.poll_sensors(vehicle)['damage']
				if (sensor['damage'] != 0): # Crash detected
					print("Crash detected!")
					accident_log.update( { vehicle.vid: sensor['partDamage'] } )
			
			empty = not bool(accident_log)
			if not empty:
				waiting_steps = 175
				self.write_log(accident_log)
				bng.step(waiting_steps, wait=True)
				print("Within time!")
				self.close_scenario(bng)

		# Timeout
		print("Timed out!")
		self.close_scenario(bng)