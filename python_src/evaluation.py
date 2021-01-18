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
            # Suggest vehicle to increase speed
            t1 = v1.times
            t2 = v2.times
            p1 = [Point(p[0], p[1]) for p in v1.positions]
            p2 = [Point(p[0], p[1]) for p in v2.positions]
            crash_point = Point(-2.0, 2.0)
            d1 = [p.distance(crash_point) for p in p1]
            idx1 = d1.index(min(d1))
            d2 = [p.distance(crash_point) for p in p2]
            idx2 = d2.index(min(d2))
            slow = 'v2_speed' if t1[idx1] < t2[idx2] else 'v1_speed'
            fast = 'v1_speed' if t1[idx1] < t2[idx2] else 'v2_speed'

            return distance, status, slow, fast

        return distance, status
