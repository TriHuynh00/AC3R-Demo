import beamngpy
import time
from models.road_profiler import RoadProfiler

VEHICLE_PARTS_DICT = {
    'Tailgate': 'B',
    'Wagon Unibody': 'M',
    'Rear Bumper': 'B',
    'Front Bumper Support': 'F',
    'Front Bumper': 'F',
    'Hood': 'F',
    'Right Headlight': 'FR',
    'Left Headlight': 'FL',
    'Front Right Fender': 'FR',
    'Front Left Fender': 'FL',
    'Single Exhaust': 'B',
    'Front Right Door': 'MR',
    'Front Left Door': 'ML',
    'Rear Right Door': 'MR',
    'Rear Left Door': 'ML',
    'Wagon Right Taillight': 'MR',
    'Right Mirror': 'FR',
    'Left Mirror': 'FL',
}


class Player:
    def __init__(self, vehicle: beamngpy.Vehicle, road_pf: RoadProfiler, pos, rot, rot_quat):
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

    def get_damage(self, debug: bool = False) -> list:
        if len(self.damage) == 0:  # handle a crash scenario without damage
            return []
        if debug is True:
            print(self.vehicle.vid)
        tmp_comp = self.damage[0]
        components = []
        for k in tmp_comp:
            v = tmp_comp[k]
            part_name = v['name']
            part_damage = v['damage']
            if debug is True:
                print(f'{part_name}: {part_damage}')
            try:
                components.append({"name": VEHICLE_PARTS_DICT[part_name], "damage": part_damage})
            except KeyError:
                print(f'Warning: A part {part_name} is NOT FOUND in a dictionary!')
                components.append({"name": part_name, "damage": part_damage})
        return components

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
