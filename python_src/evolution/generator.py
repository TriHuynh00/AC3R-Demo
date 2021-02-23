import copy
import numpy


class Generator:
    @staticmethod
    def generate_random_from(individual, generate_params):
        # Create a new crash scenario
        random_ind = copy.deepcopy(individual)

        # Provide a new average speed to vehicles of the new crash scenario
        for i in range(len(random_ind.vehicles)):
            avg_speed = numpy.random.uniform(generate_params["min"], generate_params["max"], 1)[0]
            vehicle = random_ind.vehicles[i]
            for v in vehicle.driving_actions:
                v["speed"] = avg_speed

        return random_ind
