from __future__ import annotations
from typing import Tuple
from abc import ABC, abstractmethod
import numpy
from models.ac3rp import Vehicle

CAT_A = 'A'  # Mutator mutates a speed
CAT_B = 'B'  # Mutator mutates an initial point


class MutatorCreator(ABC):
    """
    The MutatorCreator class declares the factory method that returns a new mutator object.
    The return type of this method must match the Report interface.
    """

    def __init__(self, probability: float, params: dict):
        self.probability = probability
        self.params = params

    @abstractmethod
    def create(self) -> Mutator:
        """
        Return the mutator with a probability and its appropriate type
        """
        pass

    def mutate(self, vehicle: Vehicle) -> Vehicle:
        """
        Return the mutated vehicle after it is mutated
        by a speed or an initial point mutator
        """
        mutator = self.create()
        return mutator.process(vehicle)


class Mutator(ABC):
    """
    The Mutator interface declares the operations that all concrete Mutators
    must implement.
    """

    def __init__(self, params):
        self.params = params

    def mutate_value(self, value: float):
        value += numpy.random.normal(self.params["mean"], self.params["std"], 1)[0]
        if value < self.params["min"]:
            value = self.params["min"]
        if value > self.params["max"]:
            value = self.params["max"]
        return value

    @abstractmethod
    def process(self, vehicle: Vehicle) -> Vehicle:
        """
        Return the vehicle with a new speed
               the vehicle with a trajectory based on a new starting point
        """
        pass


"""
Concrete Creators override the factory method in order to 
generate a different type of mutator.
"""


class SpeedCreator(MutatorCreator):
    """
    SpeedMutator override the factory method in order to
    mutate vehicle speed.
    """

    def create(self) -> Mutator:
        from models.mutator import MutatorTypeA
        return MutatorTypeA(self.params)


class InitialPointCreator(MutatorCreator):
    """
    InitialPointCreator override the factory method in order to
    mutate vehicle's initial point and its trajectory.
    """

    def create(self) -> Mutator:
        from models.mutator import MutatorTypeB
        return MutatorTypeB(self.params)


def categorize_mutator(mutator_data: dict) -> MutatorCreator:
    """
    Categorizes the type of mutator
    """
    mt_type = mutator_data["type"]
    probability = mutator_data["probability"]
    params = mutator_data["params"]

    if mt_type is CAT_A:
        return SpeedCreator(probability, params)
    if mt_type is CAT_B:
        return InitialPointCreator(probability, params)

    raise Exception("Exception: Mutator Type not found!")
