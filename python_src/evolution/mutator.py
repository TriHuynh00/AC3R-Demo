import numpy
import copy


class Mutator:
    @staticmethod
    def mutate(mutate_params, deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual = mutant[0]  # deap_individual is a list

        value_v1 = individual.vehicles[0].get_speed()[0]  # extract attribute value from an individual
        value_v2 = individual.vehicles[1].get_speed()[0]  # extract attribute value from an individual

        value_v1 += numpy.random.normal(0, mutate_params['std'], 1)[0]
        value_v2 += numpy.random.normal(0, mutate_params['std'], 1)[0]

        for i in individual.vehicles[0].driving_actions:
            i["speed"] = value_v1
        for i in individual.vehicles[1].driving_actions:
            i["speed"] = value_v2
        return mutant  # return deap_individual
