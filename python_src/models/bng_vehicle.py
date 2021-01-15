class BNGVehicle:
    def __init__(self, vehicle, pos, rot, rot_quat, road_pf):
        self.vehicle = vehicle
        self.pos = pos
        self.rot = rot
        self.rot_quat = rot_quat
        self.road_pf = road_pf

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)