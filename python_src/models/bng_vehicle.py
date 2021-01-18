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

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
