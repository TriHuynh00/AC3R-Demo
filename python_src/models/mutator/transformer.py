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

    def mutate_random_from(self, scenario: CrashScenario) -> CrashScenario:
        """
        Implement method to generate a random scenario based on given crash scenario object.

        Args:
            scenario (CrashScenario): a crash scenario is provided by AC3RPlus.
        Return:
            CrashScenario: a mutated crash scenario object generated randomly.
        """

        # Initialize configuration
        mutated_scenario = copy.deepcopy(scenario)

        for vehicle in mutated_scenario.vehicles:
            for mutator in self.mutators:
                mutator.mutate(vehicle, is_random=True)

        return mutated_scenario

    def mutate_from(self, scenario: CrashScenario, is_unit_test: bool = False) -> CrashScenario:
        """
        Implement method to modify a given crash scenario object.

        Args:
            scenario (CrashScenario): a crash scenario is provided by AC3RPlus.
            is_unit_test (Boolean): a parameter used for testing only.

        Return:
            CrashScenario: a mutated crash scenario object.
        """

        # Initialize configuration
        is_triggered_mutator = False
        mutated_scenario = copy.deepcopy(scenario)

        # Mutators Order in List: [Speed v1, Point v1, Speed v2, Point v2]
        for vehicle in mutated_scenario.vehicles:
            for mutator in self.mutators:
                probability = random.uniform(0, 1)
                if is_unit_test:  # Executing for unit test only
                    mutator.mutate(vehicle)
                else:
                    if probability <= mutator.probability:
                        mutator.mutate(vehicle)
                        is_triggered_mutator = True if is_triggered_mutator is False else is_triggered_mutator

        # Define a function's response when one of mutators is triggered
        if is_triggered_mutator:
            print("=====")
            print("Due to a triggered mutator, we have a comparison between an old vehicle and mutated vehicle!")
            print("-----")
            for i in range(len(scenario.vehicles)):
                for scene in [scenario, mutated_scenario]:
                    vehicle = scene.vehicles[i]
                    print(f'Vehicle {vehicle.name}: Speed = {vehicle.get_speed()}; Point 0 = {vehicle.movement.get_driving_points()[0]}')
            print("=====")

        return mutated_scenario
