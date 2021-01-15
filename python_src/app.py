#
# Entry point for the AC3R plus system
#

import click
import logging as log
import sys
import json
from beamng import BeamNg
from beamngpy import Scenario, Road, Vehicle
from beamngpy.sensors import Damage
from ac3r_plus import CrashScenario
from models import RoadProfiler, ScriptFactory, BNGVehicle
import matplotlib.pyplot as plt
import numpy as np
import math


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
    # Sample data
    p1_test = [-2.0, 86.84551724137933, 0]
    p2_test = [-2.0, 17.880000000000006, 0]
    v1_trajectory = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1]).compute_scripts(speeds=[35])
    p1_test = [89.845517, 87.845517, 0]
    p2_test = [20.88, 18.88, 0]
    v2_trajectory = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1]).compute_scripts(speeds=[55])

    bng_vehicles = []
    for vehicle in crash_scenario.vehicles:
        trajectory = v1_trajectory if vehicle.name == 'v1' else v2_trajectory
        initial_position = (trajectory[0][0], trajectory[0][1], 0)
        v = Vehicle("scenario_player_" + str(vehicle.name),
                              model="etk800", licence=vehicle.name, color=vehicle.color)
        v.attach_sensor('damage', Damage())
        road_pf = RoadProfiler()
        road_pf.compute_ai_script(trajectory, vehicle.color)
        bng_vehicles.append(BNGVehicle(v, initial_position, None, vehicle.rot_quat, road_pf))

    # BeamNG Executor
    ac3r = BeamNg()
    bng_instance = ac3r.init_beamng()
    scenario = Scenario('smallgrid', 'test_01')

    for road in bng_roads:
        scenario.add_road(road)

    for bng_vehicle in bng_vehicles:
        scenario.add_vehicle(bng_vehicle.vehicle, pos=bng_vehicle.pos,
                             rot=bng_vehicle.rot, rot_quat=bng_vehicle.rot_quat)

    scenario.make(bng_instance)
    bng_instance.open(launch=True)
    bng_instance.set_deterministic()

    # 3 minutes for each scenario
    timeout = 180
    is_crash = False

    try:
        bng_instance.load_scenario(scenario)
        bng_instance.start_scenario()

        # Drawing debug line and forcing vehicle moving by given trajectory
        for bng_vehicle in bng_vehicles:
            vehicle = bng_vehicle.vehicle
            road_pf = bng_vehicle.road_pf
            bng_instance.add_debug_line(road_pf.points, road_pf.sphere_colors,
                                        spheres=road_pf.spheres, sphere_colors=road_pf.sphere_colors,
                                        cling=True, offset=0.1)
            vehicle.ai_set_mode('manual')
            vehicle.ai_set_script(road_pf.script, cling=False)

        # Update the vehicle information
        for _ in range(timeout):
            bng_instance.step(10)
            for bng_vehicle in bng_vehicles:
                vehicle = bng_vehicle.vehicle
                # Find the position of moving car
                current_position = (vehicle.state['pos'][0], vehicle.state['pos'][1])

                # Collect the damage sensor information
                sensor = bng_instance.poll_sensors(vehicle)['damage']
                if sensor['damage'] != 0:  # Crash detected
                    # Disable AI control
                    vehicle.ai_set_mode('disable')
                    vehicle.ai_set_speed(20 / 3.6, 'set')
                    vehicle.control(throttle=0, steering=0, brake=0, parkingbrake=0)
                    vehicle.update_vehicle()
                    is_crash = True

        if not is_crash:
            print("Timed out!")
        else:
            print("Crash detected!")
            for bng_vehicle in bng_vehicles:
                vehicle = bng_vehicle.vehicle
                sensor = bng_instance.poll_sensors(vehicle)['damage']
                if sensor['damage'] != 0:
                    print({vehicle.vid: sensor['part_damage']})
    finally:
        bng_instance.close()


def _run_ac3rplus_from_scenario(original_crash_scenario):
    pass


# make sure we invoke cli
if __name__ == '__main__':
    cli()
