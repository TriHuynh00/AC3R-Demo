import os
import click
import json
import numpy as np
from visualization import VehicleTrajectoryVisualizer
from models import SimulationFactory, Simulation, SimulationScore
from models.ac3rp import CrashScenario
from experiment import Experiment
import matplotlib.pyplot as plt
from models import ac3r
from descartes import PolygonPatch
from shapely.geometry import LineString


@click.group()
def cli():
    pass


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def plot_ac3r(scenario_file):
    """Take a JSON scenario file and plot the AC3R data points."""
    VehicleTrajectoryVisualizer.plot_ac3r(scenario_file)


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def plot_ac3rp(scenario_file):
    """Take a JSON scenario file and plot the AC3RPlus data points."""
    VehicleTrajectoryVisualizer.plot_ac3rp(scenario_file)


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def run_from_scenario(scenario_file):
    """Take a JSON scenario file and run the entire search algorithm."""
    # TODO: Can we read some configurations (like fitness function, mutation operators, speed min/max,
    #  and other parameters), so we can add it from there?
    with open(scenario_file) as file:
        scenario_data = json.load(file)
    sim_factory = SimulationFactory(CrashScenario.from_json(scenario_data))
    simulation = Simulation(sim_factory=sim_factory, debug=True)
    simulation.execute_scenario(timeout=60)
    print(f'Simulation Score: {SimulationScore(simulation).calculate(debug=True)} / '
          f'{SimulationScore(simulation).get_expected_score(debug=True)}')


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def evol_scenario(scenario_file):
    experiment: Experiment = Experiment(scenario_file)
    experiment.run()


def visualize_csc(fn, case_name):
    with open(fn + ".json") as file:
        csc = json.load(file)

    import matplotlib.pyplot as plt
    fig = plt.figure()
    dist_x, dist_y = 0, 0

    for color in ["red", "blue"]:
        csc_cp = csc["vehicles"][color]["crash_point"]["coordinates"]
        aspect_ratio = csc["vehicles"][color]["dimensions"]["car_length_sim"] / csc["vehicles"][color]["dimensions"][
            "car_length"]
        csc_cp = [csc_cp[0] * aspect_ratio, csc_cp[1] * aspect_ratio]
        # assign coordinates
        dist_x = - csc_cp[0]
        dist_y = - csc_cp[1]

    for i, road_nodes in enumerate(csc["road_nodes"]):
        road_width = 8
        road_poly = LineString([(t[0] + dist_x, t[1] + dist_y) for t in road_nodes]).buffer(road_width, cap_style=2,
                                                                                            join_style=2)
        road_patch = PolygonPatch(road_poly, fc='gray', ec='dimgray')  # ec='#555555', alpha=0.5, zorder=4)
        plt.gca().add_patch(road_patch)
        xs = [p[0] + dist_x for p in road_nodes]
        ys = [p[1] + dist_y for p in road_nodes]
        plt.plot(xs, ys, '-', color="#9c9c9c")

    for color in ["red", "blue"]:
        v1_line = csc["vehicles"][color]["trajectories"]["simulation_trajectory"]
        xs = [p[0] + dist_x for p in v1_line]
        ys = [p[1] + dist_y for p in v1_line]
        plt.plot(xs, ys, '-', label=f'Vehicle {color}', color=color)

    plt.legend()
    plt.gca().set_aspect("equal")
    plt.xlim([-100, 100])
    plt.ylim([-100, 100])
    plt.title(f'CRISCEs {case_name}')
    plt.show()
    fig.savefig(f'{case_name}_crisce.png')


def visualize_ac3r(fn, case_name):
    fig = plt.figure()

    colors = ["red", "blue"]
    with open(fn) as file:
        scenario_data = json.load(file)

    ac3r_scenario = ac3r.CrashScenario.from_json(scenario_data)

    for i, road in enumerate(ac3r_scenario.roads):
        trajectory_points = road.trajectory_points
        road_width = 8
        road_poly = LineString([(t[0], t[1]) for t in trajectory_points]).buffer(road_width, cap_style=2, join_style=2)
        road_patch = PolygonPatch(road_poly, fc='gray', ec='dimgray')  # ec='#555555', alpha=0.5, zorder=4)
        plt.gca().add_patch(road_patch)
        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]
        plt.plot(xs, ys, '-', color="#9c9c9c")

    for i, vehicle in enumerate(ac3r_scenario.vehicles):
        trajectory_points = vehicle.trajectory_points
        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]
        plt.plot(xs, ys, '-', label=vehicle.name, color=colors[i])

    plt.legend()
    plt.gca().set_aspect('equal')
    plt.xlim([-100, 100])
    plt.ylim([-100, 100])
    plt.title(f'AC3R {case_name}')
    plt.show()
    fig.savefig(f'{case_name}_ac3r.png')


