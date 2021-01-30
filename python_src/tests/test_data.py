import json
from ac3r_plus import CrashScenario
from models import RoadProfiler, ScriptFactory, BNGVehicle
from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage


def sample_scenarios():
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
    scenario_4 = {
        'v1_speed': [40],
        'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
        'v2_speed': [40],
        'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
    }
    scenario_5 = {
        'v1_speed': [34],
        'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
        'v2_speed': [55],
        'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
    }
    return [scenario_1, scenario_2, scenario_3]


def collect_police_report():
    with open("./data/sample_report.json") as file:
        report_data = json.load(file)
    return report_data


def collect_scenario_data(scenario):
    with open("./data/Case6_mod.json") as file:
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

    return crash_scenario, bng_roads, bng_vehicles
