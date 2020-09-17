import os
import sys
from beamngpy import setup_logging
from BeamNGpy.scenario import BeamNg


# Defines log file locations
scenario = sys.argv[1]
bng_log = os.path.dirname(__file__) + '\\logCases\\' + scenario + '.txt'
# Creates empty log file
with open(bng_log, 'w') as fp:
	pass
# Executing the given scenario
setup_logging()

ac3r = BeamNg(bng_log, scenario)
bng = ac3r.start_beamng()
ac3r.execute_scenario(bng)

record_dir = os.path.expanduser('~') + "\\Documents\\BeamNG.research\\levels\\smallgrid\\damageRecord\\"
if ac3r.isCrash:
	for car in ac3r.cars:
		car.set_description()
		record_file = scenario + "-" + car.name + "-damageLog.log"
		with open(record_dir + record_file, 'w') as f:
			print(car.description, file=f)
else:
	record_file = scenario + "-noCrash-damageLog.log"
	with open(record_dir + record_file, 'w') as f:
		print(car.description, file=f)