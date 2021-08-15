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


# make sure we invoke cli
if __name__ == '__main__':
    # cli()

    # for s in ["data/Case4_data.json"]:
    #     for i in np.arange(start=1, stop=6, step=1):
    #         sim_name: str = s[5:11] + str(i)
    #         print(f'Level {sim_name}...')
    #         exp: Experiment = Experiment(file_path=s, simulation_name=sim_name)
    #         exp.run(method_name="Random")
    #     print(f'-------------------- End of {s} --------------------------------------------------------------')
    #     print()

    scenario_files = ["data/crisce/crash_report_100343_data.json",
                      "data/crisce/crash_report_103378_data.json",
                      "data/crisce/crash_report_105222_data.json",
                      "data/crisce/crash_report_108812_data.json",
                      "data/crisce/crash_report_108909_data.json",
                      "data/crisce/crash_report_119897_data.json",
                      "data/crisce/crash_report_120013_data.json",
                      "data/crisce/crash_report_120278_data.json",
                      "data/crisce/crash_report_120305_data.json",
                      "data/crisce/crash_report_120565_data.json",
                      "data/crisce/crash_report_122080_data.json",
                      "data/crisce/crash_report_122168_data.json",
                      "data/crisce/crash_report_128697_data.json"]
    for s in scenario_files:
        with open(s) as file:
            scenario_data = json.load(file)
        sim_factory = SimulationFactory(CrashScenario.from_json(scenario_data))
        simulation = Simulation(sim_factory=sim_factory, file_name=s)
        simulation.execute_scenario(timeout=30)
        # print(f'Simulation Score: {SimulationScore(simulation).calculate()} / '
        #       f'{SimulationScore(simulation).get_expected_score()}')

    # import libs
    # import scipy.stats as stats
    # import numpy as np

    # f1, f2 = np.array([0, 7, 17, 2, 9, 0]), np.array([0, 1, 1, 1, 3, 0])
    # f1c, f2c = np.array([1.4, 1.8, 1.4, 1.6, -16, 1.7]), np.array([1.4, 1.8, 1.4, 1.6, -12.86, 1.7])

    # print(f'Average Time:  Random {str(round(f1.mean(), 2))}; OpO {str(round(f2.mean(), 2))}')
    # print(f'Average Score: Random {str(round(f1c.mean(), 2))}; OpO {str(round(f2c.mean(), 2))}')

    # # Calculate the p-value to know if two populations are different
    # alpha = 0.05  # significance level to reject H0
    # stat, p = stats.mannwhitneyu(f1, f2)
    # print('Test Statistics=%.3f, p=%.3f' % (stat, p))
    # if p > alpha:
    #     print('H0: The two populations are equal versus.')
    # else:
    #     print('H1: The two populations are different.')
    #     print("Compare: ")
    #     print(f'Random: {f1}')
    #     print(f'OpO: {f2}')
    #     # To understand how different they were (the effect size)
    #     # we used VDA
    #     if libs._VD_A(f1, f2) > 0.5:
    #         print(f'Random faster: {f1}')
    #     else:
    #         print(f'OpO faster: {f2}')
