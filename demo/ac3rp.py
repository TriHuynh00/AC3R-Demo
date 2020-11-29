import os
import sys
import json
from beamngpy import setup_logging
from BeamNGpy.beamng import BeamNg
from BeamNGpy.accident_case import AccidentCase
from BeamNGpy.street import Street
from BeamNGpy.car import Car

# Define json data file
scenario_path = os.getcwd() + '\\accidentCasesJSON\\' + sys.argv[1]

# Read JSON data
try:
    with open(scenario_path) as f:
        scenario_data = json.load(f)

    street_list = []
    road_num = scenario_data['road_num']
    road_id = 1
    while (road_id <= road_num):
        street = Street(
            id = road_id,
            type = scenario_data['road_type_' + str(road_id)],
            shape = scenario_data['road_shape_' + str(road_id)],
            points = scenario_data['road_node_list_' + str(road_id)]
            )
        street_list.append(street)
        road_id+=1

    car_list = []
    vehicle_num = scenario_data['vehicle_num']
    vehicle_id = 1
    while (vehicle_id <= vehicle_num):
        car = Car(
            id = vehicle_id,
            velocities = scenario_data["v"+ str(vehicle_id) + "_velocities"],
            rot_degree = scenario_data["v"+ str(vehicle_id) + "_rot_degree"],
            travelling_dir = scenario_data["v"+ str(vehicle_id) + "_travelling_dir"],
            damage = scenario_data["v"+ str(vehicle_id) + "_damage_components"]
        )
        car_list.append(car)
        vehicle_id+=1


    accident_case = AccidentCase(
        name = sys.argv[1],
        crash_point = scenario_data['crash_point'],
        car_list = car_list,
        street_list = street_list
    )
    print(accident_case)
    # Executing the scenario
    # setup_logging()

    # ac3r = BeamNg()
    # bng = ac3r.start_beamng()
except Exception as e:
    print("Oops!", e.__class__, "occurred.")
    exit()

