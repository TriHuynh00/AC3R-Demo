import json
import unittest
from models import categorize_report

data_targets = json.loads('{"v1":[{"name":"F","damage":0.053689561784267}, {"name":"M","damage":0.053689561784267}],'
                          '"v2":[{"name":"R","damage":1.4163608690724},{"name":"L","damage":0.095952279865742}],'
                          '"v3":[{"name":"ML","damage":0.053689561784267}]}')


class TestMultiReports(unittest.TestCase):
    def test_multi_types_report(self):
        expected = (2, 5, 11)
        data_outputs = {"v1": [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}],
                        "v2": [{"name": "FL", "damage": 1}, {"name": "ML", "damage": 1}],
                        "v3": [{"name": "FL", "damage": 1}]}

        target = (0, 0, 0)
        for vehicle in data_targets:
            data_target = data_targets[vehicle]
            creator = categorize_report(data_target)
            data_output = data_outputs[vehicle]
            target = tuple(map(lambda x, y: x + y, target, creator.match(data_output, data_target)))

        self.assertEqual(expected, target)

    def test_multi_types_report_Case6(self):
        expected = (2, 3, 5)
        data_targets = json.loads('{"vehicle2": [{"name": "F", "damage": 0}],"vehicle1": [{"name": "L", "damage": 0}]}')
        data_outputs = {"vehicle2": [{"name": "FR", "damage": 0.010666666666666666}, {"name": "FR", "damage": 1}],
                        "vehicle1": [{"name": "FL", "damage": 1}]}

        target = (0, 0, 0)
        for vehicle in data_targets:
            data_target = data_targets[vehicle]
            creator = categorize_report(data_target)
            data_output = data_outputs[vehicle]
            target = tuple(map(lambda x, y: x + y, target, creator.match(data_output, data_target)))

        self.assertEqual(expected, target)

    def test_multi_types_report_Case6_max_score(self):
        expected = (2, 3, 5)
        data_targets = json.loads('{"vehicle2": [{"name": "F", "damage": 0}],"vehicle1": [{"name": "L", "damage": 0}]}')
        data_outputs = {"vehicle2": [{"name": "F", "damage": 0.010666666666666666}], "vehicle1": [{"name": "L", "damage": 1}]}

        target = (0, 0, 0)
        for vehicle in data_targets:
            data_target = data_targets[vehicle]
            creator = categorize_report(data_target)
            data_output = data_outputs[vehicle]
            target = tuple(map(lambda x, y: x + y, target, creator.match(data_output, data_target)))

        self.assertEqual(expected, target)


if __name__ == '__main__':
    unittest.main()
