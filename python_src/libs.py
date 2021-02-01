import json
from ac3r_plus import CrashScenario
from models import RoadProfiler, ScriptFactory, BNGVehicle
from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage


def collect_police_report(path):
    with open(path) as file:
        report_data = json.load(file)
    return report_data


# Collect simulation data from CrashScenario object
def collect_sim_data(crash_scenario):
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

    return crash_scenario, bng_roads, bng_vehicles
