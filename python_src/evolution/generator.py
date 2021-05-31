import copy
import numpy
from models.ac3rp import CrashScenario


class Generator:
    @staticmethod
    def generate_random_from(scenario: CrashScenario, generate_params: [float, float]):
        # Create a new crash scenario
        randomScenario = copy.deepcopy(scenario)

        # Assign a new average speed to all driving actions belong to random crash scenario
        random_speed = numpy.random.choice(list(range(generate_params["min"], generate_params["max"])))
        for vehicle in randomScenario.vehicles:
            for action in vehicle.driving_actions:
                action["speed"] = random_speed

        return randomScenario
