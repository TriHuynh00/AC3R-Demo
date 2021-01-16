from shapely.geometry import Point

CRASHED = 1
NO_CRASH = 0


class Evaluation:
    def __init__(self, simulation_result):
        self.simulation_result = simulation_result

    def evaluate_simulation(self):
        distance = 0
        v1 = self.simulation_result["vehicles"][0]
        v2 = self.simulation_result["vehicles"][1]
        status = self.simulation_result["status"]

        if status == NO_CRASH:
            p1 = Point(v1.positions[-1][0], v1.positions[-1][1])
            p2 = Point(v2.positions[-1][0], v2.positions[-1][1])
            distance = p1.distance(p2)

        return distance, status
