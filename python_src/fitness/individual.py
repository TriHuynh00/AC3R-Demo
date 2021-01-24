from shapely.geometry import Point

CRASHED = 1
NO_CRASH = 0


class Individual:
    def __init__(self, simulation):
        self.simulation = simulation
        self.fitness = 0

    def cal_fitness(self):
        status = self.simulation["status"]
        if status == CRASHED:
            self.fitness = 1
        elif status == NO_CRASH:
            # For testing purpose
            if "distance" in self.simulation:
                self.fitness = -self.simulation["distance"]
            else:
                v1 = self.simulation["vehicles"][0]
                v2 = self.simulation["vehicles"][1]
                p1 = Point(v1.positions[-1][0], v1.positions[-1][1])
                p2 = Point(v2.positions[-1][0], v2.positions[-1][1])
                self.fitness = -p1.distance(p2)
        return self.fitness

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
