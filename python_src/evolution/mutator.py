import numpy
import copy
from models.ac3rp import CrashScenario


class Mutator:
    @staticmethod
    def by_speed(mutate_params, deap_inds):
        mutant = copy.deepcopy(deap_inds)
        individual: CrashScenario = mutant[0]  # deap_individual is a list

        def _mutate_val(value: float, params: dict):
            value += numpy.random.normal(params["mean"], params["std"], 1)[0]
            if value < params['min']:
                value = params['min']
            if value > params['max']:
                value = params['max']
            return value

        for vehicle in individual.vehicles:
            for action in vehicle.driving_actions:
                new_speed = _mutate_val(action["speed"], mutate_params)
                action["speed"] = new_speed

        return mutant  # return deap_individual