def visualize_nmvccs(ac3r_scenario, case_name):
    fig = plt.figure()

    colors = ["red", "blue"]
    # with open(fn) as file:
    #     scenario_data = json.load(file)
    #
    # ac3r_scenario = ac3r.CrashScenario.from_json(scenario_data)

    for i, road in enumerate(ac3r_scenario["roads"]):
        trajectory_points = road["trajectory_points"]
        road_width = 8
        road_poly = LineString([(t[0], t[1]) for t in trajectory_points]).buffer(road_width, cap_style=2, join_style=2)
        road_patch = PolygonPatch(road_poly, fc='gray', ec='dimgray')  # ec='#555555', alpha=0.5, zorder=4)
        plt.gca().add_patch(road_patch)
        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]
        plt.plot(xs, ys, '-', color="#9c9c9c")

    for i, vehicle in enumerate(ac3r_scenario["vehicles"]):
        trajectory_points = vehicle["trajectory_points"]
        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]
        plt.plot(xs, ys, '-', label=vehicle["name"], color=colors[i])

    plt.legend()
    plt.gca().set_aspect('equal')
    plt.xlim([-100, 100])
    plt.ylim([-100, 100])
    plt.title(f'AC3R {case_name}')
    plt.show()
    fig.savefig(f'{case_name}_ac3r.png')


def read_ciren_data():
    import re

    directory_list = list()
    for root, dirs, files in os.walk("/Users/vuong/Dropbox/CRISCE-AC3R-Datasets/AC3R_CIREN_JSON", topdown=False):
        for name in dirs:
            directory_list.append(os.path.join(root, name))

    for case_dir in directory_list:
        case_name = case_dir.split("/")[-1]
        if not re.match("^[0-9]{1,6}$", case_name):
            continue
        if int(case_name) in [122080, 108812, 119897, 137780, 165428]:
            crise_dir = f'/Users/vuong/Dropbox/CRISCE-AC3R-Datasets/Results/CIREN/{case_name}/output.json'
            visualize_ac3r(f'{case_dir}/crash_report_{case_name}_data.json', case_name)
        # visualize_csc(crise_dir, case_name)

    for s in ["data/122080", "data/108812", "data/119897", "data/137780", "data/165428"]:
        visualize_csc(s, s)


def run_nmvccs(scenario_id):
    import re
    import time
    from beamngpy import BeamNGpy, Scenario

    CARS = []
    ROADS = []
    timeout = 30
    start_time = 0
    is_crash = False
    bng_home = os.getenv('BNG_HOME')
    bng_research = os.getenv('BNG_RESEARCH')
    host = "127.0.0.1"
    port = 64257
    bng_instance = BeamNGpy(host, port, bng_home, bng_research)

    scenario = Scenario("smallgrid", scenario_id)
    scenario.find(bng_instance)
    bng_instance.open(launch=True)
    bng_instance.set_deterministic()
    bng_instance.remove_step_limit()
    bng_instance.load_scenario(scenario)

    class Car:
        def __init__(self, id, name):
            self.id = id
            self.name = name
            self.trajectories = []

        def set_trajectories(self, point):
            self.trajectories.append(point)

    for v in bng_instance.find_objects_class("BeamNGVehicle"):
        CARS.append(Car(v.id, v.name))

    for dr in bng_instance.find_objects_class("DecalRoad"):
        if (re.match("(road)\d", dr.name)) is not None:
            road = Car(dr.id, dr.name)
            for point in bng_instance.get_road_edges(dr.name):
                road.set_trajectories([point["middle"][0], point["middle"][1]])
            ROADS.append(road)

    try:
        bng_instance.start_scenario()
        start_time = time.time()
        while time.time() < (start_time + timeout):
            bng_instance.step(50, True)
            for v, c in zip(bng_instance.find_objects_class("BeamNGVehicle"), CARS):
                c.set_trajectories([v.pos[0], v.pos[1]])

    except Exception as ex:
        bng_instance.close()
    finally:
        bng_instance.close()
        print("Simulation Name: ", scenario_id)
        print("Simulation Time: ", time.time() - start_time)

    vehicles = []
    for car in CARS:
        vehicles.append({"id": car.id, "name": car.name, "trajectory_points": car.trajectories})
    roads = []
    for road in ROADS:
        roads.append({"id": road.id, "name": road.name, "trajectory_points": road.trajectories})
    toJSON = {"vehicles": vehicles, "roads": roads}

    visualize_nmvccs(toJSON, scenario_id)


# make sure we invoke cli
if __name__ == '__main__':
    files = os.listdir("C:\\Users\\vuong\\Documents\\NMVCCS\\")
    cases = []
    for i, f in enumerate(files):
        case_name = f.split(".")[0]
        if case_name not in cases:
            cases.append(case_name)
    # cases = [c + ".json" for c in cases]
    print(cases)

    for case in ["FI_10_2006048103049", "RE_1_2005003498523", "SP_2_2005003498061", "TIP_2_2005006445243"]:
        run_nmvccs(case)
