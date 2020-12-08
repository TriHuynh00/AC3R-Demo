import os
import csv
import sys
import json
import time
import numpy as np
from beamngpy import BeamNGpy, Scenario, Road, Vehicle, setup_logging
from beamngpy.sensors import Damage
from datetime import datetime
from libs import read_json_data, path_generator
from helper_algorithm import generateRandomPopulation, decoding_of_parameter, tournament_parent_selection, crossover_mutation
from helper_crash import AngleBtw2Points, angle_between
from helper_vehicle import DamageExtraction, DistanceExtraction, RotationExtraction

# ----------------------------- create csv file --------------------------
pos_crash_dict = {}

csv_columns = ['chromosome', 'v1_speed', 'v1_waypoint', 'v2_speed', 'v2_waypoint', 'striker_damage', 'victim_damage',
               'striker_distance', 'victim_distance', 'striker_rotation', 'victim_rotation', 'fitness_value']
csv_file = "pos_crash_analysis.csv"
try:
    with open(csv_file, 'w', encoding='utf-8') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=csv_columns, delimiter=',', lineterminator='\n')
        writer.writeheader()
except IOError:
    print("I/O error")


# -------------------------------------------------------------------------

def saveDictionaryToCsvFile():
    csv_columns = ['chromosome', 'v1_speed', 'v1_waypoint', 'v2_speed', 'v2_waypoint', 'striker_damage',
                   'victim_damage', 'striker_distance', 'victim_distance', 'striker_rotation', 'victim_rotation',
                   'fitness_value']
    csv_file = "pos_crash_analysis.csv"
    try:
        with open(csv_file, 'a', encoding='utf-8') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=csv_columns, delimiter=',', lineterminator='\n')
            writer.writerow(pos_crash_dict)
            print(pos_crash_dict)
    except IOError:
        print("I/O error")


# -------------------------------------------------------------------------

actual_striker_damage = ""
actual_victim_damage = ""
crash_fitness_function = False
distance_fitness_function = False
rotation_fitness_function = False
# read fitness function json file.

actual_striker_damage = "F"
actual_victim_damage = "R"
crash_fitness_function = True
distance_fitness_function = False
rotation_fitness_function = False

print(actual_striker_damage)
print(actual_victim_damage)
print(crash_fitness_function)
print(distance_fitness_function)
print(rotation_fitness_function)

# --------------------------------------------------------------------------
# Define json data file
scenario_path = os.getcwd() + '\\demo\\BeamNGpy\\assets\\' + sys.argv[1]
# scenario_path = os.getcwd() + '\\assets\\' + sys.argv[1]
accident_case = read_json_data(scenario_path)

setup_logging()
beamng = BeamNGpy('127.0.0.1', 64256, home=os.getenv('BNG_HOME'))
scenario = Scenario('smallgrid', 'crash_simulation_1')

# JSON READ: Collect Crash point
impact_x = accident_case.crash_point[0]
impact_y = accident_case.crash_point[1]
impact_z = accident_case.crash_point[2]

# JSON READ: Build scenario's streets
street_list = accident_case.street_list
for street in street_list:
    road_rid = 'road_' + str(street.id)
    road = Road('road_asphalt_2lane', rid=road_rid)
    nodes = street.points
    road.nodes.extend(nodes)
    scenario.add_road(road)

collision_point = []
four_way = []

# Define basic vector to find angle
head_south_vector = np.array([0, -1, 0])
head_north_vector = np.array([0, 1, 0])
head_east_vector = np.array([1, 0, 0])
head_west_vector = np.array([-1, 0, 0])

# JSON READ: Build scenario's vehicle
car_list = accident_case.car_list
vehicle_dict = {}
for car in car_list:
    vid = "scenario_player_" + str(car.id)
    model = "etk800"
    color = car.color
    licence = car.name
    vehicle = Vehicle(vid, model=model, licence=licence, color=color)
    car.set_vehicle(vehicle)
    if car.id == 1:
        vehicle_dict['striker'] = car
    else:
        vehicle_dict['victim'] = car

