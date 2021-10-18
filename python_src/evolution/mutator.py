import copy
from models.ac3rp import CrashScenario
from models.mutator import Transformer


class Mutator:
    @staticmethod
    def mutate_from(transformer: Transformer, deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual: CrashScenario = mutant[0]  # deap_individual is a list
        score = deap_inds.fitness.values[0]

        # Mutate the individual
        mutant[0] = transformer.mutate_from(individual)
        return mutant  # return deap_individual
