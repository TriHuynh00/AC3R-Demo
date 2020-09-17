import os
import sys
from beamngpy import setup_logging
from BeamNGpy.scenario import BeamNg
from BeamNGpy.car import Car


# Defines log file locations
bng_log = os.path.dirname(__file__) + '\\logCases\\' + sys.argv[1] + '.txt'
# Creates empty log file
with open(bng_log, 'w') as fp:
	pass
# Executing the given scenario
setup_logging()
ac3r = BeamNg(bng_log, sys.argv[1])
bng = ac3r.start_beamng()
ac3r.execute_scenario(bng)