import json
import unittest
from models.ac3rp import CrashScenario
from models.mutator import MUTATE_INITIAL_POINT_CLASS, MUTATE_SPEED_CLASS, categorize_mutator, Transformer


class TestTransformerClass(unittest.TestCase):
    def test_mutate_from_crash_scenario_with_different_speeds(self):
        with open("../data/Case6_data.json") as file:
            scenario_data = json.load(file)
        scenario = CrashScenario.from_json(scenario_data)

        # Generate mutators
        mutators_data = [
            {
                "type": MUTATE_SPEED_CLASS,
                "probability": 0.5,
                "params": {"mean": 0, "std": 10, "min": 10, "max": 50}
            },
            {
                "type": MUTATE_INITIAL_POINT_CLASS,
                "probability": 0.5,
                "params": {"mean": 0, "std": 1, "min": -10, "max": 10}
            }
        ]

        transformer = Transformer([categorize_mutator(m) for m in mutators_data])
        mutated_scenario = transformer.mutate_from(scenario, is_test_mode=True)

        self.assertNotEqual(scenario.vehicles[0].get_speed(), mutated_scenario.vehicles[0].get_speed())
        self.assertNotEqual(scenario.vehicles[1].get_speed(), mutated_scenario.vehicles[1].get_speed())

    def test_mutate_from_crash_scenario_with_different_intial_points(self):
        with open("../data/Case6_data.json") as file:
            scenario_data = json.load(file)
        scenario = CrashScenario.from_json(scenario_data)

        # Generate mutators
        mutators_data = [
            {
                "type": MUTATE_SPEED_CLASS,
                "probability": 0.5,
                "params": {"mean": 0, "std": 10, "min": 10, "max": 50}
            },
            {
                "type": MUTATE_INITIAL_POINT_CLASS,
                "probability": 0.5,
                "params": {"mean": 0, "std": 1, "min": -10, "max": 10}
            }
        ]

        transformer = Transformer([categorize_mutator(m) for m in mutators_data])
        mutated_scenario = transformer.mutate_from(scenario, is_test_mode=True)

        self.assertNotEqual(scenario.vehicles[0].movement.get_driving_points()[0],
                            mutated_scenario.vehicles[0].movement.get_driving_points()[0])
        self.assertNotEqual(scenario.vehicles[1].movement.get_driving_points()[0],
                            mutated_scenario.vehicles[1].movement.get_driving_points()[0])
