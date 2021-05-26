#
# Entry point for the AC3R plus system
#

import click
import logging as log
import sys
import json
from models.simulation import Simulation
from beamngpy import Road, Vehicle
from models.ac3rp import CrashScenario


def _log_exception(extype, value, trace):
    log.exception('Uncaught exception:', exc_info=(extype, value, trace))


def _set_up_logging(log_to, debug):
    # Disable annoyng messages from matplot lib.
    # See: https://stackoverflow.com/questions/56618739/matplotlib-throws-warning-message-because-of-findfont-python
    log.getLogger('matplotlib.font_manager').disabled = True

    term_handler = log.StreamHandler()
    log_handlers = [term_handler]
    start_msg = "Started test generation"

    if log_to is not None:
        file_handler = log.FileHandler(log_to, 'a', 'utf-8')
        log_handlers.append(file_handler)
        start_msg += " ".join(["writing to file: ", str(log_to)])

    log_level = log.DEBUG if debug else log.INFO

    log.basicConfig(format='%(asctime)s %(levelname)-8s %(message)s', level=log_level, handlers=log_handlers)

    # Configure default logging for uncaught exceptions
    sys.excepthook = _log_exception

    log.info(start_msg)


def _invoke_ac3r():
    """
    Invoke the AC3R as java executable and return its output, i.e., the JSON file
    """
    # TODO Can we specificy the output location and name of the JSON so we can read it from there?
    pass


@click.group()
# Logging options
@click.option('--log-to', required=False, type=click.Path(exists=False),
              help="File to Log to. If not specified logs will show only on the console")
@click.option('--debug', required=False, is_flag=True, default=False, help="Activate debugging (more logging)")
def cli(log_to, debug):
    """
    Main entry point for the CLI
    """
    _set_up_logging(log_to, debug)

    pass


@cli.command()
@click.argument('police_report', type=click.Path(exists=True))
# TODO Maybe a better name?
def run(police_report):
    # Get the json from AC3R or fail
    # TODO Add here all the other CLI and options
    scenario_file = _invoke_ac3r(police_report)
    _run_ac3rplus_from_scenario(scenario_file)


@cli.command()
@click.argument('scenario_file', type=click.Path(exists=True))
def run_from_scenario(scenario_file):
    """
    Take a JSON scenario file and run the entire search algorithm
    """
    # From the JSON file create a CrashScenario object, this includes transforming the waypoint representation
    #   into a trajectory segment representation
    # original_crash_scenario = _
    # _run_ac3rplus_from_scenario(original_crash_scenario)

    with open(scenario_file) as file:
        scenario_data = json.load(file)
    crash_scenario = CrashScenario.from_json(scenario_data)
    # JSON READ: Building scenario's streets
    bng_roads = []
    for road in crash_scenario.roads:
        bng_road = Road('road_asphalt_2lane', rid=road.name)
        bng_road.nodes.extend(road.road_nodes)
        bng_roads.append(bng_road)

    # JSON READ: Building scenario's vehicle


def _run_ac3rplus_from_scenario(original_crash_scenario):
    pass


# make sure we invoke cli
if __name__ == '__main__':
    cli()
