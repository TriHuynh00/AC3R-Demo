import json
import unittest
from ac3r_plus import CrashScenario
from evolution import RandomEvolution, Selector, Generator, Fitness


def get_speed_v1(individual):
    speed = 0
    for i in individual.vehicles[0].driving_actions:
        speed = i["speed"]
    return speed


def get_speed_v2(individual):
    speed = 0
    for i in individual.vehicles[1].driving_actions:
        speed = i["speed"]
    return speed


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
            fitness=Fitness.evaluate,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            select=Selector.select_random_ev,
            expectations=expectations
        )
        rev.start_from(timeout=60*15)

        rev.print_logbook()
        rev.visualize_evolution()
