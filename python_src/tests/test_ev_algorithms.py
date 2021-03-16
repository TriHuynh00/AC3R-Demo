import json
import unittest
import numpy
from ac3r_plus import CrashScenario
from evolution import RandomEvolution, OpoEvolution, Selector, Mutator, Generator, Fitness, LogBook


def expectations(gens):
    # TODO: calculate maximum value for each scenario
    return [7 for _ in range(gens)]


with open("./data/Case6.json") as file:
    scenario_data = json.load(file)
ORIG_IND = CrashScenario.from_json(scenario_data)
VIS = LogBook(expectations)


class RandomEvolutionTest(unittest.TestCase):
    def test_experiments(self):
        self.assertEqual(0, 0)
        timeout = 60 * 5
        numpy.random.seed(64)

        #########################################################
        # First Experiment
        # Run simulations one time only
        #########################################################
        rev = RandomEvolution(
            orig_ind=ORIG_IND,
            fitness=Fitness.evaluate,
            fitness_repetitions=1,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            select=Selector.select_by_aggregate,
            timeout=timeout
        )
        oev = OpoEvolution(
            orig_ind=ORIG_IND,
            fitness=Fitness.evaluate,
            fitness_repetitions=1,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            mutate=Mutator.mutate,
            mutate_params={"mean": 0, "std": 8, "min": 10, "max": 50},
            select=Selector.select_by_aggregate,
            timeout=timeout
        )
        rev.run()
        oev.run()
        VIS.visualize(rev.logbook, oev.logbook, "Random", "OPO")

        #########################################################
        # Second Experiment
        # Run simulations a fixed amount of time (e.g.,10)
        # and take average/mean
        #########################################################
        rev = RandomEvolution(
            orig_ind=ORIG_IND,
            fitness=Fitness.evaluate,
            fitness_repetitions=10,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            select=Selector.select_by_aggregate,
            select_strategy=numpy.mean,
            timeout=timeout
        )
        oev = OpoEvolution(
            orig_ind=ORIG_IND,
            fitness=Fitness.evaluate,
            fitness_repetitions=10,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            mutate=Mutator.mutate,
            mutate_params={"mean": 0, "std": 8, "min": 10, "max": 50},
            select=Selector.select_by_aggregate,
            select_strategy=numpy.mean,
            timeout=timeout
        )
        rev.run()
        oev.run()
        VIS.visualize(rev.logbook, oev.logbook, "Random", "OPO")
