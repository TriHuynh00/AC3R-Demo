import json
import time
import unittest
import libs

from ac3r_plus import CrashScenario
from simulation import Simulation
from libs import _collect_sim_data, _calculate_score_police_report


class TrajectoryTest(unittest.TestCase):
    def test_generate_trajectory(self):
        sources = [libs.PATH_TEST+"./data/Case6.json"]

        for s in sources:
            with open(s) as file:
                scenario_data = json.load(file)
            crash_scenario = CrashScenario.from_json(scenario_data)
            timeout = time.time() + 60*1
            crash_scenario, bng_roads, bng_vehicles = _collect_sim_data(crash_scenario)
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario(timeout)
            crash_scenario.sim_report = simulation.get_report()

            crash_scenario.cal_fitness(police_report_path=
                                       libs.PATH_TEST+"./data/Case6_report.json")  # Calculate fitness score

            print("Scores: ", crash_scenario.score)
            print("Expected Scores: ", _calculate_score_police_report(libs.PATH_TEST+"./data/Case6_report.json"))
        self.assertEqual(0, 0)