# Using by beamng
vehicleStriker = vehicle_dict['striker'].vehicle
vehicleVictim = vehicle_dict['victim'].vehicle

damageStriker = Damage()
vehicleStriker.attach_sensor('damagesS', damageStriker)
damageVictim = Damage()
vehicleVictim.attach_sensor('damagesV', damageVictim)

# roads for striker and victim vehicle.
road_striker = [vehicle_dict['striker'].points[0], (impact_x, impact_y)]
road_victim = [vehicle_dict['victim'].points[0], (impact_x, impact_y)]

# actual_striker_damage = "F"
# actual_victim_damage = "R"

# parameters for vehicle state extraction
positions = list()
directions = list()
damages = list()

populations_fitness = {}  # fitness function to store fitness values of chromosomes.

# initial population
populations = generateRandomPopulation(5, 10)
print('initial population')
print(populations)

# code to run the simulation and set the fitness of the function.
for population in populations:
    collision_points = []
    striker_points = []
    victim_points = []
    striker_speeds = []
    victim_speeds = []

    beamng_parameters = decoding_of_parameter(population, impact_x, impact_y, road_striker, road_victim)
    striker_speeds.append(beamng_parameters[0])
    striker_points.append(beamng_parameters[1])
    victim_speeds.append(beamng_parameters[2])
    victim_points.append(beamng_parameters[3])
    collision_points.append(beamng_parameters[4])

    # create beamng scenario and run the simulation.
    # Add it to our scenario at this position and rotation

    # alpha = AngleBtw2Points([5,5],[7,4])
    striker_alpha = AngleBtw2Points(road_striker[1], road_striker[0])
    victim_alpha = AngleBtw2Points(road_victim[1], road_victim[0])

    # save values to dictionary
    pos_crash_dict["chromosome"] = population
    pos_crash_dict["v1_speed"] = striker_speeds[0]
    pos_crash_dict["v1_waypoint"] = striker_points[0]
    pos_crash_dict["v2_speed"] = victim_speeds[0]
    pos_crash_dict["v2_waypoint"] = victim_points[0]

    striker_target_vector = np.array([beamng_parameters[4][0], beamng_parameters[4][1], 0]) - \
                            np.array([beamng_parameters[1][0], beamng_parameters[1][1], 0])
    victim_target_vector  = np.array([beamng_parameters[4][0], beamng_parameters[4][1], 0]) - \
                            np.array([beamng_parameters[3][0], beamng_parameters[3][1], 0])

    striker_population = {
        'speed': vehicle_dict['striker'].get_velocities()[0],  # starting speed
        'col_speed': beamng_parameters[0],  # collision speed
        # starting point
        'pos_x': beamng_parameters[1][0],
        'pos_y': beamng_parameters[1][1],
        'pos_z': 0,
        # collision point
        'col_x': beamng_parameters[4][0],
        'col_y': beamng_parameters[4][1],
        'col_z': 0,
        # rotation coordinate
        'rot_x': 0,
        'rot_y': 0,
        'rot_z': angle_between(head_south_vector, striker_target_vector, vehicle_dict['striker'].get_direction()),
    }

    victim_population = {
        'speed': vehicle_dict['victim'].get_velocities()[0],  # starting speed
        'col_speed': beamng_parameters[2],  # collision speed
        # starting point
        'pos_x': beamng_parameters[3][0],
        'pos_y': beamng_parameters[3][1],
        'pos_z': 0,
        # collision point
        'col_x': beamng_parameters[4][0],
        'col_y': beamng_parameters[4][1],
        'col_z': 0,
        # rotation coordinate
        'rot_x': 0,
        'rot_y': 0,
        'rot_z': angle_between(head_south_vector, victim_target_vector, vehicle_dict['victim'].get_direction()),
    }

    print('\n =========== angle =========')
    print(striker_population)
    print(victim_population)
    print('striker: ', str(striker_population['rot_z']))
    print('victim: ', str(victim_population['rot_z']))
    print('=========== angle ========= \n')

    scenario.add_vehicle(
        vehicleStriker,
        pos=(striker_population['pos_x'], striker_population['pos_y'], striker_population['pos_z']),
        rot=(striker_population['rot_x'], striker_population['rot_y'], striker_population['rot_z'])
    )
    scenario.add_vehicle(
        vehicleVictim,
        pos=(victim_population['pos_x'], victim_population['pos_y'], victim_population['pos_z']),
        rot=(victim_population['rot_x'], victim_population['rot_y'], victim_population['rot_z'])
    )

    # Generate path for striker and victim
    striker_path = path_generator(striker_population)
    victim_path = path_generator(victim_population)

    scenario.make(beamng)
    bng = beamng.open(launch=True)
    bng.set_deterministic()

    try:
        bng.load_scenario(scenario)
        bng.start_scenario()

        bng.add_debug_line(striker_path['points'], striker_path['point_colors'],
                           spheres=striker_path['spheres'], sphere_colors=striker_path['sphere_colors'],
                           cling=True, offset=0.1)
        bng.add_debug_line(victim_path['points'], victim_path['point_colors'],
                           spheres=victim_path['spheres'], sphere_colors=victim_path['sphere_colors'],
                           cling=True, offset=0.1)

        vehicleStriker.ai_set_script(striker_path['script'])
        vehicleVictim.ai_set_script(victim_path['script'])

        for number in range(60):
            time.sleep(0.20)

            vehicleStriker.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
            sensors = bng.poll_sensors(vehicleStriker)  # Polls the data of all sensors attached to the vehicle
            # print(vehicleStriker.state['pos'])
            if vehicleStriker.state['pos'][0] > impact_x and vehicleStriker.state['pos'][1] > impact_y:
                # print('free state')
                vehicleStriker.control(throttle=0, steering=0, brake=0, parkingbrake=0)
                vehicleStriker.update_vehicle()

            vehicleVictim.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
            sensors = bng.poll_sensors(vehicleVictim)  # Polls the data of all sensors attached to the vehicle
            # print(vehicleStriker.state['pos'])
            if vehicleVictim.state['pos'][0] > impact_x and vehicleVictim.state['pos'][1] > impact_y:
                # print('free state')
                vehicleVictim.control(throttle=0, steering=0, brake=0, parkingbrake=0)
                vehicleVictim.update_vehicle()

            if number > 58:

                # striker vehhicle state extraction
                striker_damage = {}
                vehicleStriker.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
                sensorsStriker = bng.poll_sensors(
                    vehicleStriker)  # Polls the data of all sensors attached to the vehicle
                striker_position = vehicleStriker.state['pos']
                striker_direction = vehicleStriker.state['dir']
                if 'damagesS' in sensorsStriker:
                    striker_damage = sensorsStriker['damagesS']

                # victim vehicle state extraction
                victim_damage = {}
                vehicleVictim.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
                sensorsVictim = bng.poll_sensors(vehicleVictim)  # Polls the data of all sensors attached to the vehicle
                victim_position = vehicleVictim.state['pos']
                victim_direction = vehicleVictim.state['dir']
                if 'damagesV' in sensorsVictim:
                    victim_damage = sensorsVictim['damagesV']

                # multiobjective fitness function.
                multiObjectiveFitnessScore = 0
                critical_damage_score = (0, 0)
                distance_score = (0, 0)
                rotation_score = (0, 0)

                if crash_fitness_function:
                    critical_damage_score = DamageExtraction(striker_damage, victim_damage, actual_striker_damage,
                                                             actual_victim_damage)
                if distance_fitness_function:
                    distance_score = DistanceExtraction(striker_speeds[0], striker_position, collision_points[0],
                                                        victim_speeds[0], victim_position, collision_points[0])
                if rotation_fitness_function:
                    rotation_score = RotationExtraction(striker_points[0], collision_points[0], striker_position,
                                                        victim_points[0], collision_points[0], victim_position)

                print("multiObjectiveFitnessScore")
                print(critical_damage_score)
                print(distance_score)
                print(rotation_score)

                multiObjectiveFitnessScore = critical_damage_score[0] + critical_damage_score[1]
                multiObjectiveFitnessScore = multiObjectiveFitnessScore + distance_score[0] + distance_score[1]
                multiObjectiveFitnessScore = multiObjectiveFitnessScore + rotation_score[0] + rotation_score[1]
                print(multiObjectiveFitnessScore)

                # set the fitness function value
                populations_fitness[tuple(population)] = multiObjectiveFitnessScore

                # save value to dictionary.

                pos_crash_dict["striker_damage"] = critical_damage_score[0]
                pos_crash_dict["victim_damage"] = critical_damage_score[1]
                pos_crash_dict["striker_distance"] = distance_score[0]
                pos_crash_dict["victim_distance"] = distance_score[1]
                pos_crash_dict["striker_rotation"] = rotation_score[0]
                pos_crash_dict["victim_rotation"] = rotation_score[1]
                pos_crash_dict["fitness_value"] = multiObjectiveFitnessScore

                if critical_damage_score[0] > 0 or critical_damage_score[1] > 0:
                    print("critical scenario")
                    saveDictionaryToCsvFile()

                break

        bng.kill_beamng()

    finally:
        bng.close()

