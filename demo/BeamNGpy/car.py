from beamngpy import Vehicle

CART_PARTS_DICT = {
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
    "Dual Outlet Exhaust": "B",
    "Wagon Left Taillight": "ML",
    "Radiator": 'F',
    "Independent Front Suspension": 'F',
    "Independent Rear Suspension": 'B',
    "Tailgate Glass": 'B'
}

class Car:
    def __init__(self, id, vehicle):
        self.name = "v" + str(id)
        self.vehicle = vehicle
        self.damage = {}
        self.description = ''
        self.position = []

    def set_position(self, vehicle):
        self.position.append([vehicle.state['pos'][0], vehicle.state['pos'][1]])

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
