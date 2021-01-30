import unittest
from models import Population
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