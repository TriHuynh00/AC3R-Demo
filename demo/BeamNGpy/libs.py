import sys
import json
from BeamNGpy.accident_case import AccidentCase
from BeamNGpy.street import Street
from BeamNGpy.car import Car

# list into a tuple 
def convert(list): 
    return tuple(list) 

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
