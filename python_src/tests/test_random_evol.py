import json
import unittest
import time
import numpy as np
from copy import deepcopy
from ac3r_plus import CrashScenario
from models import RoadProfiler, BNGVehicle
from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage
from simulation import Simulation
from test_data import collect_police_report
from evolution import RandomEvolution

FIRST = 0


def get_speed_v1(individual):
    speed = 0
    for i in individual.vehicles[0].driving_actions:
        speed = i["speed"]
    return speed


def change_speed_v1(individual, speed_val):
    for i in individual.vehicles[0].driving_actions:
        i["speed"] = speed_val


def fitness(deap_inds):
    # Collect simulation data from CrashScenario object
    def collect_sim_data(crash_scenario):
        # JSON READ: Building scenario's streets
        bng_roads = []
        for road in crash_scenario.roads:
            bng_road = Road('road_asphalt_2lane', rid=road.name)
            bng_road.nodes.extend(road.road_nodes)
            bng_roads.append(bng_road)

        bng_vehicles = []
        for vehicle in crash_scenario.vehicles:
            trajectory = vehicle.generate_trajectory()
            initial_position = (trajectory[0][0], trajectory[0][1], 0)
            v = Vehicle("scenario_player_" + str(vehicle.name),
                        model="etk800", licence=vehicle.name, color=vehicle.color)
            v.attach_sensor('damage', Damage())
            road_pf = RoadProfiler()
            road_pf.compute_ai_script(trajectory, vehicle.color)
            bng_vehicles.append(BNGVehicle(v, initial_position, None, vehicle.rot_quat, road_pf))

        return crash_scenario, bng_roads, bng_vehicles

    individual = deap_inds[FIRST]
    crash_scenario, bng_roads, bng_vehicles = collect_sim_data(individual)
    # Execute crash scenario and collect simulation's result
    simulation = Simulation(bng_roads, bng_vehicles)
    simulation.execute_scenario(time.time() + 60 * 1)
    crash_scenario.sim_report = simulation.get_report()

    # Fixed sample report data
    # TODO: change the sample police report to dynamic variable
    report_data = collect_police_report()
    crash_scenario.cal_fitness(report_data)  # Calculate fitness score
    return crash_scenario.score,


def generate_random_ind(orig_ind, generate_params):
    random_ind = deepcopy(orig_ind)
    change_speed_v1(random_ind, np.random.uniform(generate_params["min"], generate_params["max"], 1)[0])
    return random_ind


def select(orig_inds, pop_ind):
    deap_inds = pop_ind[FIRST]  # deap_pop is a list
    target_ind = deap_inds[FIRST]  # deap_individual is a list
    orig_ind = orig_inds[FIRST]  # deap_individual is a list

    f1 = orig_inds.fitness.values
    f2 = deap_inds.fitness.values

    if f1 >= f2:
        value = get_speed_v1(orig_ind)
        fitness_value = f1
    else:
        value = get_speed_v1(target_ind)
        fitness_value = f2

    deap_inds.fitness.values = fitness_value  # update fitness value to offspring
    change_speed_v1(target_ind, value)  # update attribute value to offspring
    return deap_inds  # return deap_individual


def mutate(deap_inds, mutate_params):
    individual = deap_inds[FIRST]  # deap_individual is a list

    # value = get_speed_v1(individual)  # extract attribute value from an individual
    # std, dim = mutate_params['std'], mutate_params['dim']
    value = np.random.uniform(mutate_params["min"], mutate_params["max"], mutate_params["dim"])[0]
    if value < mutate_params['min']:
        value = mutate_params['min']
    if value > mutate_params['max']:
        value = mutate_params['max']

    change_speed_v1(individual, value)
    return individual  # return deap_individual


def expectations(gens):
    # TODO: calculate maximum value for each scenario
    return [7 for _ in range(gens)]


class RandomEvolutionTest(unittest.TestCase):
    def test_run_scenario_in_population(self):
        self.assertEqual(0, 0)
        with open("./data/Case6.json") as file:
            scenario_data = json.load(file)
        orig_ind = CrashScenario.from_json(scenario_data)

        rev = RandomEvolution(
            orig_ind=orig_ind,
            fitness=fitness,
            generate_random_ind=generate_random_ind,
            select=select,
            generate_params={"min": 10, "max": 50},
            expectations=expectations
        )
        rev.start_from(timeout=180)

        rev.print_logbook()
        rev.visualize_evolution()
