import json
import time
import unittest

import libs
from models import ac3r
from ac3r_plus import CrashScenario
from simulation import Simulation
from libs import _collect_sim_data, _calculate_score_police_report
from matplotlib import pyplot as plt


class TrajectoryTest(unittest.TestCase):
    def test_json_trajectory(self):
        sources = [libs.PATH_TEST + "/data/Case0_data.json",
                   libs.PATH_TEST + "/data/Case1_data.json",
                   libs.PATH_TEST + "/data/Case2_data.json",
                   libs.PATH_TEST + "/data/Case3_data.json",
                   libs.PATH_TEST + "/data/Case4_data.json",
                   libs.PATH_TEST + "/data/Case5_data.json",
                   libs.PATH_TEST + "/data/Case6_data.json"]
        for source in sources:
            with open(source) as file:
                data = json.load(file)
            ac3r_scenario = ac3r.CrashScenario.from_json(data)
            colors = ["#ffdab9", "#b1c3de"]
            for i, vehicle in enumerate(ac3r_scenario.vehicles):
                trajectory_points = vehicle.trajectory_points
                xs = [p[0] for p in trajectory_points]
                ys = [p[1] for p in trajectory_points]
                plt.plot(xs, ys, 'o-', label='O' + vehicle.name, color=colors[i])

            with open(source) as file:
                scenario_data = json.load(file)
            crash_scenario = CrashScenario.from_json(scenario_data)
            bng_vehicles = _collect_sim_data(crash_scenario)[2]
            colors = ["#ff8c00", "#4069e1"]
            for i, v in enumerate(bng_vehicles):
                trajectory_points = v.road_pf.points
                xs = [p[0] for p in trajectory_points]
                ys = [p[1] for p in trajectory_points]
                plt.plot(xs, ys, '*-', label='N' + v.vehicle.vid, color=colors[i])
            plt.legend()
            plt.show()

    def test_generate_trajectory(self):
        sources = [libs.PATH_TEST + "/data/Case6_data.json"]

        for s in sources:
            with open(s) as file:
                scenario_data = json.load(file)
            crash_scenario = CrashScenario.from_json(scenario_data)
            timeout = time.time() + 90 * 1
            crash_scenario, bng_roads, bng_vehicles = _collect_sim_data(crash_scenario)


        self.assertEqual(0, 0)
