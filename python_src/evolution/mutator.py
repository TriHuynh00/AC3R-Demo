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
            mutated_speed = _mutate_val(vehicle.get_speed(), mutate_params)  # 1 speed / 1 vehicle for all actions
            for action in vehicle.driving_actions:
                action["speed"] = mutated_speed

        return mutant  # return deap_individual
