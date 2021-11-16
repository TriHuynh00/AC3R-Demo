import click
import json
import numpy as np
from visualization import VehicleTrajectoryVisualizer
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
    simulation = Simulation(sim_factory=sim_factory, debug=True)
    # SimulationExec(simulation).execute_scenario(timeout=30)
    print(f'Simulation Score: {SimulationScore(simulation).calculate(debug=True)}')
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
        # {"name": "Case0", "path": "data/Case0_data.json", "threshold": 1.4, },
        # {"name": "Case1", "path": "data/Case1_data.json", "threshold": 1.7999999999999998,},
        # {"name": "Case2", "path": "data/Case2_data.json", "threshold": 1.4, },
        # {"name": "Case3", "path": "data/Case3_data.json", "threshold": 2.0,},
        # {"name": "Case4", "path": "data/Case4_data.json", "threshold": 2.0, },
        # {"name": "Case5", "path": "data/Case5_data.json", "threshold": 2.4000000000000004,},
        # {"name": "Case6", "path": "data/Case6_data.json", "threshold": 1.7, },
        # {"name": "Case7", "path": "data/Case7_data.json", "threshold": 1.4, },
        # {"name": "Case8", "path": "data/Case8_data.json", "threshold": 2.1, },
        # {"name": "Case9", "path": "data/Case9_data.json", "threshold": 1.7, },
        {"name": "FI_8", "path": "data/FI_8_data.json", "threshold": 1.7999999999999998, },
        {"name": "FI_12", "path": "data/FI_12_data.json", "threshold": 1.4, },
        {"name": "FI_14", "path": "data/FI_14_data.json", "threshold": 1.7, },
        {"name": "SP_5", "path": "data/SP_5_data.json", "threshold": 1.7, },
        {"name": "SP_18", "path": "data/SP_18_data.json", "threshold": 1.7, },
        {"name": "TIP_6", "path": "data/TIP_6_data.json", "threshold": 2.1, },
    ]

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
        for scenario in scenarios:
            case_name = scenario["name"]
            path = scenario["path"]
            threshold = scenario["threshold"]
            # Random Search
            for i in np.arange(start=1, stop=11, step=1):
                sim_name: str = f'{(mutator_dict["name"].title() + "_Random")}_{path[5:11]}{str(i)}'
                print(f'Level {sim_name}...')
                exp: Experiment = Experiment(file_path=path,
                                             case_name=case_name,
                                             simulation_name=sim_name,
                                             threshold=threshold,
                                             mutators=mutator_dict["mutators"],
                                             method_name=CONST.RANDOM)
                exp.run()

            # OpO Search
            for i in np.arange(start=1, stop=11, step=1):
                sim_name: str = f'{(mutator_dict["name"].title() + "_OpO")}_{path[5:11]}{str(i)}'
                print(f'Level {sim_name}...')
                exp: Experiment = Experiment(file_path=path,
                                             case_name=case_name,
                                             simulation_name=sim_name,
                                             threshold=threshold,
                                             mutators=mutator_dict["mutators"],
                                             method_name=CONST.OPO)
                exp.run()
            print("=========")

