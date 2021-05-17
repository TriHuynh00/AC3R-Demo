import models
from typing import Tuple, List
from shapely.geometry import Point

MATCHING_CRASH_INDEX = 0
MATCHING_NONCRASH_INDEX = 1
NO_CRASH = 0


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

    @staticmethod
    def distance_between_two_players(players: List[models.Player]):
        p0 = Point(players[0].positions[-1][0], players[0].positions[-1][1])
        p1 = Point(players[1].positions[-1][0], players[1].positions[-1][1])
        return round(-p0.distance(p1), 2)

    def calculate(self, debug: bool = False):
        # If a crash doesn't occur, a score is distance between 2 vehicles' position
        if self.simulation.status is NO_CRASH:
            if debug is True:
                print("Log SimulationScore.calculate() - NO_CRASH: ")
                for player in self.simulation.players:
                    print(player.vehicle.vid)
                    print(player.positions)
            return self.distance_between_two_players(self.simulation.players)

        # Else
        data_targets = self.simulation.targets
        data_outputs = self.simulation.get_data_outputs()
        if debug is True:
            print("Log SimulationScore.calculate() - CRASH: ")
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
