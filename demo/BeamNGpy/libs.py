import csv
import sys
import json
import math
import numpy as np
from BeamNGpy.models.accident_case import AccidentCase
from BeamNGpy.models.street import Street
from BeamNGpy.models.car import Car

from BeamNGpy.helpers.crash_simulation_helper import getV1BeamNGCoordinaes, getV2BeamNGCoordinaes

# list into a tuple 
def convert(list): 
    return tuple(list) 

# ----------------------------- genetic algorithm helper --------------------
def generate_random_population(N=5,Gene=10):
    print("random population")
    random_population = []
    random_population.append([5, 6, 7, 2, 5, 5, 8, 8, 7, 5, 4, 2, 4, 7])
    seed_population = [[np.random.randint(1,9) for i in range(Gene + 4)] for j in range(N - 1)]
    for i, val in enumerate(seed_population):
        random_population.append(val)

    print(random_population)
    initial_population = convert_population(random_population)
    return initial_population


def convert_population(random_population):
    print("convert population")
    converted_population = []
    for i, val in enumerate(random_population):
        merge_list = []
        merge_list.append(int(str(val[0]) + str(val[1])))
        merge_list.append(val[2])
        merge_list.append(val[3])
        merge_list.append(val[4])
        merge_list.append(int(str(val[5]) + str(val[6])))
        merge_list.append(val[7])
        merge_list.append(val[8])
        merge_list.append(val[9])
        merge_list.append(val[10])
        merge_list.append(int(str(val[11]) + str(val[12]) + str(val[13])))
        converted_population.append(merge_list)

    return converted_population


def shrink_chromosome(chromosome):
    merge_chromosome = []
    merge_chromosome.append(int(str(chromosome[0]) + str(chromosome[1])))
    merge_chromosome.append(chromosome[2])
    merge_chromosome.append(chromosome[3])
    merge_chromosome.append(chromosome[4])
    merge_chromosome.append(int(str(chromosome[5]) + str(chromosome[6])))
    merge_chromosome.append(chromosome[7])
    merge_chromosome.append(chromosome[8])
    merge_chromosome.append(chromosome[9])
    merge_chromosome.append(chromosome[10])
    merge_chromosome.append(int(str(chromosome[11]) + str(chromosome[12]) + str(chromosome[13])))
    return merge_chromosome


def expand_chromosome(chromosome):
    expand_chromosome = []
    speed_striker = [int(d) for d in str(chromosome[0])]
    expand_chromosome.append(speed_striker[0])
    expand_chromosome.append(speed_striker[1])
    expand_chromosome.append(chromosome[1])
    expand_chromosome.append(chromosome[2])
    expand_chromosome.append(chromosome[3])
    speed_victim = [int(d) for d in str(chromosome[4])]
    expand_chromosome.append(speed_victim[0])
    expand_chromosome.append(speed_victim[1])
    expand_chromosome.append(chromosome[5])
    expand_chromosome.append(chromosome[6])
    expand_chromosome.append(chromosome[7])
    expand_chromosome.append(chromosome[8])
    angle = [int(d) for d in str(chromosome[9])]
    expand_chromosome.append(angle[0])
    expand_chromosome.append(angle[1])
    expand_chromosome.append(angle[2])
    return expand_chromosome


# Decoding of population chromosome
def decoding_of_parameter(chromosome):
    print("decoding of parameters")

    V1_SPEED_INDEX = 0
    V1_DISTANCE_INDEX_1 = 1
    V1_DISTANCE_INDEX_2 = 2
    V1_WIDTH_INDEX = 3
    V2_SPEED_INDEX = 4
    V2_DISTANCE_INDEX_1 = 5
    V2_DISTANCE_INDEX_2 = 6
    V2_WIDTH_INDEX = 7
    POINT_OF_IMPACT_RADIUS = 8
    POINT_OF_IMPACT_ANGLE = 9
    # IMPACT_POSITION_X = 239
    # IMPACT_POSITION_Y = 143
    IMPACT_POSITION_X = -2
    IMPACT_POSITION_Y = 2

    # rotation of the car in beamng scenario

    # Speed
    v1_speed = int(str(chromosome[V1_SPEED_INDEX]))
    v2_speed = int(str(chromosome[V2_SPEED_INDEX]))

    # point of impact
    radius = chromosome[POINT_OF_IMPACT_RADIUS] % 1
    angle_str = str(chromosome[POINT_OF_IMPACT_ANGLE])
    angle = int(angle_str) % 360

    # point of impact (collision point  provided by user)
    # https://stackoverflow.com/questions/2912779/how-to-calculate-a-point-with-an-given-center-angle-and-radius

    point_of_impact_x = IMPACT_POSITION_X + radius * math.cos(math.radians(angle)) # radians
    point_of_impact_y = IMPACT_POSITION_Y + radius * math.sin(math.radians(angle))  # radians
    impact_point = (point_of_impact_x,point_of_impact_y)


    # position length
    total_distance_v1 = float(int(str(chromosome[V1_DISTANCE_INDEX_1]) + str(chromosome[V1_DISTANCE_INDEX_2])) / 50)
    v1_pos_bg = getV1BeamNGCoordinaes(total_distance_v1,  chromosome[V1_WIDTH_INDEX] % 4)  # get beamng coordinates (polyline coordinate). it will be always calculated from center - joint

    total_distance_v2 = float(int(str(chromosome[V2_DISTANCE_INDEX_1]) + str(chromosome[V2_DISTANCE_INDEX_2])) / 50)
    v2_pos_bg = getV2BeamNGCoordinaes(total_distance_v2, chromosome[V2_WIDTH_INDEX] % 4)  # get beamng coordinates (polyline coordinate). it will be always calculated from center - joint

    return v1_speed, v1_pos_bg, v2_speed, v2_pos_bg, impact_point


