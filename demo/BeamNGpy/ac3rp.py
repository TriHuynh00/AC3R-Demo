import random
import csv
import sys
import json
import os
import pickle
import time
from time import sleep

from beamngpy import BeamNGpy, Scenario, Road, Vehicle, setup_logging
from beamngpy.sensors import Electrics, Damage, Camera

from CrashHelper.crash_simulation_helper import AngleBtw2Points, getDistance
from CrashHelper.vehicle_state_helper import DamageExtraction, DistanceExtraction, RotationExtraction

from beamng import BeamNg
from libs import read_json_data, process_csv_file, generate_random_population, decoding_of_parameter, path_generator

# Define json data file
scenario_path = os.getcwd() + '\\assets\\' + sys.argv[1]
accident_case = read_json_data(scenario_path)

# ----------------------------- create csv file --------------------------
pos_crash_dict = {}
csv_path = os.getcwd() + '\\assets\\' + "pos_crash_analysis.csv"
process_csv_file("w", csv_path)

# -------------------------------------------------------------------------

def saveDictionaryToCsvFile():
    csv_columns = ['chromosome', 'v1_speed', 'v1_waypoint', 'v2_speed', 'v2_waypoint', 'striker_damage',
                   'victim_damage', 'striker_distance', 'victim_distance', 'striker_rotation', 'victim_rotation',
                   'fitness_value']
    csv_file = "assets\\pos_crash_analysis.csv"
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
distance_fitness_function =  False
rotation_fitness_function = False
# read fitness function json file.
with open('assets\\fitness_function_1.json') as json_file:
    data = json.load(json_file)

    actual_striker_damage = data['actual_striker_damage']
    actual_victim_damage = data['actual_victim_damage']
    crash_fitness_function = data['crash_fitness_function']
    distance_fitness_function =  data['distance_fitness_function']
    rotation_fitness_function = data['rotation_fitness_function']

    print(actual_striker_damage)
    print(actual_victim_damage)
    print(crash_fitness_function)
    print(distance_fitness_function)
    print(rotation_fitness_function)

# --------------------------------------------------------------------------

# beamng = BeamNGpy('localhost', 64256, home='F:\Softwares\BeamNG_Research_SVN')
# scenario = Scenario('GridMap', 'crash_simulation_1')
setup_logging()
ac3r = BeamNg()
beamng = ac3r.start_beamng()
scenario = Scenario('smallgrid', 'crash_simulation_1')

collision_point =[]
four_way = []

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
    car.set_vehicle(vehicle)

    scenario.add_vehicle(vehicle, pos=pos, rot=None, rot_quat=rot_quat)

vehicleStriker = ''
vehicleVictim = ''
for car in car_list:
    if (car.id == 1):
        vehicleStriker = car.vehicle
    else:
        vehicleVictim = car.vehicle

damageStriker = Damage()
vehicleStriker.attach_sensor('damagesS', damageStriker)
damageVictim = Damage()
vehicleVictim.attach_sensor('damagesV', damageVictim)

# roads for striker and victim vehicle.
# road_striker = [(241, 72),(238, 143)]
# road_victim =  [(167, 139),(238, 143)]

# road_striker = [(-2.0,17.880000000000006),(-2.0,2.0)]
# road_victim =  [(20.88,18.88),(-2.0,2.0)]

road_striker = [(-2.0,86.84551724137933,0),(-2.0,2.0,0)]
road_victim =  [(89.845517,87.845517,0),(-2.0,2.0,0)]

#actual_striker_damage = "F"
#actual_victim_damage = "R"

# parameters for vehicle state extraction
positions = list()
directions = list()
damages = list()

populations_fitness = {} # fitness function to store fitness values of chromosomes.

#initial population
populations = generate_random_population(5,10)
print('initial population')
print(populations)

impact_y = 2
impact_x = -2

# code to run the simulation and set the fitness of the function.
for population in populations:
    collision_points = []
    striker_points = []
    victim_points = []
    striker_speeds = []
    victim_speeds = []

    beamng_parameters = decoding_of_parameter(population)
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

    striker_population = {
        'speed': 56, # starting speed
        'col_speed': beamng_parameters[0], # collision speed
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
        'rot_z': 0,
    }

    victim_population = {
        'speed': 56, # starting speed
        'col_speed': beamng_parameters[2], # collision speed
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
        'rot_z': 90,
    }

    scenario.add_vehicle(
        vehicleStriker, 
        pos = (striker_population['pos_x'], striker_population['pos_y'], striker_population['pos_z']), 
        rot = (striker_population['rot_x'], striker_population['rot_y'], striker_population['rot_z'])
    )
    scenario.add_vehicle(
        vehicleVictim, 
        pos = (victim_population['pos_x'], victim_population['pos_y'], victim_population['pos_z']), 
        rot = (victim_population['rot_x'], victim_population['rot_y'], victim_population['rot_z'])
    )

    # Generate path for striker and victim
    striker_path = path_generator(striker_population, num_points = 10, extra_points = 2)
    victim_path = path_generator(victim_population, num_points = 10, extra_points = 2, is_striker = False)

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
                sensorsStriker = bng.poll_sensors(vehicleStriker)  # Polls the data of all sensors attached to the vehicle
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
                critical_damage_score = (0,0)
                distance_score = (0,0)
                rotation_score = (0,0)

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

    #     bng.kill_beamng()

    finally:
        break
    #     bng.close()
    

