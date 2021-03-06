import json
import time
import unittest

from ac3r_plus import CrashScenario
from simulation import Simulation
from libs import _collect_sim_data


class TrajectoryTest(unittest.TestCase):
    def test_generate_trajectory(self):
        sources = ["./data/Case6_data.json", "./data/Case6_mod.json", "./data/Case1_data.json", "./data/Case0_data.json"]
        for s in sources:
            with open(s) as file:
                scenario_data = json.load(file)
            crash_scenario = CrashScenario.from_json(scenario_data)
            timeout = time.time() + 60*1 # 2mins
            crash_scenario, bng_roads, bng_vehicles = _collect_sim_data(crash_scenario)
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario(timeout)
        self.assertEqual(0, 0)
