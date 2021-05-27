import json
import unittest
from models import categorize_report


data_targets_01 = json.loads('{"v1":[{"name":"R","damage":1.4163608690724},{"name":"L","damage":0.095952279865742}]}')
data_targets_02 = json.loads('{"v1":[{"name":"L","damage":0.053689561784267}]}')


class TestPoliceReportTypeC(unittest.TestCase):
    def test_simulation_FL_must_match_SIDE_report_R_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_ML_must_match_SIDE_report_R_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "ML", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_must_match_SIDE_report_R_L(self):
        expected = (2, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_must_match_SIDE_report_L(self):
        expected = (1, 1, 2)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_MR_must_match_SIDE_report_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "MR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_must_match_SIDE_report_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_MR_FR_not_match_SIDE_report_L(self):
        expected = (0, 0, 2)
        data_outputs = [{"name": "MR", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_L_match_SIDE_report_L(self):
        expected = (1, 1, 2)
        data_outputs = [{"name": "L", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_R_not_match_SIDE_report_L(self):
        expected = (0, 0, 2)
        data_outputs = [{"name": "R", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_F_match_SIDE_report_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "F", "damage": 1}, {"name": "M", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_M_match_SIDE_report_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "M", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_B_match_SIDE_report_L(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "B", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_exception_is_raised_when_no_crash(self):
        expected = Exception
        data_outputs = []
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            with self.assertRaises(expected):
                creator.match(data_outputs, data_targets)

    def test_exception_is_raised_when_unknown_element_is_found(self):
        expected = Exception
        data_outputs = [{"name": "NON_DEFINED", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            with self.assertRaises(expected):
                creator.match(data_outputs, data_targets)


if __name__ == '__main__':
    unittest.main()
