import numpy as np
import traceback

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
        self.expected_score = 0
        self.simulation_score = 0

    @staticmethod
    def formula(alpha: float, beta: float, tpl: Tuple[int, int, int] = (0, 0, 0)):
        return 1 + (alpha * tpl[MATCHING_CRASH_INDEX]) + (beta * tpl[MATCHING_NONCRASH_INDEX])

    @staticmethod
    def distance_between_two_players(players: List[models.Player]):
        distances = []
        for i in range(len(players[0].positions)):
            p0 = Point(players[0].positions[i][0], players[0].positions[i][1])
            p1 = Point(players[1].positions[i][0], players[1].positions[i][1])
            distances.append(p0.distance(p1))
        return -min(distances)

    def _compute(self, data_targets: {}, data_outputs: {},
                 debug: bool = False, debug_message: str = "Method Name"):
        if debug is True:
            print(f'Log {debug_message}: ')
            print("Targets: ", data_targets)
            print("Outputs", data_outputs)

        result = (0, 0, 0)
        for vehicle in data_targets:
            data_target = data_targets[vehicle]
            creator = models.categorize_report(data_target)
            data_output = data_outputs[vehicle]
            # Fix the issue when one of v1_damage or v2_damage is empty while other is not.
            # Without if, an exception will be triggered and the damage info will be lost, which
            # leads to a wrong simulation score.
            if len(data_output) == NO_CRASH:
                continue
            result = tuple(map(lambda x, y: x + y, result, creator.match(data_output, data_target)))
            if debug is True:
                print(f'{vehicle} target: {data_target}')
                print(f'{vehicle} output: {data_output}')
                print(f'{vehicle}: {creator.match(data_output, data_target)}')
        if debug is True:
            print(f'Total Sum: {result}')

        return self.formula(self.alpha, self.beta, result)

    def get_expected_score(self, debug: bool = False):
        # Calculate the max (expected) score of this scenario
        self.expected_score = self._compute(self.simulation.targets, self.simulation.targets,
                                            debug=debug, debug_message="SimulationScore.get_expected_score()")
        return self.expected_score

    def calculate(self, debug: bool = False):
        # If a crash doesn't occur, a score is distance between 2 vehicles' position
        if self.simulation.status == NO_CRASH:
            if debug is True:
                print("Log SimulationScore.calculate() - NO_CRASH: ")
                for player in self.simulation.players:
                    print(player.vehicle.vid)
                    print(player.positions)
            self.simulation_score = self.distance_between_two_players(self.simulation.players)
        else:
            try:
                self.simulation_score = self._compute(self.simulation.targets, self.simulation.get_data_outputs(),
                                                      debug=debug, debug_message="SimulationScore.calculate() - CRASH")
            except Exception as ex:
                traceback.print_exception(type(ex), ex, ex.__traceback__)
                self.simulation_score = self.distance_between_two_players(self.simulation.players)

        return self.simulation_score
