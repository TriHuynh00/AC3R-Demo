import copy
import numpy
from models.ac3rp import CrashScenario


class Generator:
    @staticmethod
    def generate_random_from(scenario: CrashScenario, generate_params: [float, float]):
        # Create a new crash scenario
        randomScenario = copy.deepcopy(scenario)

        # Assign a new average speed to all driving actions belong to random crash scenario
        for vehicle in randomScenario.vehicles:
            random_speed = numpy.random.choice(list(range(generate_params["min"], generate_params["max"])))
            vehicle.movement.set_speed(random_speed)

        return randomScenario
