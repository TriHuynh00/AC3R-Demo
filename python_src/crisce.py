from roads import Roads
from car import Car
from kinematics import Kinematics
import pandas as pd
import os
import json
import click
import numpy as np
from shapely.geometry import Point
import logging as logger
import sys

CRISCE_IMPACT_MODEL = {
    "front_left": [
        "headlight_L", "hood", "fender_L", "bumper_F", "bumperbar_F", "suspension_F", "body_wagon"
    ],

    "front_right": [
        "hood", "bumper_F", "bumperbar_F", "fender_R", "headlight_R", "body_wagon", "suspension_F"
    ],

    "front_mid": [
        "bumperbar_F", "radiator", "body_wagon", "headlight_R", "headlight_L", "bumper_F", "fender_R", "fender_L",
        "hood", "suspension_F"
    ],

    "left_mid": [
        "door_RL_wagon", "body_wagon", "doorglass_FL", "mirror_L", "door_FL"
    ],

    "rear_left": [
        "suspension_R", "exhaust_i6_petrol", "taillight_L""body_wagon", "bumper_R", "tailgate", "bumper_R"
    ],

    "rear_mid": [
        "tailgate", "tailgateglass", "taillight_L", "taillight_R", "exhaust_i6_petrol", "bumper_R", "body_wagon",
        "suspension_R"
    ],

    "rear_right": [
        "suspension_R", "exhaust_i6_petrol", "taillight_R", "body_wagon", "tailgateglass", "tailgate", "bumper_R"
    ],

    "right_mid": [
        "door_RR_wagon", "body_wagon", "doorglass_FR", "mirror_R", "door_FR"
    ]
}


def translate_damage_comp_crisce2ac3r(part):
    impact_dict = {
        "front_left": "front left",
        "front_right": "front right",
        "front_mid": "front",
        "rear_mid": "rear",
        "left_mid": "left",
        "right_mid": "right",
        "rear_left": "rear left",
        "rear_right": "rear right"
    }
    return impact_dict[part]


def pairs(lst):
    for i in range(1, len(lst)):
        yield lst[i - 1], lst[i]


def angle_to_quat(angle):
    angle = np.radians(angle)
    cy = np.cos(angle[2] * 0.5)
    sy = np.sin(angle[2] * 0.5)
    cp = np.cos(angle[1] * 0.5)
    sp = np.sin(angle[1] * 0.5)
    cr = np.cos(angle[0] * 0.5)
    sr = np.sin(angle[0] * 0.5)

    w = cr * cp * cy + sr * sp * sy
    x = sr * cp * cy - cr * sp * sy
    y = cr * sp * cy + sr * cp * sy
    z = cr * cp * sy - sr * sp * cy

    return x, y, z, w


def generate_expected_crash_components(vehicles, dataset_name=None):
    expected_crash_components = []
    external_or_internal = "internal_impact_side" if dataset_name == "SYNTH" else "external_impact_side"
    for i, v_color in enumerate(["red", "blue"]):
        # Extract expected broken parts from external.csv
        external_or_internal_parts = vehicles[v_color]["impact_point_details"][external_or_internal]
        parts = [{
            "name": translate_damage_comp_crisce2ac3r(external_or_internal_parts),
            "damage": 1
        }]
        crash_dict = {
            "name": "v" + str(i + 1),
            "parts": parts
        }
        expected_crash_components.append(crash_dict)
    return expected_crash_components


def generate_roads(road_nodes):
    roads = []
    for i, nodes in enumerate(road_nodes):
        road_dict = {
            "name": "road" + str(i + 1),
            "road_type": "roadway",
            "road_shape": "I",
            "road_node_list": nodes
        }
        roads.append(road_dict)
    return roads


