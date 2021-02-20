import json
import unittest
from ac3r_plus import CrashScenario
from evolution import RandomEvolution, Selector, Generator, Fitness, LogBook


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
            timeout=60 * 3
        )
        rev.run()

        v = LogBook(expectations)
        v.print_logbook(rev.logbook)
        v.visualize_ind(rev.logbook, "Random")


        # v.print_logbook(opo_ev.logbook)
        # v.visualize_ind(opo_ev.logbook, "OPO")
        #
        # v.visualize(rev.logbook, opo_ev.logbook, "Random", "OPO")


