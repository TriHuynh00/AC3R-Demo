import click
import json
import numpy as np
from visualization import Scenario as VehicleTrajectoryVisualizer
from models import SimulationFactory, Simulation, SimulationScore, SimulationExec, CONST
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
    simulation = Simulation(sim_factory=sim_factory, name="test00", debug=True)
    SimulationExec(simulation=simulation, is_birdview=True).execute_scenario(timeout=30)
    # print(f'Simulation Score: {SimulationScore(simulation).calculate(debug=True)}')
    print(f'{SimulationScore(simulation).get_expected_score(debug=False)}')


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def evol_scenario(scenario_file):
    experiment: Experiment = Experiment(scenario_file)
    experiment.run()


def execute_searching(scenario_files):
    single_mutator = [
        {
            "type": CONST.MUTATE_SPEED_CLASS,
            "probability": 0.5,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        },
    ]

    multi_mutators = [
        {
            "type": CONST.MUTATE_SPEED_CLASS,
            "probability": 0.5,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        },
        {
            "type": CONST.MUTATE_INITIAL_POINT_CLASS,
            "probability": 0.5,
            "params": {"mean": 0, "std": 1, "min": -5, "max": 5}
        },
    ]

    for mutator_dict in [{"name": "single", "mutators": single_mutator},
                         {"name": "multi", "mutators": multi_mutators}]:
        for scenario in scenario_files:
            case_name = scenario["name"]
            path = scenario["path"]
            # Random Search
            for i in np.arange(start=1, stop=11, step=1):
                sim_name: str = f'{(mutator_dict["name"].title() + "_Random")}_{str(i)}'
                print(f'Case {case_name}: Level {sim_name}...')
                exp: Experiment = Experiment(file_path=path,
                                             case_name=case_name,
                                             simulation_name=sim_name,
                                             mutators=mutator_dict["mutators"],
                                             method_name=CONST.RANDOM,
                                             epochs=30)
                exp.run()

            # OpO Search
            for i in np.arange(start=1, stop=11, step=1):
                sim_name: str = f'{(mutator_dict["name"].title() + "_OpO")}_{str(i)}'
                print(f'Case {case_name}: Level {sim_name}...')
                exp: Experiment = Experiment(file_path=path,
                                             case_name=case_name,
                                             simulation_name=sim_name,
                                             mutators=mutator_dict["mutators"],
                                             method_name=CONST.OPO,
                                             epochs=30)
                exp.run()
            print("=========")


# make sure we invoke cli
if __name__ == '__main__':
    cli()
    exit()
    scenarios = [
        {"name": "129224", "path": "ciren/129224/data.json"},
    ]

    execute_searching(scenarios)
