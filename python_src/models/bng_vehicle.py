import time


class BNGVehicle:
    def __init__(self, vehicle, pos, rot, rot_quat, road_pf):
        self.vehicle = vehicle
        self.pos = pos
        self.rot = rot
        self.rot_quat = rot_quat
        self.road_pf = road_pf
        self.positions = []
        self.damage = []
        self.times = []

    def collect_positions(self, position):
        self.positions.append(position)
        self.times.append(time.time())

    def collect_damage(self, damage):
        self.damage.append(damage)

    def get_damage(self):
        if len(self.damage) == 0:  # handle a crash scenario without damage
            return {}
        tmp_comp = self.damage[0]
        dam_comp = {}
        for k in tmp_comp:
            v = tmp_comp[k]
            dam_comp[v['name']] = v['damage']
        return dam_comp

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
