import time

THRESHOLD_DAMAGE = 0.05


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

    def collect_damage(self, bng_damage):
        # bng_damage format:
        # {'etk800_door_RL_wagon': {'name': 'Rear Left Door', 'damage': 0.009009009009009009}}
        for k in bng_damage:
            v = bng_damage[k]
            # Only collect component with damage bigger than threshold
            if v["damage"] > THRESHOLD_DAMAGE:
                self.damage.append(bng_damage)

    def get_damage(self):
        dam_comp = {}
        if len(self.damage) == 0:  # handle a crash scenario without damage
            return dam_comp
        tmp_comp = self.damage[0]
        for k in tmp_comp:
            v = tmp_comp[k]
            dam_comp[v['name']] = v['damage']
        return dam_comp

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
