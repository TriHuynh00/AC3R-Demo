import json
import unittest

import models.mutator.mutator
from models import categorize_mutator
from models import CONST
from models.ac3rp import Road, Vehicle


def get_test_vehicle():
    test_data = {
        "roads": [{"name": "road1", "road_type": "roadway", "road_shape": "I",
                   "road_node_list": [[137.65834472425104, 196.59649062935804, 0, 12.0],
                                      [-137.65834472425104, -196.59649062935804, 0, 12.0]]}], "vehicles": [
            {"name": "v2", "color": "1 0 0", "rot_quat": [-0.00593952, -0.02359371, 0.42191736, 0.90630778],
             "distance_to_trigger": -1.0, "damage_components": "front", "driving_actions": [
                {"name": "follow", "trajectory": [[[55.72493, 77.1553, 0.0], [9.838813, 11.623133, 0.0]]],
                 "speed": 20},
                {"name": "follow", "trajectory": [[[9.838813, 11.623133, 0.0], [-2.0, 2.0, 0.0]]], "speed": 20},
                {"name": "follow", "trajectory": [[[-2.0, 2.0, 0.0], [-13.022756, -10.927389, 0.0]]],
                 "speed": 20}]}]
    }
    roads = []
    for road_dict in test_data["roads"]:
        roads.append(Road.from_dict(road_dict))

    vehicles = []
    for vehicle_dict in test_data["vehicles"]:
        vehicles.append(Vehicle.from_dict(vehicle_dict, roads))
    return vehicles[0]


class TestMutateSpeedClass(unittest.TestCase):
    def test_probability_equal_5(self):
        mutator = categorize_mutator({
            "type": CONST.MUTATE_SPEED_CLASS,
            "probability": 5,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        })
        expected = 5
        self.assertEqual(expected, mutator.probability)

    def test_mutator_is_speed_mutator(self):
        mutator = categorize_mutator({
            "type": CONST.MUTATE_SPEED_CLASS,
            "probability": 5,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        })
        self.assertEqual(models.mutator.mutator.SpeedCreator, type(mutator))

    def test_vehicle_has_new_speed(self):
        vehicle = get_test_vehicle()
        old_speed = vehicle.get_speed()

        mutator = categorize_mutator({
            "type": CONST.MUTATE_SPEED_CLASS,
            "probability": 0.5,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        })
        self.assertNotEqual(old_speed, mutator.mutate(vehicle).get_speed())
