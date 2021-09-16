import beamngpy
import time
from models.road_profiler import RoadProfiler
from .vehicle_parts_dict import VEHICLE_PARTS_DICT


class Player:
    """
    The Player class declares the interface that provides necessary information to
    add Vehicle to Scenario at the given position with the given orientation
    and add a visual line to be rendered by BeamNG, but can also be used to drive Vehicle
    using ai_set_script() method.

    Args:
        pos (tuple): (x,y,z) tuple specifying the position of the vehicle.
        rot (tuple): (x,y,z) tuple expressing the rotation of the vehicle in Euler angles around each axis.
        rot_quat (tuple): (x, y, z, w) tuple specifying the rotation as quaternion.
        road_pf (RoadProfiler): list of timestamped positions defining to make the vehicle follow a given "script".
        distance_to_trigger (float): a floating number specifies the distance between 2 vehicles. When the distance
                                     between 2 vehicles <= this number, an event in the simulation will trigger
                                     this vehicle starting to move. By default, the value is -1 which means the vehicle
                                     will run at the beginning of simulation.
    """
    def __init__(self, vehicle: beamngpy.Vehicle, road_pf: RoadProfiler, pos, rot, rot_quat, distance_to_trigger: float,
                 speed: float):
        self.vehicle = vehicle
        self.pos = pos
        self.rot = rot
        self.rot_quat = rot_quat
        self.road_pf = road_pf
        self.distance_to_trigger = distance_to_trigger
        self.positions = []
        self.damage = []
        self.times = []
        self.speed = speed

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
