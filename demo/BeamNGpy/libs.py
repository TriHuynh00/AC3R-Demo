import sys
import json
import numpy as np
from models import AccidentCase
from models import Street
from models import Car


# list into a tuple
def convert(list_el):
    return tuple(list_el)


# Read JSON data
def read_json_data(scenario_path):
    try:
        # Open file with file path
        with open(scenario_path) as file:
            scenario_data = json.load(file)

        street_list = []  # Define array of roads in json
        road_num = scenario_data['road_num']  # Number of roads
        road_id = 1
        while road_id <= road_num:
            # Convert a single point of road_node_list to tuple
            points = []
            road_node_list = scenario_data["r" + str(road_id) + '_road_node_list']
            for point in road_node_list:
                points.append(convert(point))

            # Create an instance of road
            street = Street(
                id=road_id,
                type=scenario_data["r" + str(road_id) + "_road_type"],
                shape=scenario_data["r" + str(road_id) + "_road_shape"],
                points=points
            )
            street_list.append(street)
            road_id += 1

        car_list = []  # Define array of vehicles in json
        vehicle_num = scenario_data['vehicle_num']  # Number of vehicles
        vehicle_id = 1
        while vehicle_id <= vehicle_num:
            # Convert a single point of vehicle_points and rot_degrees to tuple
            points = []
            vehicle_points = scenario_data["v" + str(vehicle_id) + "_points"]
            for point in vehicle_points:
                points.append(convert(point))

            rot_degrees = []
            rot_degree = scenario_data["v" + str(vehicle_id) + "_rot_degree"]
            rot_degrees.append(convert(rot_degree[0]))
            rot_degrees.append(rot_degree[1][0])

            # Instance of vehicle
            car = Car(
                id=vehicle_id,
                velocities=scenario_data["v" + str(vehicle_id) + "_velocities"],
                color=scenario_data["v" + str(vehicle_id) + "_color"],
                rot_degree=rot_degrees,
                travelling_dir=scenario_data["v" + str(vehicle_id) + "_travelling_dir"],
                damage=scenario_data["v" + str(vehicle_id) + "_damage_components"],
                points=points
            )
            car_list.append(car)
            vehicle_id += 1

        # Instance of accident case
        accident_case = AccidentCase(
            name=sys.argv[1],
            crash_point=scenario_data['crash_point'],
            car_list=car_list,
            street_list=street_list
        )

        return accident_case
    except Exception as e:
        print("Oops!", e.__class__, "occurred.")
        print("Details: ", e, ".")
        exit()


def path_generator(vehicle_population):
    print('Start path_generator \n')
    # vehicle_population (required): object to extract starting and collision coordinates to drive the vehicle
    # num_points (required): the expected amount of points requires to generate path
    # extra_points (optional): the amount of points requires to drive the car further after reaching the num_point

    script = list()  # Properties for ai_set_script()

    # Properties for add_debug_line()
    points = list()
    point_colors = list()
    spheres = list()
    sphere_colors = list()

    starting_node = {
        'x': vehicle_population['pos_x'], 'y': vehicle_population['pos_y'],
        'z': vehicle_population['pos_z'], 't': (2 * 1 + (np.abs(np.sin(np.radians(1)))) * 32) / 32
    }
    middle_node = {
        'x': (vehicle_population['pos_x'] + vehicle_population['col_x']) / 2,
        'y': (vehicle_population['pos_y'] + vehicle_population['col_y']) / 2,
        'z': vehicle_population['pos_z'], 't': (2 * 2 + (np.abs(np.sin(np.radians(2)))) * 32) / 32
    }
    end_node = {
        'x': vehicle_population['col_x'], 'y': vehicle_population['col_y'],
        'z': vehicle_population['pos_z'], 't': (2 * 3 + (np.abs(np.sin(np.radians(3)))) * 32) / 32
    }
    script_nodes = [starting_node, middle_node, end_node]

    for node in script_nodes:
        # Values for ai_set_script()
        script.append(node)

        # Values for add_debug_line()
        points.append([node['x'], node['y'], node['z']])
        point_colors.append([0, 0, 45, 0.1])
        # Values for add_debug_line()
        spheres.append([node['x'], node['y'], node['z'], 0.25])
        sphere_colors.append([0, 0, 45, 0.8])

    result = {
        'script': script,
        'points': points,
        'point_colors': point_colors,
        'spheres': spheres,
        'sphere_colors': sphere_colors
    }
    return result
