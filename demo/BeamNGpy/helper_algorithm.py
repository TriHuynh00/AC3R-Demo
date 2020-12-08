import math
import random
import numpy as np
from helper_crash import getV1BeamNGCoordinaes, getV2BeamNGCoordinaes


def generateRandomPopulation(N=5, Gene=10):
    print("random population")
    random_population = [[5, 6, 7, 2, 5, 5, 8, 8, 7, 5, 4, 2, 4, 7]]
    seed_population = [[np.random.randint(1, 9) for i in range(Gene + 4)] for j in range(N - 1)]
    for i, val in enumerate(seed_population):
        random_population.append(val)

    print(random_population)
    initial_population = convertPopulation(random_population)
    return initial_population


def convertPopulation(random_population):
    print("convert population")
    converted_population = []
    for i, val in enumerate(random_population):
        merge_list = [int(str(val[0]) + str(val[1])), val[2], val[3], val[4], int(str(val[5]) + str(val[6])), val[7],
                      val[8], val[9], val[10], int(str(val[11]) + str(val[12]) + str(val[13]))]
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
def decoding_of_parameter(chromosome, impact_x, impact_y, road_striker, road_victim):
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

    point_of_impact_x = impact_x + radius * math.cos(math.radians(angle))  # radians
    point_of_impact_y = impact_y + radius * math.sin(math.radians(angle))  # radians
    impact_point = (point_of_impact_x, point_of_impact_y)

    # position length
    total_distance_v1 = float(int(str(chromosome[V1_DISTANCE_INDEX_1]) + str(chromosome[V1_DISTANCE_INDEX_2])) / 50)
    v1_pos_bg = getV1BeamNGCoordinaes(total_distance_v1, chromosome[V1_WIDTH_INDEX] % 4, road_striker)
    # get beamng coordinates (polyline coordinate). it will be always calculated from center - joint

    total_distance_v2 = float(int(str(chromosome[V2_DISTANCE_INDEX_1]) + str(chromosome[V2_DISTANCE_INDEX_2])) / 50)
    v2_pos_bg = getV2BeamNGCoordinaes(total_distance_v2, chromosome[V2_WIDTH_INDEX] % 4, road_victim)
    # get beamng coordinates (polyline coordinate). it will be always calculated from center - joint

    return v1_speed, v1_pos_bg, v2_speed, v2_pos_bg, impact_point


def tournament_parent_selection(populations, populations_fitness, n=2, tsize=3):
    print('tournament selection')
    selected_candidates = []
    for i in range(n):
        fittest_population_in_tournament = None
        candidates = random.sample(populations, tsize)  # tsize = 20% of population.
        print(candidates)
        current = None
        for candidate in candidates:
            if fittest_population_in_tournament is None:
                fittest_population_in_tournament = populations_fitness[
                    tuple(candidate)]  # assign the fitness of current chromosome.
                current = candidate

            if populations_fitness[tuple(candidate)] > fittest_population_in_tournament:
                current = candidate

        selected_candidates.append(current)

    print(selected_candidates)
    return selected_candidates  # it becomes the matinn pool
    # https://towardsdatascience.com/evolution-of-a-salesman-a-complete-genetic-algorithm-tutorial-for-python-6fe5d2b3ca35


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