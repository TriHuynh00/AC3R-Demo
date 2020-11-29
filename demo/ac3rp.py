import os
import sys
import json
from BeamNGpy.beamng import BeamNg
from BeamNGpy.libs import read_json_data
from beamngpy import BeamNGpy, Scenario, Road, Vehicle, setup_logging

# Define json data file
scenario_path = os.getcwd() + '\\accidentCasesJSON\\' + sys.argv[1]
accident_case = read_json_data(scenario_path)

# Executing the scenario
ac3r = BeamNg()
beamng = ac3r.start_beamng()
scenario = Scenario('smallgrid', 'test_01')

street_list = accident_case.street_list
for street in street_list:
    road_rid = 'road_' + str(street.id)
    road = Road('road_asphalt_2lane', rid=road_rid)
    nodes = street.points
    road.nodes.extend(nodes)
    scenario.add_road(road)

car_list = accident_case.car_list
for car in car_list:
    vid = "scenario_player_" + str(car.id)
    model = "etk800"
    color = car.color
    licence = car.name
    pos = car.points[0]
    rot_quat = car.rot_degree[0]
    vehicle = Vehicle(vid, model=model, licence=licence, color=color)
    scenario.add_vehicle(vehicle, pos=pos, rot=None, rot_quat=rot_quat)
    

scenario.make(beamng)
bng = beamng.open(launch=True)
try:
    bng.load_scenario(scenario)
    bng.start_scenario()

    input('Press enter when done...')
finally:
    bng.close()