def generate_vehicles(vehicles, dataset_name=None):
    vehicle_list = []
    external_or_internal = "internal_impact_side" if dataset_name == "SYNTH" else "external_impact_side"
    for i, v_color in enumerate(["red", "blue"]):
        angle = vehicles[v_color]["vehicle_info"]["0"]["angle_of_car"]

        veh_dict = {
            "name": "v" + str(i + 1),
            "color": "1 0 0" if v_color == "red" else "0 0 1",
            "rot_quat": angle_to_quat((0, 0, -angle - 90)),
            "distance_to_trigger": -1.0,
            "damage_components": vehicles[v_color]["impact_point_details"][external_or_internal]
        }
        driving_actions = []
        script_trajectory = vehicles[v_color]["trajectories"]["script_trajectory"]
        if len(script_trajectory) > 1:
            length = 0
            duration = script_trajectory[-1]['t']
            for segment in pairs(script_trajectory):
                p1, p2 = segment[0], segment[1]
                length += Point(p1['x'], p1['y']).distance(Point(p2['x'], p2['y']))
            velocity = length / duration * 3.6
            for segment in pairs(script_trajectory):
                p1, p2 = segment[0], segment[1]
                driving_actions.append({
                    "name": "follow",
                    "trajectory": [[[p1['x'], p1['y']], [p2['x'], p2['y']]]],
                    "speed": velocity
                })
        else:
            segment = script_trajectory[0]
            driving_actions.append({
                "name": "stop",
                "trajectory": [[[segment['x'], segment['y']]]],
                "speed": 0
            })
        veh_dict["driving_actions"] = driving_actions
        vehicle_list.append(veh_dict)
    return vehicle_list


def exec_scenarion(scenario_file, dataset_name=None, output_to=None):
    dir_path = scenario_file
    file = dir_path + "\\sketch.jpeg"
    road = dir_path + "\\road.jpeg"

    BLUE_CAR_BOUNDARY = np.array([[85, 50, 60], [160, 255, 255]])

    if dataset_name == "SYNTH":
        sketch_type_external = False
        RED_CAR_BOUNDARY = np.array([[0, 200, 180],  # red internal crash sketches
                                     [110, 255, 255]])
        external_impact_points = None
    else:
        RED_CAR_BOUNDARY = np.array([[0, 190, 215], [179, 255, 255]])  # red external_0
        external_csv = dir_path + "\\external.csv"
        sketch_type_external = True
        external_impact_points = None
        if sketch_type_external:
            df = pd.read_csv(external_csv)
            external_impact_points = dict()
            for i in df.index:
                color = str.lower(df.vehicle_color[i])
                impact = str.lower(df.impact_point[i])
                external_impact_points[color] = dict()
                external_impact_points[color] = impact

    # --------- Main Logic Of the Code Starts Here ---------
    car = Car()
    roads = Roads()
    kinematics = Kinematics()
    sketch = file
    output_folder = os.path.join(dir_path, "output")  # sketch.split(".")[0])
    car_length_sim = 4.670000586694935
    sketch_image_path = sketch
    road_image_path = road
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # ------ Read Sketch Image ------
    car.setColorBoundary(red_boundary=RED_CAR_BOUNDARY, blue_boundary=BLUE_CAR_BOUNDARY)
    vehicles, time_efficiency = car.extractVehicleInformation(image_path=sketch_image_path,
                                                              time_efficiency=dict(),
                                                              show_image=False, output_folder=output_folder,
                                                              external=sketch_type_external,
                                                              external_impact_points=external_impact_points,
                                                              crash_impact_locations=CRISCE_IMPACT_MODEL,
                                                              car_length_sim=car_length_sim)
    car_length, car_width = car.getCarDimensions()
    roads, lane_nodes = roads.extractRoadInformation(image_path=road_image_path,
                                                     time_efficiency=time_efficiency,
                                                     show_image=False,
                                                     output_folder=output_folder, car_length=car_length,
                                                     car_width=car_width,
                                                     car_length_sim=car_length_sim)
    vehicles, time_efficiency = kinematics.extractKinematicsInformation(image_path=sketch_image_path,
                                                                        vehicles=vehicles,
                                                                        time_efficiency=time_efficiency,
                                                                        output_folder=output_folder,
                                                                        show_image=False)

    # ------ Generate JSON data ------
    ac3r_plus = {
        "name": scenario_file.split("\\")[-1],
        "roads": generate_roads(lane_nodes),
        "vehicles": generate_vehicles(vehicles, dataset_name),
        "expected_crash_components": generate_expected_crash_components(vehicles, dataset_name)
    }

    # ------ Write JSON to file ------
    file_name = scenario_file.split("\\")[-1] + ".json"
    path_ac3rp = file_name if output_to is None else output_to + '\\' + file_name
    with open(path_ac3rp, 'w') as fp:
        json.dump(ac3r_plus, fp)

    print(f'Data file {file_name} is written to {path_ac3rp}!')
    print("END")


