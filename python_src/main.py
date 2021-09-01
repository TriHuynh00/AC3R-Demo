import click
import json
import numpy as np
from visualization import VehicleTrajectoryVisualizer
from models import SimulationFactory, Simulation, SimulationScore
from models.ac3rp import CrashScenario
from experiment import Experiment


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
    with open(fn) as file:
        csc = json.load(file)

    import matplotlib.pyplot as plt
    fig = plt.figure()

    for color in ["red", "blue"]:
        csc_cp = csc["vehicles"][color]["crash_point"]["coordinates"]
        v1_line = csc["vehicles"][color]["trajectories"]["simulation_trajectory"]
        aspect_ratio = csc["vehicles"][color]["dimensions"]["car_length_sim"] / csc["vehicles"][color]["dimensions"][
            "car_length"]
        csc_cp = [csc_cp[0] * aspect_ratio, csc_cp[1] * aspect_ratio]

        # assign coordinates
        dist_x = - csc_cp[0]
        dist_y = - csc_cp[1]
        xs = [p[0] + dist_x for p in v1_line]
        ys = [p[1] + dist_y for p in v1_line]
        plt.plot(xs, ys, '-', label=f'Vehicle {color}', color=color)
    # plt.plot(0, 0, 'o-', color="black")
    plt.legend()
    plt.gca().set_aspect("equal")
    plt.xlim([-100, 100])
    plt.ylim([-100, 100])
    plt.title(f'CRISCEs {case_name}')
    plt.show()
    fig.savefig(f'{case_name}_crisce.png')


def visualize_ac3r(fn, case_name):
    import matplotlib.pyplot as plt
    from models import ac3r
    from descartes import PolygonPatch
    from shapely.geometry import LineString
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
    plt.xlim([-240, 240])
    plt.ylim([-240, 240])
    plt.title(f'AC3R {case_name}')
    plt.show()
    fig.savefig(f'{case_name}_ac3r.png')


# make sure we invoke cli
if __name__ == '__main__':
    import os
    import re

    directory_list = list()
    for root, dirs, files in os.walk("/Users/vuong/Dropbox/CRISCE-AC3R-Datasets/AC3R_CIREN_JSON", topdown=False):
        for name in dirs:
            directory_list.append(os.path.join(root, name))

    for case_dir in directory_list:
        case_name = case_dir.split("/")[-1]
        if not re.match("^[0-9]{1,6}$", case_name):
            continue
        crise_dir = f'/Users/vuong/Dropbox/CRISCE-AC3R-Datasets/Results/CIREN/{case_name}/output.json'
        visualize_ac3r(f'{case_dir}/crash_report_{case_name}_data.json', case_name)
        visualize_csc(crise_dir, case_name)
