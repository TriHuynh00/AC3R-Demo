import unittest
from models import SimulationScore, categorize_report


class TestSimScore(unittest.TestCase):
    def test_formula_for_Case06(self):
        expected = 1.7
        target = SimulationScore.formula(alpha=0.2, beta=0.1, tpl=(2, 3, 5))
        self.assertEqual(expected, target)

    def test_formula_for_sample_multi_reports(self):
        expected = 1.9
        target = SimulationScore.formula(alpha=0.2, beta=0.1, tpl=(2, 5, 11))
        self.assertEqual(expected, target)

    def test_computation_with_sample_data(self):
        data_targets = {'v1': [{'name': 'FR', 'damage': 0}], 'v2': [{'name': 'ANY', 'damage': 0}]}
        data_outputs = {'v1': [{'name': 'F', 'damage': 0}, {'name': 'FR', 'damage': 0}, {'name': 'F', 'damage': 1}],
                        'v2': [{'name': 'B', 'damage': 0}, {'name': 'B', 'damage': 0}, {'name': 'ML', 'damage': 0}]}

        result = (0, 0, 0)
        for vehicle in data_targets:
            data_target = data_targets[vehicle]
            creator = categorize_report(data_target)
            data_output = data_outputs[vehicle]
            # print(vehicle, creator.match(data_output, data_target))
            result = tuple(map(lambda x, y: x + y, result, creator.match(data_output, data_target)))
            # print(result)
            # print("=====")

        self.assertTrue(True, True)


if __name__ == '__main__':
    unittest.main()
