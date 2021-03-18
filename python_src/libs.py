import json
import os
from pathlib import Path

from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage
from models import RoadProfiler, BNGVehicle
from typing import List
import scipy.stats as stats
from bisect import bisect_left

ROOT: Path = Path(os.path.abspath(os.path.join(os.path.dirname(__file__))))
PATH_TEST = str(ROOT.joinpath("tests"))


def _key_exist_in_list(target, lis):
    for d in lis:
        if d["name"] == target:
            return True
    return False


def _calculate_score_police_report(path):
    with open(path) as file:
        report_data = json.load(file)
    score = 1
    for v_id in report_data:
        v_parts = report_data[v_id]
        score += len(v_parts)
    return score


def _collect_police_report(path):
    with open(path) as file:
        report_data = json.load(file)
    return report_data


# Collect simulation data from CrashScenario object
def _collect_sim_data(crash_scenario):
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
        v = Vehicle(str(vehicle.name),
                    model="etk800", licence=vehicle.name, color=vehicle.color)
        v.attach_sensor('damage', Damage())
        road_pf = RoadProfiler()
        road_pf.compute_ai_script(trajectory, vehicle.color)
        bng_vehicles.append(BNGVehicle(v, initial_position, None, vehicle.rot_quat, road_pf))

    return crash_scenario, bng_roads, bng_vehicles


def _VD_A(treatment: List[float], control: List[float]):
    m = len(treatment)
    n = len(control)

    if m != n:
        raise ValueError("Data d and f must have the same length")

    r = stats.rankdata(treatment + control)
    r1 = sum(r[0:m])

    # Compute the measure
    # A = (r1/m - (m+1)/2)/n # formula (14) in Vargha and Delaney, 2000
    A = (2 * r1 - m * (m + 1)) / (2 * n * m)  # equivalent formula to avoid accuracy errors

    levels = [0.147, 0.33, 0.474]  # effect sizes from Hess and Kromrey, 2004
    # magnitude = ["negligible", "small", "medium", "large"]
    # scaled_A = (A - 0.5) * 2

    # magnitude = magnitude[bisect_left(levels, abs(scaled_A))]
    estimate = A

    return estimate
