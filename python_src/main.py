import click
import json
import numpy as np
from visualization import VehicleTrajectoryVisualizer
from models import SimulationFactory, Simulation, SimulationScore, SimulationExec
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
    # SimulationExec(simulation).execute_scenario(timeout=30)
    # print(f'Simulation Score: {SimulationScore(simulation).calculate(debug=True)}')
    print(f'{SimulationScore(simulation).get_expected_score(debug=True)}')


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def evol_scenario(scenario_file):
    experiment: Experiment = Experiment(scenario_file)
    experiment.run()


# make sure we invoke cli
if __name__ == '__main__':
    # cli()

    scenarios = [
        {"name": "Case0", "path": "data/Case0_data.json", "threshold": 1.4, },
        {"name": "Case1", "path": "data/Case1_data.json", "threshold": 1.7999999999999998,},
        {"name": "Case2", "path": "data/Case2_data.json", "threshold": 1.4, },
        {"name": "Case3", "path": "data/Case3_data.json", "threshold": 2.0,},
        {"name": "Case4", "path": "data/Case4_data.json", "threshold": 2.0, },
        {"name": "Case5", "path": "data/Case5_data.json", "threshold": 2.4000000000000004,},
        {"name": "Case6", "path": "data/Case6_data.json", "threshold": 1.7, },
    ]

    for scenario in scenarios:
        path = scenario["path"]
        threshold = scenario["threshold"]
        for i in np.arange(start=1, stop=5, step=1):
            sim_name: str = path[5:11] + str(i)
            print(f'Level {sim_name}...')
            exp: Experiment = Experiment(file_path=path, simulation_name=sim_name, threshold=threshold)
            exp.run(method_name="Random")
        print(f'-------------------- End of {path} --------------------------------------------------------------')
        print()

    for scenario in scenarios:
        path = scenario["path"]
        threshold = scenario["threshold"]
        for i in np.arange(start=1, stop=5, step=1):
            sim_name: str = path[5:11] + str(i)
            print(f'Level {sim_name}...')
            exp: Experiment = Experiment(file_path=path, simulation_name=sim_name, threshold=threshold)
            exp.run(method_name="OpO")
        print(f'-------------------- End of {path} --------------------------------------------------------------')
        print()
