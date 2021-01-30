import json
import random
import unittest
from deap import tools, creator, base
from copy import deepcopy
from models import Population
from ac3r_plus import CrashScenario
from simulation import Simulation
from test_data import sample_scenarios, collect_scenario_data, collect_police_report


class PopulationTest(unittest.TestCase):
    def test_run_scenario_in_popuplation(self):
        report_data = collect_police_report()
        individuals = []
        scenarios = sample_scenarios()
        for s in scenarios:
            crash_scenario, bng_roads, bng_vehicles = collect_scenario_data(s)
            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            crash_scenario.simulation = simulation.get_result()
            individuals.append(crash_scenario)

        population = Population(individuals)
        population.calculate_fitness(report_data)
        for p in population.individuals:
            print(p)

        print('====')
        print("The best scenario:")
        print(population.get_fittest())
        print("The second scenario:")
        print(population.get_second_fittest())
        print("The worst scenrario:")
        print(population.get_least_fittest())

        self.assertEqual(0, 0)

    def test_deap(self):
        def _mutate_scenario(scenario, min, max):
            mutated_scenario = deepcopy(scenario)
            for v in mutated_scenario.vehicles:
                idx = 0
                while idx < len(v.driving_actions):
                    v.driving_actions[idx]["speed"] = random.randint(min, max)
                    idx += 1
            return mutated_scenario

        def _eval_one_max(individual):
            report_data = collect_police_report()
            crash_scenario, bng_roads, bng_vehicles = collect_scenario_data(individual)
            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            crash_scenario.simulation = simulation.get_result()
            return crash_scenario.cal_fitness(report_data),

        with open("./data/Case6_mod.json") as file:
            scenario_data = json.load(file)
        crash_scenario = CrashScenario.from_json(scenario_data)

        creator.create("FitnessMax", base.Fitness, weights=(1.0,))
        creator.create("Individual", list, fitness=creator.FitnessMax)

        toolbox = base.Toolbox()
        # Attr generator
        toolbox.register("attr_bool", _mutate_scenario, crash_scenario, 10, 90)
        toolbox.register("individual", tools.initRepeat, creator.Individual, toolbox.attr_bool, 1)
        toolbox.register("population", tools.initRepeat, list, toolbox.individual)

        toolbox.register("evaluate", _eval_one_max)
        toolbox.register("mate", tools.cxTwoPoint)
        toolbox.register("mutate", tools.mutFlipBit, indpb=0.05)
        toolbox.register("select", tools.selTournament, tournsize=3)

        pop = toolbox.population(n=4)

        for ind in pop:
            print(ind[0].vehicles[0])

        self.assertEqual(0, 0)