import copy
import numpy


class Generator:
    @staticmethod
    def generate_random_from(individual, generate_params):
        random_ind = copy.deepcopy(individual)
        avg_speed_v1 = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
        avg_speed_v2 = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
        for i in individual.vehicles[0].driving_actions:
            i["speed"] = avg_speed_v1
        for i in individual.vehicles[1].driving_actions:
            i["speed"] = avg_speed_v2
        return random_ind
