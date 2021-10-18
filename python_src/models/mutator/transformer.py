import copy
import random
from typing import List
from models.mutator import MutatorCreator
from models.ac3rp import CrashScenario


class Transformer:
    """
    The Transformer class declares the mutate operation that modifies an scenario.

    Args:
        mutators (List[MutatorCreator]): a set of mutators to modify a crash scenario.
    """
    def __init__(self, mutators: List[MutatorCreator]):
        self.mutators = mutators

    def mutate_from(self, scenario: CrashScenario):
        """
        Implement method to modify a given crash scenario object.

        Args:
            scenario (CrashScenario): a crash scenario is provided by AC3RPlus.
        """

        # Initialize configuration
        is_triggered_mutator = False
        mutated_scenario = copy.deepcopy(scenario)

        # Mutators Order in List: [Speed v1, Point v1, Speed v2, Point v2]
        mutators_index = 0
        for vehicle in mutated_scenario.vehicles:
            # Mutate Speed
            probability = random.uniform(0, 1)
            if probability <= self.mutators[mutators_index].probability:
                self.mutators[mutators_index].mutate(vehicle)
                is_triggered_mutator = True
            mutators_index += 1

            # Mutate Initial Point
            probability = random.uniform(0, 1)
            if probability <= self.mutators[mutators_index].probability:
                self.mutators[mutators_index].mutate(vehicle)
                is_triggered_mutator = True
            mutators_index += 1

        # Define a function's response when one of mutators is triggered
        if is_triggered_mutator:
            print("=====")
            print("Due to a triggered mutator, we have a comparison between an old scenario and mutated scenario!")
            print("-----")
            for scene in [scenario, mutated_scenario]:
                for vehicle in scene.vehicles:
                    print(f'Vehicle {vehicle.name}: Speed = {vehicle.get_speed()}; Point 0 = {vehicle.movement.get_driving_points()[0]}')
            print("=====")

        return mutated_scenario
