from beamngpy import Vehicle
from beamngpy.sensors import Damage

CART_PARTS_DICT = {
    'Tailgate': 'R',
    'Wagon Unibody': 'M',
    'Rear Bumper': 'R',
    'Front Bumper Support': 'F',
    'Front Bumper': 'F',
    'Hood': 'F',
    'Right Headlight': 'FR',
    'Left Headlight': 'FL',
    'Front Right Fender': 'FR',
    'Front Left Fender': 'FL',
    'Single Exhaust': 'R',
    'Front Right Door': 'MR',
    'Front Left Door': 'ML',
    'Rear Right Door': 'MR',
    'Rear Left Door': 'ML',
    'Wagon Right Taillight': 'MR'
}


class Car:
    def __init__(self, id, vehicle=None, **args):
        self.name = "v" + str(id)
        self.id = id
        self.vehicle = vehicle
        self.damage = args.get('damage') if args.get('damage') else {}
        self.description = ''
        self.velocities = args.get('velocities') if args.get('velocities') else []
        self.rot_degree = args.get('rot_degree') if args.get('rot_degree') else []
        self.travelling_dir = args.get('travelling_dir') if args.get('travelling_dir') else []
        self.points = args.get('points') if args.get('points') else []
        self.color = args.get('color') if args.get('color') else ""

    def set_damage_sensor(self):
        damage_sensor = Damage()
        self.vehicle.attach_sensor('damage_' + self.name, damage_sensor)

    def set_vehicle(self, vehicle):
        self.vehicle = vehicle

    def set_damage(self, damage):
        self.damage = damage

    def set_description(self):
        # Sample damage data from Damage Sensor
        # test_dict = {
        # 	'etk800_body_wagon': {
        # 		'name': 'Wagon Unibody', 'damage': 0.0004748338081671415
        # 	},
        # 	'etk800_bumper_F': {
        # 		'name': 'Front Bumper', 'damage': 0.15593220338983052
        # 	}
        # }
        tmp_description = self.name
        for key, val in self.damage.items():
            position = CART_PARTS_DICT[self.damage[key]['name']]
            value = self.damage[key]['damage']
            tmp_description += ":" + position + "-" + str(value)
        self.description = tmp_description

    def get_rot_quat(self):
        return self.rot_degree[0]

    def get_rot_degree(self):
        return self.rot_degree[1]

    def get_velocities(self):
        return self.velocities

    def get_points(self):
        return self.points

    def get_direction(self):
        return self.travelling_dir

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
