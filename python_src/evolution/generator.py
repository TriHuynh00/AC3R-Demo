import copy
import numpy
import logging


class Generator:
    @staticmethod
    def generate_random_from(individual, generate_params):
        random_ind = copy.deepcopy(individual)
        for i in individual.vehicles[0].driving_actions:
            i["speed"] = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
        for i in individual.vehicles[1].driving_actions:
            i["speed"] = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
        return random_ind
