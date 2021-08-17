import json
import unittest
from models import categorize_report


data_targets_01 = json.loads('{"v1":[{"name":"R","damage":1.4163608690724},{"name":"F","damage":0.095952279865742}]}')
data_targets_02 = json.loads('{"v1":[{"name":"F","damage":0.053689561784267}, {"name":"L","damage":0.053689561784267}]}')
data_targets_03 = json.loads('{"v1":[{"name":"M","damage":1.4163608690724},{"name":"R","damage":0.095952279865742},'
                             '{"name":"F","damage":0.095952279865742}]}')


class TestPoliceReportTypeBC(unittest.TestCase):
    def test_simulation_F_must_match_SIDECOMPSHORT_report_R_F(self):
        # 1 point for number of matching crashed-component: ['F']
        # 3 points for number of matching non-crashed components: ['M', 'B', 'L']
        # Total points can reach is 5 ['F', 'M', 'B', 'L', 'R']
        expected = (1, 3, 5)
        data_outputs = [{"name": "F", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_must_match_SIDECOMPSHORT_report_F_L(self):
        # 2 point for number of matching crashed-component: ['F', 'L']
        # 3 points for number of matching non-crashed components: ['M', 'B', 'R']
        expected = (2, 3, 5)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FR_must_match_SIDECOMPSHORT_report_F_R_M(self):
        # 1 point for number of matching crashed-component: ['F']
        # 1 point for number of matching non-crashed components: ['B']
        expected = (1, 1, 5)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))


if __name__ == '__main__':
    unittest.main()
