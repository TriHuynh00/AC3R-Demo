import json
import unittest
from models import Population
from ac3r_plus import CrashScenario
from simulation import Simulation
from test_data import sample_scenarios, collect_scenario_data, collect_police_report
from search_algorithm import AC3RPlusSearch


class PopulationTest(unittest.TestCase):
    def test_run_scenario_in_population(self):
        report_data = collect_police_report()
        individuals = []
        scenarios = sample_scenarios()
        for s in scenarios:
            crash_scenario, bng_roads, bng_vehicles = collect_scenario_data(s)
            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            crash_scenario.sim_report = simulation.get_report()
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
        print("The worst scenario:")
        print(population.get_least_fittest())

        self.assertEqual(0, 0)

    def test_search_algorithm(self):
        ac3r_search = AC3RPlusSearch()
        with open("./data/Case6.json") as file:
            scenario_data = json.load(file)
        crash_scenario = CrashScenario.from_json(scenario_data)
        ac3r_search.start_from(crash_scenario)
        self.assertEqual(0, 0)
