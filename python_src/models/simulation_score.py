import models
from typing import Tuple

MATCHING_CRASH_INDEX = 0
MATCHING_NONCRASH_INDEX = 1


class SimulationScore:
    """
    The SimulationScore class calculates the desired score to each simulation scenario. The score can be used
    in the Fitness function to measure the effectiveness of the algorithm

    Args:
        alpha (float): weight for number of matching crashed parts
        beta (float): weight for number of matching non-crashed parts
        simulation (models.Simulation): a running simulation of specific scenario
    """
    def __init__(self, simulation: models.Simulation, alpha: float = 0.2, beta: float = 0.1):
        self.alpha = alpha
        self.beta = beta
        self.simulation = simulation

    @staticmethod
    def formula(alpha: float, beta: float, tpl: Tuple[int, int, int] = (0, 0, 0)):
        return 1 + (alpha * tpl[MATCHING_CRASH_INDEX]) + (beta * tpl[MATCHING_NONCRASH_INDEX])

    def calculate(self, debug: bool = False):
        data_targets = self.simulation.targets
        data_outputs = self.simulation.get_data_outputs()
        if debug is True:
            print("Log SimulationScore.calculate(): ")
            print(data_targets)
            print(data_outputs)

        result = (0, 0, 0)
        for vehicle in data_targets:
            data_target = data_targets[vehicle]
            creator = models.categorize_report(data_target)
            data_output = data_outputs[vehicle]
            result = tuple(map(lambda x, y: x + y, result, creator.match(data_output, data_target)))
        if debug is True:
            print(result)

        return self.formula(self.alpha, self.beta, result)
