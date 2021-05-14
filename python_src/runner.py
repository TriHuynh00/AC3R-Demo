import json

import libs
from models import ac3r, ac3rp
from libs import _collect_sim_data
from matplotlib import pyplot as plt

sources = [libs.PATH_TEST + "/data/Case6_data.json"]
for source in sources:
    with open(source) as file:
        data = json.load(file)
    ac3r_scenario = ac3r.CrashScenario.from_json(data)
    colors = ["#ffdab9", "#b1c3de"]
    for i, vehicle in enumerate(ac3r_scenario.vehicles):
        trajectory_points = vehicle.trajectory_points
        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]
        plt.plot(xs, ys, 'o-', label=vehicle.name, color=colors[i])
    plt.legend()
    plt.title(f'AC3R {ac3r_scenario.name}')
    plt.show()

    with open(source) as file:
        scenario_data = json.load(file)
    ac3rp_scenario = ac3rp.CrashScenario.from_json(scenario_data)
    bng_vehicles = _collect_sim_data(ac3rp_scenario)[2]
    colors = ["#ff8c00", "#4069e1"]
    for i, v in enumerate(bng_vehicles):
        trajectory_points = v.road_pf.points
        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]
        plt.plot(xs, ys, 'o-', label=v.vehicle.vid, color=colors[i])
    plt.legend()
    plt.title(f'AC3R Plus {ac3rp_scenario.name}')
    plt.show()
