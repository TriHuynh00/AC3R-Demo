import click
import json
from visualization import VehicleTrajectoryVisualizer
from models import SimulationFactory, Simulation, SimulationScore, categorize_report
from models.ac3rp import CrashScenario


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
    simulation = Simulation(sim_factory=sim_factory)
    simulation.execute_scenario(60)

    data_targets = simulation.targets
    data_outputs = simulation.get_data_outputs()
    result = (0, 0, 0)
    for vehicle in data_targets:
        data_target = data_targets[vehicle]
        creator = categorize_report(data_target)
        data_output = data_outputs[vehicle]
        result = tuple(map(lambda x, y: x + y, result, creator.match(data_output, data_target)))
    print(f'Result: {result}')
    print(f'SimulationScore: {SimulationScore(simulation_tuple=result).calculate()}')


# make sure we invoke cli
if __name__ == '__main__':
    cli()
