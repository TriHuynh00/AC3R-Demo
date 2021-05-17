MATCHING_CRASH_INDEX = 0
MATCHING_NONCRASH_INDEX = 1


class SimulationScore:
    """
    The SimulationScore class calculates the desired score to each simulation scenario. The score can be used
    in the Fitness function to measure the effectiveness of the algorithm

    Args:
        alpha (float): weight for number of matching crashed parts
        beta (float): weight for number of matching non-crashed parts
        simulation_tuple (tuple): (match crash, match non-crash, maximum conditions)
    """
    def __init__(self, alpha: float = 0.2, beta: float = 0.1, simulation_tuple: tuple[int, int, int] = (0, 0, 0)):
        self.alpha = alpha
        self.beta = beta
        self.simulation_tuple = simulation_tuple

    def calculate(self):
        return 1 + (self.alpha * self.simulation_tuple[MATCHING_CRASH_INDEX]) + (self.beta * self.simulation_tuple[MATCHING_NONCRASH_INDEX])
