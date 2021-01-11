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
from models import RoadProfiler
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
    bng_vehicles = []
    bng_scripts = []

    # v0 = crash_scenario.vehicles[0]
    # vehicle_trajectory = v0.generate_trajectory()
    # vehicle_nodes = []
    # for vehicle_node in vehicle_trajectory:
    #     vehicle_nodes.extend(vehicle_node["points"])
    # print("Trajectory Generate")
    # print(vehicle_nodes)
    # print("=========")
    #
    # segment_x_0 = [segment[0] for segment in vehicle_nodes]
    # segment_y_0 = [segment[1] for segment in vehicle_nodes]
    # plt.plot(segment_x_0, segment_y_0, 'black')  # plotting t, a separately
    # plt.scatter(segment_x_0, segment_y_0, c='b')
    # v1 = crash_scenario.vehicles[1]
    # print("Finish car 1")
    # print("-========")
    # vehicle_trajectory = v1.generate_trajectory()
    # vehicle_nodes = []
    # for vehicle_node in vehicle_trajectory:
    #     vehicle_nodes.extend(vehicle_node["points"])
    # segment_x_1 = [segment[0] for segment in vehicle_nodes]
    # segment_y_1 = [segment[1] for segment in vehicle_nodes]
    #
    #
    # plt.plot(segment_x_1, segment_y_1, 'black')  # plotting t, b separately
    # plt.scatter(segment_x_1, segment_y_1, c='r')
    # plt.show()
    #
    # print("Finish plotting")
    # exit()

    for vehicle in crash_scenario.vehicles:
        vehicle_trajectory = vehicle.generate_trajectory()
        vehicle_nodes = []
        for vehicle_node in vehicle_trajectory:
            vehicle_nodes.extend(vehicle_node["points"])
        bng_vehicle = Vehicle("scenario_player_" + str(vehicle.name),
                              model="etk800", licence=vehicle.name, color=vehicle.color)
        bng_vehicle.attach_sensor('damage', Damage())
        bng_vehicles.append({
            'vehicle': bng_vehicle,
            'pos': vehicle_nodes[0],
            'rot': None,
            'rot_quat': vehicle.rot_quat
        })
        road_pf = RoadProfiler(vehicle.color)
        bng_scripts.append({
            'script': road_pf.compute_ai_script(vehicle_trajectory), # Script for ai_set_script
            'road_pf': road_pf  # Color of road profile
        })

    # BeamNG Executor
    ac3r = BeamNg()
    bng_instance = ac3r.init_beamng()
    scenario = Scenario('smallgrid', 'test_01')

    for road in bng_roads:
        scenario.add_road(road)

    vehicles = []
    for bng_vehicle in bng_vehicles:
        vehicles.append(bng_vehicle['vehicle'])
        scenario.add_vehicle(bng_vehicle['vehicle'], pos=bng_vehicle['pos'],
                             rot=bng_vehicle['rot'], rot_quat=bng_vehicle['rot_quat'])

    scenario.make(bng_instance)
    bng_instance.open(launch=True)
    bng_instance.set_deterministic()

    # 2 minutes for each scenario
    timeout = 1200
    is_crash = False

    try:
        bng_instance.load_scenario(scenario)
        bng_instance.start_scenario()

        # Drawing debug line and forcing vehicle moving by given trajectory
        for vehicle, bng_script in zip(vehicles, bng_scripts):
            script, road_pf = bng_script['script'], bng_script['road_pf']
            bng_instance.add_debug_line(road_pf.points, road_pf.sphere_colors,
                                        spheres=road_pf.spheres, sphere_colors=road_pf.sphere_colors,
                                        cling=True, offset=0.1)
            vehicle.ai_set_mode('manual')
            vehicle.ai_set_script(script, cling=False)

        # Update the vehicle information
        for _ in range(timeout):
            bng_instance.step(10)
            for vehicle in vehicles:
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
            for vehicle in vehicles:
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
