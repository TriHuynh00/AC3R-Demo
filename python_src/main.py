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


def visualize_csc(fn):
    with open(fn+"_c.json") as file:
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
    plt.title(f'CRISCEs crash_report_{fn.split("/")[1]}')
    plt.show()
    fig.savefig(f'crisce_{fn.split("/")[1]}.png')


# make sure we invoke cli
if __name__ == '__main__':
    cli()
    # scenario_files = ["data/crisce/crash_report_122080_data.json"]
    # for s in scenario_files:
    #     with open(s) as file:
    #         scenario_data = json.load(file)
    #     VehicleTrajectoryVisualizer.plot_ac3r(s)
    #     VehicleTrajectoryVisualizer.plot_ac3rp(s)

    # scenario_files = ["data/122080"]
    # for f in scenario_files:
    #     VehicleTrajectoryVisualizer.plot_ac3r(f+"_a.json")
        # visualize_csc(f)