# ---------------------------- save genetic algorithm iteration-----------------------------------------
f = open("genetic_algorithm_iteration.csv", "w+")
# -------------- save genetic algorithm iterator -------------------------------------
lines = ""
for k, v in populations_fitness.items():
    lines = lines + str(k) + ',' + str(v) + ','

lines = lines[:-1]
f.writelines(lines + '\n')
print(lines)

print('\n\n --------------------------------')
print('Start 2nd loop')
print('--------------------------------\n\n')
exit()


# -------------------------------- genetic algorithm helper --------------------------

# exit()


# -------------------------------- genetic algorithm helper --------------------------

# iteration of genetic algorithm.
for _ in range(5):  # Number of Generations to be Iterated.
    print("genetic algorithm simulation")
    selected_parents = tournament_parent_selection(populations, populations_fitness)
    next_population = crossover_mutation(selected_parents=selected_parents)
    print("population")
    print(populations)
    print("selected parents " + str(selected_parents))
    print("next population " + str(next_population))

    for children in next_population:
        print("iteration of children")
        collision_points = []
        striker_points = []
        victim_points = []
        striker_speeds = []
        victim_speeds = []

        beamng_parameters = decoding_of_parameter(population, impact_x, impact_y, road_striker, road_victim)
        print(beamng_parameters)
        striker_speeds.append(beamng_parameters[0])
        striker_points.append(beamng_parameters[1])
        victim_speeds.append(beamng_parameters[2])
        victim_points.append(beamng_parameters[3])
        collision_points.append(beamng_parameters[4])

        striker_alpha = AngleBtw2Points(road_striker[0], road_striker[1])
        victim_alpha = AngleBtw2Points(road_victim[0], road_victim[1])

        striker_target_vector = np.array([beamng_parameters[4][0], beamng_parameters[4][1], 0]) - \
                                np.array([beamng_parameters[1][0], beamng_parameters[1][1], 0])
        victim_target_vector = np.array([beamng_parameters[4][0], beamng_parameters[4][1], 0]) - \
                               np.array([beamng_parameters[3][0], beamng_parameters[3][1], 0])

        striker_population = {
            'speed': vehicle_dict['striker'].get_velocities()[0],  # starting speed
            'col_speed': beamng_parameters[0],  # collision speed
            # starting point
            'pos_x': beamng_parameters[1][0],
            'pos_y': beamng_parameters[1][1],
            'pos_z': 0,
            # collision point
            'col_x': beamng_parameters[4][0],
            'col_y': beamng_parameters[4][1],
            'col_z': 0,
            # rotation coordinate
            'rot_x': 0,
            'rot_y': 0,
            'rot_z': angle_between(head_south_vector, striker_target_vector, vehicle_dict['striker'].get_direction()),
        }

        victim_population = {
            'speed': vehicle_dict['victim'].get_velocities()[0],  # starting speed
            'col_speed': beamng_parameters[2],  # collision speed
            # starting point
            'pos_x': beamng_parameters[3][0],
            'pos_y': beamng_parameters[3][1],
            'pos_z': 0,
            # collision point
            'col_x': beamng_parameters[4][0],
            'col_y': beamng_parameters[4][1],
            'col_z': 0,
            # rotation coordinate
            'rot_x': 0,
            'rot_y': 0,
            'rot_z': angle_between(head_south_vector, victim_target_vector, vehicle_dict['victim'].get_direction()),
        }

        scenario.add_vehicle(
            vehicleStriker,
            pos=(striker_population['pos_x'], striker_population['pos_y'], striker_population['pos_z']),
            rot=(striker_population['rot_x'], striker_population['rot_y'], striker_population['rot_z'])
        )
        scenario.add_vehicle(
            vehicleVictim,
            pos=(victim_population['pos_x'], victim_population['pos_y'], victim_population['pos_z']),
            rot=(victim_population['rot_x'], victim_population['rot_y'], victim_population['rot_z'])
        )

        # save values to dictionary
        pos_crash_dict["chromosome"] = children
        pos_crash_dict["v1_speed"] = striker_speeds[0]
        pos_crash_dict["v1_waypoint"] = striker_points[0]
        pos_crash_dict["v2_speed"] = victim_speeds[0]
        pos_crash_dict["v2_waypoint"] = victim_points[0]

        scenario.make(beamng)

        striker_path = path_generator(striker_population)
        victim_path = path_generator(victim_population)

        bng = beamng.open(launch=True)
        bng.set_deterministic()

        try:
            bng.load_scenario(scenario)
            bng.start_scenario()
            bng.add_debug_line(striker_path['points'], striker_path['point_colors'],
                               spheres=striker_path['spheres'], sphere_colors=striker_path['sphere_colors'],
                               cling=True, offset=0.1)
            bng.add_debug_line(victim_path['points'], victim_path['point_colors'],
                               spheres=victim_path['spheres'], sphere_colors=victim_path['sphere_colors'],
                               cling=True, offset=0.1)

            vehicleStriker.ai_set_script(striker_path['script'])
            vehicleVictim.ai_set_script(victim_path['script'])

            # vehicle state extraction and fitness function here
            for number in range(60):
                time.sleep(0.20)

                vehicleStriker.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
                sensors = bng.poll_sensors(vehicleStriker)  # Polls the data of all sensors attached to the vehicle
                # print(vehicleStriker.state['pos'])
                if vehicleStriker.state['pos'][0] > impact_x and vehicleStriker.state['pos'][1] > impact_y:
                    # print('free state')
                    vehicleStriker.control(throttle=0, steering=0, brake=0, parkingbrake=0)
                    vehicleStriker.update_vehicle()

                vehicleVictim.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
                sensors = bng.poll_sensors(vehicleVictim)  # Polls the data of all sensors attached to the vehicle
                # print(vehicleStriker.state['pos'])
                if vehicleVictim.state['pos'][0] > impact_x and vehicleVictim.state['pos'][1] > impact_y:
                    # print('free state')
                    vehicleVictim.control(throttle=0, steering=0, brake=0, parkingbrake=0)
                    vehicleVictim.update_vehicle()

                if number > 58:

                    # striker vehhicle state extraction
                    striker_damage = {}
                    vehicleStriker.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
                    sensorsStriker = bng.poll_sensors(
                        vehicleStriker)  # Polls the data of all sensors attached to the vehicle
                    striker_position = vehicleStriker.state['pos']
                    striker_direction = vehicleStriker.state['dir']
                    if 'damagesS' in sensorsStriker:
                        striker_damage = sensorsStriker['damagesS']

                    # victim vehicle state extraction
                    victim_damage = {}
                    vehicleVictim.update_vehicle()  # Synchs the vehicle's "state" variable with the simulator
                    sensorsVictim = bng.poll_sensors(
                        vehicleVictim)  # Polls the data of all sensors attached to the vehicle
                    victim_position = vehicleVictim.state['pos']
                    victim_direction = vehicleVictim.state['dir']
                    if 'damagesV' in sensorsVictim:
                        victim_damage = sensorsVictim['damagesV']

                    # multiobjective fitness function.
                    multiObjectiveFitnessScore = 0
                    critical_damage_score = (0, 0)
                    distance_score = (0, 0)
                    rotation_score = (0, 0)

                    if crash_fitness_function:
                        critical_damage_score = DamageExtraction(striker_damage, victim_damage, actual_striker_damage,
                                                                 actual_victim_damage)
                    if distance_fitness_function:
                        distance_score = DistanceExtraction(striker_speeds[0], striker_position, collision_points[0],
                                                            victim_speeds[0], victim_position, collision_points[0])
                    if rotation_fitness_function:
                        rotation_score = RotationExtraction(striker_points[0], collision_points[0], striker_position,
                                                            victim_points[0], collision_points[0], victim_position)

                    print("multiObjectiveFitnessScore")
                    print(critical_damage_score)
                    print(distance_score)
                    print(rotation_score)

                    multiObjectiveFitnessScore = critical_damage_score[0] + critical_damage_score[1]
                    multiObjectiveFitnessScore = multiObjectiveFitnessScore + distance_score[0] + distance_score[1]
                    multiObjectiveFitnessScore = multiObjectiveFitnessScore + rotation_score[0] + rotation_score[1]
                    print(multiObjectiveFitnessScore)

                    # set the fitness function value
                    populations_fitness[tuple(children)] = multiObjectiveFitnessScore

                    # save value to dictionary.

                    pos_crash_dict["striker_damage"] = critical_damage_score[0]
                    pos_crash_dict["victim_damage"] = critical_damage_score[1]
                    pos_crash_dict["striker_distance"] = distance_score[0]
                    pos_crash_dict["victim_distance"] = distance_score[1]
                    pos_crash_dict["striker_rotation"] = rotation_score[0]
                    pos_crash_dict["victim_rotation"] = rotation_score[1]
                    pos_crash_dict["fitness_value"] = multiObjectiveFitnessScore

                    if critical_damage_score[0] > 0 or critical_damage_score[1] > 0:
                        print("critical scenario")
                        saveDictionaryToCsvFile()

                    break

            # input('Press enter when done...')
            bng.kill_beamng()


        finally:
            bng.close()

        print("adding children")
        print(populations_fitness)

        populations_fitness_tuples = sorted(populations_fitness.items(), key=lambda x: x[1], reverse=True)
        populations_fitness = dict((x, y) for x, y in populations_fitness_tuples)
        print(populations_fitness)
        populations_fitness.popitem()
        print("length " + str(len(populations_fitness)))
        populations = list(populations_fitness.keys())

        # -------------- save genetic algorithm iterator -------------------------------------
        lines = ""
        for k, v in populations_fitness.items():
            lines = lines + str(k) + ',' + str(v) + ','

        print("genetic algorithm saving iteration")
        print(lines)
        lines = lines[:-1]
        f.writelines(lines + '\n')

        # -------------------------------- genetic algorithm helper --------------------------

        # iteratoin of genetic algorithm finished.

print(datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
#
# scenario.make(beamng)
# bng = beamng.open(launch=True)
# try:
#     bng.load_scenario(scenario)
#     bng.start_scenario()
#
#     input('Press enter when done...')
# finally:
#     bng.close()
