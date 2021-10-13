import copy
import random
from models.ac3rp import CrashScenario
from typing import List
from models.mutator import MutatorCreator


class Mutator:
    @staticmethod
    def mutate_from(mutators: List[MutatorCreator], deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual: CrashScenario = mutant[0]  # deap_individual is a list
        score = deap_inds.fitness.values[0]

        # print("\n")
        # for vehicle in individual.vehicles:
        #     print("Speed: ", vehicle.get_speed())
        #     print("Point 0: ", vehicle.movement.get_driving_points()[0])
        # print("=====")

        # Mutate an individual
        # Mutators Order in List: [Speed v1, Point v1, Speed v2, Point v2]
        mutators_index = 0
        for vehicle in individual.vehicles:
            # Mutate Speed
            probability = random.uniform(0, 1)
            if probability <= mutators[mutators_index].probability:
                mutators[mutators_index].mutate(vehicle)
            mutators_index += 1

            # Mutate Initial Point
            probability = random.uniform(0, 1)
            if probability <= mutators[mutators_index].probability:
                mutators[mutators_index].mutate(vehicle)
            mutators_index += 1

        # for vehicle in individual.vehicles:
        #     print("Speed: ", vehicle.get_speed())
        #     print("Point 0: ", vehicle.movement.get_driving_points()[0])
        # print("\n")

        return mutant  # return deap_individual
