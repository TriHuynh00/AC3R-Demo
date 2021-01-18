import json
from ac3r_plus import CrashScenario
from models import RoadProfiler, ScriptFactory, BNGVehicle
from simulation import Simulation
from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage
from evaluation import Evaluation
import unittest

CRASHED = 1
NO_CRASH = 0

scenario_1 = {
    'v1_speed': [35],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [55],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}
scenario_2 = {
    'v1_speed': [30],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [30],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}
scenario_3 = {
    'v1_speed': [35, 55],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [55, 75],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}


def execute_scenario(scenario):
    with open("C:\\Users\\Harvey\\Documents\\AC3R-Demo\\python_src\\tests\\data\\Case6_data.json") as file:
        scenario_data = json.load(file)
    crash_scenario = CrashScenario.from_json(scenario_data)
    # JSON READ: Building scenario's streets
    bng_roads = []
    for road in crash_scenario.roads:
        bng_road = Road('road_asphalt_2lane', rid=road.name)
        bng_road.nodes.extend(road.road_nodes)
        bng_roads.append(bng_road)

    bng_vehicles = []
    for vehicle in crash_scenario.vehicles:
        trajectory = []
        if vehicle.name == 'v1':
            p = scenario['v1_trajectory']
            trajectory = ScriptFactory(p[0], p[1], p[2], p[3]).compute_scripts(speeds=scenario['v1_speed'])
        else:
            p = scenario['v2_trajectory']
            trajectory = ScriptFactory(p[0], p[1], p[2], p[3]).compute_scripts(speeds=scenario['v2_speed'])

        initial_position = (trajectory[0][0], trajectory[0][1], 0)
        v = Vehicle("scenario_player_" + str(vehicle.name),
                    model="etk800", licence=vehicle.name, color=vehicle.color)
        v.attach_sensor('damage', Damage())
        road_pf = RoadProfiler()
        road_pf.compute_ai_script(trajectory, vehicle.color)
        bng_vehicles.append(BNGVehicle(v, initial_position, None, vehicle.rot_quat, road_pf))

    return bng_roads, bng_vehicles


class EvaluationTest(unittest.TestCase):
    def test_evaluate_status(self):
        status = []
        for scenario in [scenario_1, scenario_2]:
            bng_roads, bng_vehicles = execute_scenario(scenario)
            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            simulation_result = simulation.get_result()

            evl = Evaluation(simulation_result)
            distance, st = evl.evaluate_simulation()
            status.append(st)
        self.assertEqual(CRASHED, status[0], "Crash detected in scenario 01")
        self.assertEqual(NO_CRASH, status[1], "No crash in scenario 02")

    def test_eval_last_pos(self):
        bng_roads, bng_vehicles = execute_scenario(scenario_2)
        # Execute crash scenario
        simulation = Simulation(bng_roads, bng_vehicles)
        simulation.execute_scenario()
        simulation_result = simulation.get_result()

        evl = Evaluation(simulation_result)
        distance, status = evl.evaluate_simulation()

        self.assertEqual(8, int(distance), "Distance between vehicles should be around 8")

    def test_mutate_scenario_by_status(self):
        status = NO_CRASH

        speed_inc = 0
        while status == NO_CRASH:
            scenario_2['v2_speed'][0] = scenario_2['v2_speed'][0] + speed_inc
            bng_roads, bng_vehicles = execute_scenario(scenario_2)

            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            simulation_result = simulation.get_result()

            evl = Evaluation(simulation_result)
            distance, status = evl.evaluate_simulation()
            if status == NO_CRASH:
                speed_inc = speed_inc + 1
        self.assertEqual(CRASHED, status, "Crash detected in mutated scenario")

    def test_mutate_scenario_by_distance(self):
        distance = 1
        speed_inc = 1
        slow, fast = 'v1_speed', 'v1_speed'
        while distance > 0:
            scenario_2[slow][0] = scenario_2[slow][0] + speed_inc
            scenario_2[fast][0] = scenario_2[fast][0] - speed_inc
            bng_roads, bng_vehicles = execute_scenario(scenario_2)
            print(scenario_2)

            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            simulation_result = simulation.get_result()

            evl = Evaluation(simulation_result)
            distance, status, slow, fast = evl.evaluate_simulation()
            print(distance, status)
        self.assertEqual(CRASHED, status, "Crash detected in mutated scenario")