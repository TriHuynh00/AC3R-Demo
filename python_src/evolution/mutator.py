import numpy
import copy
from numpy.random import default_rng
from models.ac3rp import CrashScenario
from shapely.geometry import LineString, Point
from models.ac3rp import common, Vehicle
# from stats.thinkstats2 import Pmf


class Mutator:
    @staticmethod
    def mutate_from(mutators, deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual: CrashScenario = mutant[0]  # deap_individual is a list
        score = deap_inds.fitness.values[0]

        return mutant  # return deap_individual
