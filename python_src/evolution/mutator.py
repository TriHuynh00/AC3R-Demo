import numpy
import copy


class Mutator:
    @staticmethod
    def mutate(mutate_params, deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual = mutant[0]  # deap_individual is a list

        def _mutate_val(value, params):
            value += numpy.random.normal(0, params['std'], 1)[0]
            if value < params['min']:
                value = params['min']
            if value > params['max']:
                value = params['max']
            return value

        value_v1 = _mutate_val(individual.vehicles[0].get_speed()[0], mutate_params)
        value_v2 = _mutate_val(individual.vehicles[1].get_speed()[0], mutate_params)

        for i in individual.vehicles[0].driving_actions:
            i["speed"] = value_v1
        for i in individual.vehicles[1].driving_actions:
            i["speed"] = value_v2
        return mutant  # return deap_individual
