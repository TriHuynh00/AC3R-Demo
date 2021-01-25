import json
import unittest
from fitness import Population, Individual
from simulation import Simulation
from test_data import sample_individuals, sample_scenarios, collect_scenario_data, collect_police_report

individuals = sample_individuals()
population = Population(individuals)
population.calculate_fitness()


class PopulationTest(unittest.TestCase):
    def test_get_fittest(self):
        self.assertEqual(population.get_fittest(), individuals[0], "The fittest is scenario 04")

    def test_get_second_fittest(self):
        self.assertEqual(population.get_second_fittest(), individuals[3], "The second fittest is scenario 03")

    def test_get_least_fittest(self):
        self.assertEqual(population.get_least_fittest(), individuals[1], "The least fittest is scenario 01")

    def test_run_scenario_in_popuplation(self):
        report_data = collect_police_report()
        individuals = []
        scenarios = sample_scenarios()
        for s in scenarios:
            bng_roads, bng_vehicles = collect_scenario_data(s)
            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            individuals.append(Individual(simulation.get_result(), report_data))

        population = Population(individuals)
        population.calculate_fitness()
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

    def test_run_scenario(self):
        report_data = collect_police_report()
        individuals = []
        scenarios = sample_scenarios()
        selected = scenarios[0]
        bng_roads, bng_vehicles = collect_scenario_data(selected)
        simulation = Simulation(bng_roads, bng_vehicles)
        simulation.execute_scenario()

        individuals.append(Individual(simulation.get_result(), report_data))
        population = Population(individuals)
        population.calculate_fitness()
        for p in population.individuals:
            print(p)
        self.assertEqual(0, 0)