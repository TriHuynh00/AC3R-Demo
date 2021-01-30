import time
import unittest
import json
from ac3r_plus import CrashScenario
from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage
from models import RoadProfiler, BNGVehicle
from simulation import Simulation


def collect_scenario_data(source):
    with open(source) as file:
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
        trajectory = vehicle.generate_trajectory()
        initial_position = (trajectory[0][0], trajectory[0][1], 0)
        v = Vehicle("scenario_player_" + str(vehicle.name),
                    model="etk800", licence=vehicle.name, color=vehicle.color)
        v.attach_sensor('damage', Damage())
        road_pf = RoadProfiler()
        road_pf.compute_ai_script(trajectory, vehicle.color)
        bng_vehicles.append(BNGVehicle(v, initial_position, None, vehicle.rot_quat, road_pf))

    return bng_roads, bng_vehicles


class TrajectoryTest(unittest.TestCase):
    def test_generate_trajectory(self):
        sources = ["./data/Case6_data.json", "./data/Case6_mod.json", "./data/Case1_data.json", "./data/Case0_data.json"]
        for s in sources:
            timeout = time.time() + 60*1 # 2mins
            crash_scenario, bng_roads, bng_vehicles = collect_scenario_data(s)
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario(timeout)
        self.assertEqual(0, 0)