def setup_logging(log_to, debug):
    def log_exception(extype, value, trace):
        logger.exception('Uncaught exception:', exc_info=(extype, value, trace))

    # Disable annoyng messages from matplot lib.
    # See: https://stackoverflow.com/questions/56618739/matplotlib-throws-warning-message-because-of-findfont-python
    logger.getLogger('matplotlib.font_manager').disabled = True
    # Disable annoyng messages from Pillow lib.
    logger.getLogger('PIL').setLevel(logger.WARNING)

    term_handler = logger.StreamHandler()
    log_handlers = [term_handler]
    start_msg = "Process Started"

    if log_to is not None:
        file_handler = logger.FileHandler(log_to, 'a', 'utf-8')
        log_handlers.append(file_handler)
        start_msg += " ".join(["writing to file: ", str(log_to)])

    log_level = logger.DEBUG if debug else logger.INFO

    logger.basicConfig(format='%(asctime)s %(levelname)-8s %(message)s', level=log_level, handlers=log_handlers)

    sys.excepthook = log_exception

    logger.info(start_msg)


@click.group()
@click.option('--log-to', required=False, type=click.Path(exists=False),
              help="Location of the log file. If not specified logs appear on the console")
@click.option('--debug', required=False, is_flag=True, default=False, show_default='Disabled',
              help="Activate debugging (results in more logging)")
@click.pass_context
def cli(ctx, log_to, debug):
    # Pass the context of the command down the line
    ctx.ensure_object(dict)
    # Setup logging
    setup_logging(log_to, debug)


@cli.command()
@click.option('--accident-sketch', required=True, type=click.Path(exists=True), multiple=False,
              help="Input accident sketch for generating the simulation")
@click.option('--dataset-name', required=True, type=click.Choice(['CIREN', 'NMVCCS', 'SYNTH'], case_sensitive=False),
              multiple=False,
              help="Name of the dataset the accident comes from.")
@click.option('--output-to', required=False, type=click.Path(exists=False), multiple=False,
              help="Folder to store outputs. It will created if not present. If omitted we use the accident folder.")
@click.pass_context
def generate(ctx, accident_sketch, dataset_name, output_to):
    # Pass the context of the command down the line
    ctx.ensure_object(dict)
    exec_scenarion(accident_sketch, dataset_name, output_to)


if __name__ == '__main__':
    cli()
    my_dict = {
        # 0: '..\\Datasets\\NMVCCS\\2005002585724',
        # 1: '..\\Datasets\\NMVCCS\\2005008586061a',
        # 2: '..\\Datasets\\NMVCCS\\2005008586061b',
        # 3: '..\\Datasets\\NMVCCS\\2005012695622',
        # 4: '..\\Datasets\\NMVCCS\\2005045587341',
        # 5: '..\\Datasets\\NMVCCS\\2005048103904',
        # 6: '..\\Datasets\\NMVCCS\\2006048103049',
        # 7: '..\\Datasets\\CIREN\\100343',
        # 8: '..\\Datasets\\CIREN\\156722',
        # 9: '..\\Datasets\\SYNTH\\curved_18',
    }

    for k in my_dict:
        if len(sys.argv) == 0:
            exec_scenarion(my_dict[k])
        else:
            exec_scenarion(my_dict[k], "SYNTH")
