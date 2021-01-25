from shapely.geometry import Point

CRASHED = 1
NO_CRASH = 0


def key_exist_in_list(k, lis):
    for d in lis:
        if d['name'] == k:
            return True
    return False


class Individual:
    def __init__(self, simulation, police_report=None):
        self.simulation = simulation
        self.fitness = 0
        self.police_report = police_report

    def cal_fitness(self):
        status = self.simulation["status"]
        if self.police_report is None or status == NO_CRASH:
            # For testing purpose
            if "distance" in self.simulation:
                self.fitness = -self.simulation["distance"]
            else:
                v1 = self.simulation["vehicles"][0]
                v2 = self.simulation["vehicles"][1]
                p1 = Point(v1.positions[-1][0], v1.positions[-1][1])
                p2 = Point(v2.positions[-1][0], v2.positions[-1][1])
                self.fitness = -p1.distance(p2)
        elif status == CRASHED:
            point = 1
            for v in self.simulation["vehicles"]:
                police_data = self.police_report[v.vehicle.vid] # Extract data from report to compare
                v_damage = v.get_damage()
                for k in v_damage:
                    if key_exist_in_list(k, police_data): # Damage component exists in police report
                        point += 1
            self.fitness = point
        # Remove unnecessary attribute
        self._cleanup()
        return self.fitness

    # Do cleanup for an attribute
    def _cleanup(self):
        delattr(self, 'simulation')
        delattr(self, 'police_report')

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
