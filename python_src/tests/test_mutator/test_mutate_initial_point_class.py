import json
import unittest

import models.mutator.mutator
from models import categorize_mutator, CONST
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


class TestMutateInitialPointClass(unittest.TestCase):
    def test_probability_equal_10(self):
        mutator = categorize_mutator({
            "type": CONST.MUTATE_INITIAL_POINT_CLASS,
            "probability": 10,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        })
        expected = 10
        self.assertEqual(expected, mutator.probability)

    def test_mutator_is_speed_mutator(self):
        mutator = categorize_mutator({
            "type": CONST.MUTATE_INITIAL_POINT_CLASS,
            "probability": 5,
            "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
        })
        self.assertEqual(models.mutator.mutator.InitialPointCreator, type(mutator))

    def test_handle_exception_when_no_mutator_type_found(self):
        expected = Exception
        with self.assertRaises(expected):
            mutator = categorize_mutator({
                "type": 'X',
                "probability": 5,
                "params": {"mean": 0, "std": 15, "min": 10, "max": 50}
            })

    def test_vehicle_has_new_initial_point(self):
        vehicle = get_test_vehicle()
        old_intial_point = vehicle.movement.get_driving_points()[0]

        mutator = categorize_mutator({
            "type": CONST.MUTATE_INITIAL_POINT_CLASS,
            "probability": 0.5,
            "params": {"mean": 0, "std": 1, "min": -10, "max": 10}
        })
        new_initial_point = mutator.mutate(vehicle).movement.get_driving_points()[0]
        self.assertNotEqual(old_intial_point, new_initial_point)