# ---------------------------- save genetic algorithm iteration-----------------------------------------
f = open("assets//genetic_algorithm_iteration.csv", "w+")
# -------------- save genetic algorithm iterator -------------------------------------
lines = ""
for k, v in populations_fitness.items():
    lines = lines + str(k) + ',' + str(v) + ','

lines = lines[:-1]
f.writelines(lines + '\n')
print(lines)
## -------------------------------- genetic algorithm helper --------------------------

#exit()

def tournament_parent_selection(populations, n=2, tsize=3):
    global populations_fitness
    print('tournament selection')
    selected_candidates = []
    for i in range(n):
        fittest_population_in_tournament = None
        candidates = random.sample(populations, tsize) #tsize = 20% of population.
        print(candidates)
        current = None
        for candidate in candidates:
            if fittest_population_in_tournament is None:
                fittest_population_in_tournament = populations_fitness[tuple(candidate)] # assign the fitness of current chromosome.
                current = candidate

            if populations_fitness[tuple(candidate)] > fittest_population_in_tournament:
                current = candidate

        selected_candidates.append(current)

    print(selected_candidates)
    return selected_candidates # it becomes the matinn pool
    #https://towardsdatascience.com/evolution-of-a-salesman-a-complete-genetic-algorithm-tutorial-for-python-6fe5d2b3ca35


def mutation(chromosome):
    print("mutation")
    chromosome = expand_chromosome(chromosome)
    chromosome[random.randint(0, len(chromosome) - 1)] = random.randint(min(chromosome), max(chromosome) - 1)
    random.shuffle(chromosome)
    return shrink_chromosome(chromosome)


def crossover(chromosome1, chromosome2):
    print("crossover")
    crossover_point = random.randint(1, len(expand_chromosome(chromosome1)) - 1)

    chromosome1 = expand_chromosome(chromosome1)
    chromosome2 = expand_chromosome(chromosome2)
    # Create children. np.hstack joins two arrays
    child = np.hstack((chromosome1[0:crossover_point],
                         chromosome2[crossover_point:]))
    return shrink_chromosome(child)



def crossover_mutation(selected_parents):
    print("crossover mutation")
    # https://stackoverflow.com/questions/20161980/difference-between-exploration-and-exploitation-in-genetic-algorithm?rq=1
    population_next = []
    n = 1
    for i in range(int(len(selected_parents) / 2)):
        for j in range(n):  # number of children
            chromosome1, chromosome2 = selected_parents[i], selected_parents[len(selected_parents) - 1 - i]
            childs = crossover(chromosome1, chromosome2)
            population_next.append(mutation(childs))

    print(population_next)
    return population_next

## -------------------------------- genetic algorithm helper --------------------------

# iteration of genetic algorithm.
for _ in range(20): # Number of Generations to be Iterated.
    print("genetic algorithm simulation")
    selected_parents = tournament_parent_selection(populations)
    next_population = crossover_mutation(selected_parents=selected_parents)
    print("population")
    print(populations)
    print("selected parents " + str(selected_parents) )
    print("next population " + str(next_population))

    for children in next_population:
        print("iteration of children")
        collision_points = []
        striker_points = []
        victim_points = []
        striker_speeds = []
        victim_speeds = []

        beamng_parameters = decoding_of_parameter(population)
        print(beamng_parameters)
        striker_speeds.append(beamng_parameters[0])
        striker_points.append(beamng_parameters[1])
        victim_speeds.append(beamng_parameters[2])
        victim_points.append(beamng_parameters[3])
        collision_points.append(beamng_parameters[4])

        striker_alpha = AngleBtw2Points(road_striker[0], road_striker[1])
        victim_alpha = AngleBtw2Points(road_victim[0], road_victim[1])

        striker_population = {
            'speed': beamng_parameters[0], # starting speed
            'col_speed': beamng_parameters[0], # collision speed
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
            'rot_z': 180,
        }

        victim_population = {
            'speed': 56, # starting speed
            'col_speed': beamng_parameters[2], # collision speed
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
            'rot_z': -90,
        }
        
        scenario.add_vehicle(
            vehicleStriker, 
            pos = (striker_population['pos_x'], striker_population['pos_y'], striker_population['pos_z']), 
            rot = (striker_population['rot_x'], striker_population['rot_y'], striker_population['rot_z'])
        )
        scenario.add_vehicle(
            vehicleVictim, 
            pos = (victim_population['pos_x'], victim_population['pos_y'], victim_population['pos_z']), 
            rot = (victim_population['rot_x'], victim_population['rot_y'], victim_population['rot_z'])
        )

        # save values to dictionary
        pos_crash_dict["chromosome"] = children
        pos_crash_dict["v1_speed"] = striker_speeds[0]
        pos_crash_dict["v1_waypoint"] = striker_points[0]
        pos_crash_dict["v2_speed"] = victim_speeds[0]
        pos_crash_dict["v2_waypoint"] = victim_points[0]

        scenario.make(beamng)

        striker_path = path_generator(striker_population, num_points = 10, extra_points = 3)
        victim_path = path_generator(victim_population, num_points = 10, extra_points = 3, is_striker = False)


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

        ## -------------------------------- genetic algorithm helper --------------------------

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
