import numpy
import copy


class Mutator:
    @staticmethod
    def mutate(mutate_params, deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual = mutant[0]  # deap_individual is a list

        def _mutate_val(value, params):
            value += numpy.random.normal(params["mean"], params["std"], 1)[0]
            if value < params['min']:
                value = params['min']
            if value > params['max']:
                value = params['max']
            return value

        for i in range(len(individual.vehicles)):
            avg_speed = _mutate_val(individual.vehicles[i].get_speed()[0], mutate_params)
            vehicle = individual.vehicles[i]
            for v in vehicle.driving_actions:
                v["speed"] = avg_speed

        return mutant  # return deap_individual