# --------------------------- genetic algorithm helper  ----------------------

def path_generator(vehicle_population, num_points = 3, extra_points = 0, is_striker = True):
    # vehicle_population (required): object to extract starting and collision coordinates to drive the vehicle
    # num_points (required): the expected amount of points requires to generate path
    # extra_points (optional): the amount of points requires to drive the car further after reaching the num_point

    script = list() # Properties for ai_set_script()

    # Properties for add_debug_line()
    points = list()
    point_colors = list()
    spheres = list()
    sphere_colors = list()


    for i in range(num_points + extra_points):
        # Values for ai_set_script()
        node = {
            'z': vehicle_population['pos_z'],
            't': (2 * i + (np.abs(np.sin(np.radians(i)))) * 32) / 32,
        }
        if is_striker: 
            # Run from bottom to top so pos_x (horizontal) remains constant 
            # while the pos_y (vertical) will be calculated by the value of starting point in pos_y
            # and the value which is substracted by collision y and starting y and devided by the number of expected points
            node['x'] = vehicle_population['pos_x']
            expected_point = (vehicle_population['col_y'] - vehicle_population['pos_y'])/num_points
            node['y'] = vehicle_population['pos_y'] + expected_point*i
        else:
            expected_point = (vehicle_population['col_x'] - vehicle_population['pos_x'])/num_points
            node['x'] = vehicle_population['pos_x'] +  expected_point* i
            node['y'] = vehicle_population['pos_y']            
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

# Process CSV file
def process_csv_file(csv_mode, csv_path, pos_crash_dict = None):
    # csv_mode is 'w' for writing or 'a' for appending

    csv_columns = [
        'chromosome',
        'v1_speed', 'v1_waypoint', 
        'v2_speed','v2_waypoint', 
        'striker_damage', 'victim_damage',
        'striker_distance', 'victim_distance', 
        'striker_rotation', 'victim_rotation', 
        'fitness_value'
    ]
    try:
        with open(csv_path, csv_mode, encoding='utf-8') as csv_file:
            writer = csv.DictWriter(csv_file, fieldnames=csv_columns, delimiter=',', lineterminator='\n')
            if (csv_mode == 'w'):
                writer.writeheader()
            else:
                writer.writerow(pos_crash_dict)
                print(pos_crash_dict)
    except IOError:
        print("I/O error")

# Read JSON data
def read_json_data(scenario_path):
    try:
        # Open file with file path
        with open(scenario_path) as file:
            scenario_data = json.load(file)

        street_list = [] # Define array of roads in json
        road_num = scenario_data['road_num'] # Number of roads
        road_id = 1
        while (road_id <= road_num):
            # Convert a single point of road_node_list to tuple
            points = []
            road_node_list = scenario_data["r"+ str(road_id) + '_road_node_list']
            for point in road_node_list:
                points.append(convert(point))

            # Create an instance of road
            street = Street(
                id = road_id,
                type = scenario_data["r"+ str(road_id) + "_road_type"],
                shape = scenario_data["r"+ str(road_id) + "_road_shape"],
                points = points
            )
            street_list.append(street)
            road_id+=1

        car_list = [] # Define array of vehicles in json
        vehicle_num = scenario_data['vehicle_num'] # Number of vehicles
        vehicle_id = 1
        while (vehicle_id <= vehicle_num):
            # Convert a single point of vehicle_points and rot_degrees to tuple
            points = []
            vehicle_points = scenario_data["v"+ str(vehicle_id) + "_points"]
            for point in vehicle_points:
                points.append(convert(point))

            rot_degrees = []
            rot_degree = scenario_data["v"+ str(vehicle_id) + "_rot_degree"]
            for el in rot_degree:
                rot_degrees.append(convert(el))

            # Instance of vehicle
            car = Car(
                id = vehicle_id,
                velocities = scenario_data["v"+ str(vehicle_id) + "_velocities"],
                color = scenario_data["v"+ str(vehicle_id) + "_color"],
                rot_degree = rot_degrees,
                travelling_dir = scenario_data["v"+ str(vehicle_id) + "_travelling_dir"],
                damage = scenario_data["v"+ str(vehicle_id) + "_damage_components"],
                points = points
            )
            car_list.append(car)
            vehicle_id+=1


        # Instance of accident case
        accident_case = AccidentCase(
            name = sys.argv[1],
            crash_point = scenario_data['crash_point'],
            car_list = car_list,
            street_list = street_list
        )
        
        return accident_case
    except Exception as e:
        print("Oops!", e.__class__, "occurred.")
        print("Details: ", e, ".")
        exit()
