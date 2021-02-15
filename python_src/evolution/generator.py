import copy
import numpy
import logging

logger = logging.getLogger(__name__)


class Generator:
    @staticmethod
    def generate_random_from(individual, generate_params):
        logger.debug("--- Creating a random scenario from %s ---", individual)
        random_ind = copy.deepcopy(individual)
        for i in individual.vehicles[0].driving_actions:
            i["speed"] = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
        for i in individual.vehicles[1].driving_actions:
            i["speed"] = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
        logger.info("--- Obtained %s ---", random_ind)
        return random_ind
