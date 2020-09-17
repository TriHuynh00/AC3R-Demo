from beamngpy import Vehicle

CART_PARTS_DICT = {'Tailgate': 'tailgate', 'Wagon Unibody': 'wagon_unibody', 'Rear Bumper': 'rear_bumper', 'Front Bumper Support': 'front_bumper_support', 'Front Bumper': 'front_bumper', 'Hood': 'hood', 'Right Headlight': 'right_headlight', 'Left Headlight': 'left_headlight', 'Front Right Fender': 'front_right_fender', 'Front Left Fender': 'front_left_fender', 'Single Exhaust': 'single_exhaust', 'Front Right Door': 'front_right_door', 'Front Left Door': 'front_left_door', 'Rear Right Door': 'rear_right_door', 'Rear Left Door': 'rear_left_door'}

class Car:
	def __init__(self, id, vehicle):
		self.name = "v" + str(id)
		self.vehicle = vehicle
		self.damage = {}
		self.description = ''

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