import os
import sys
import numpy as np
from beamngpy import Scenario, Road, Vehicle
# from road_factory import RoadFactory
from libraries.libs import read_json_data

# --------------------------------------------------------------------------
# Define json data file
if sys.argv[1] == 'pycharm':
    scenario_path = os.getcwd() + '\\assets\\' + sys.argv[2]
else:
    scenario_path = os.getcwd() + '\\demo\\BeamNGpy\\assets\\' + sys.argv[1]

accident_case = read_json_data(scenario_path)

# Define basic vector to find angle
head_south_vector = np.array([0, -1, 0])
head_north_vector = np.array([0, 1, 0])
head_east_vector = np.array([1, 0, 0])
head_west_vector = np.array([-1, 0, 0])

# Executing the scenario
ac3r = BeamNg()
bng_instance = ac3r.init_beamng()
scenario = Scenario('smallgrid', 'test_01')

# JSON READ: Building scenario's streets
street_list = accident_case.street_list
for street in street_list:
    road = Road('road_asphalt_2lane', rid='road_' + str(street.id))
    road.nodes.extend(street.points)
    scenario.add_road(road)

# JSON READ: Building scenario's vehicle
car_list = accident_case.car_list
vehicles = []
for car in car_list:
    vehicle = Vehicle("scenario_player_" + str(car.id), model="etk800", licence=car.name, color=car.color)
    vehicles.append(vehicle)
    scenario.add_vehicle(vehicle, pos=car.points[0], rot=None, rot_quat=car.rot_degree[0])

# Visualization the Road from JSON file
road_factory = RoadFactory()
the_tests = []
for street in street_list:
    road_test = street.get_road_test()
    if road_test is not None:
        the_road_test = road_factory.generate_straight_road_test(road_test, 10)
        the_tests.append(the_road_test)
    else:
        road_points = []
        road_points.extend([[point[0], point[1]] for point in street.points])
        the_road_test = road_factory.generate_road_test(road_points)
        the_tests.append(the_road_test)


# visualizer = RoadTestVisualizer(map_size=300)
# visualizer.visualize_road_test(the_tests[0])
road_factory.visualize_road_test(map_size=600, road_test=the_tests)

# # AI_SET_SCRIPT #
# orig = car_list[0].points[0]
# script = list()
# points = list()
# point_colors = list()
# spheres = list()
# sphere_colors = list()
#
# for i in range(3600):
#     node = {
#         #  Calculate the position as a sinus curve that makes the vehicle
#         #  drive from left to right. The z-coordinate is not calculated in
#         #  any way because `ai_set_script` by default makes the polyline to
#         #  follow cling to the ground, meaning the z-coordinate will be
#         #  filled in automatically.
#         'x': 4 * np.sin(np.radians(i)) + orig[0],
#         'y': i * -0.2 + orig[1],
#         'z': orig[2],
#         #  Calculate timestamps for each node such that the speed between
#         #  points has a sinusoidal variance to it.
#         't': (2 * i + (np.abs(np.sin(np.radians(i)))) * 64) / 64,
#     }
#     print(node)
#     script.append(node)
#     points.append([node['x'], node['y'], node['z']])
#     point_colors.append([0, np.sin(np.radians(i)), 0, 0.1])
#
#     if i % 10 == 0:
#         spheres.append([node['x'], node['y'], node['z'], np.abs(np.sin(np.radians(i))) * 0.25])
#         sphere_colors.append([np.sin(np.radians(i)), 0, 0, 0.8])
#
# ######################################################################################################
# scenario.make(bng_instance)
# bng_instance.open(launch=True)
# try:
#     bng_instance.load_scenario(scenario)
#     bng_instance.start_scenario()
#     bng_instance.add_debug_line(points, point_colors,
#                        spheres=spheres, sphere_colors=sphere_colors,
#                        cling=True, offset=0.1)
#     # vehicles[0].ai_set_script(script)
#     vehicles[0].ai_set_speed(1, mode='set')
#     vehicles[0].ai_set_mode('random')
#     input('Press enter when done...')
#     while True:
#         bng_instance.step(60)
# finally:
#     bng_instance.close()