import time

CART_PARTS_DICT = {
    "Tailgate": 'R',
    "Wagon Unibody": 'M',
    "Rear Bumper": 'R',
    "Front Bumper Support": 'F',
    "Front Bumper": 'F',
    "Hood": 'F',
    "Right Headlight": 'FR',
    "Left Headlight": 'FL',
    "Front Right Fender": 'FR',
    "Front Left Fender": 'FL',
    "Single Exhaust": 'R',
    "Front Right Door": 'MR',
    "Front Left Door": 'ML',
    "Rear Right Door": 'MR',
    "Rear Left Door": 'ML',
    "Wagon Right Taillight": 'MR',
    "Wagon Left Taillight": 'ML'
}

THRESHOLD_DAMAGE = 0.05


def look_up_part_code(part_name):
    if part_name in CART_PARTS_DICT:
        return CART_PARTS_DICT[part_name]
    else:
        return 'UN'


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
                damage_position = look_up_part_code(v["name"])
                damage_value = v["damage"]
                self.damage.append({damage_position: damage_value})

    def get_damage(self):
        return self.damage

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
