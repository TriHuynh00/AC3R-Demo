import json
import unittest
from models import categorize_report


data_targets_01 = json.loads('{"v1":[{"name":"F","damage":1.4163608690724}]}')
data_targets_02 = json.loads('{"v1":[{"name":"F","damage":0.053689561784267}, {"name":"M","damage":0.053689561784267}]}')
data_targets_03 = json.loads('{"v1":[{"name":"M","damage":0.053689561784267}]}')


class TestPoliceReportTypeB(unittest.TestCase):
    def test_simulation_FL_must_match_COMP_report_F(self):
        expected = (1, 2, 3)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_must_match_COMP_report_F(self):
        expected = (1, 2, 3)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_must_match_COMP_report_F_M(self):
        expected = (1, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_must_match_COMP_report_F_M(self):
        expected = (1, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_not_match_COMP_report_M(self):
        expected = (0, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_L_match_COMP_report_M(self):
        expected = (1, 0, 3)
        data_outputs = [{"name": "L", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_R_match_COMP_report_M(self):
        expected = (1, 0, 3)
        data_outputs = [{"name": "R", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_M_match_COMP_report_M(self):
        expected = (1, 2, 3)
        data_outputs = [{"name": "M", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_not_match_COMP_report_M(self):
        expected = (0, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_exception_is_raised_when_no_crash(self):
        expected = Exception
        data_outputs = []
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            with self.assertRaises(expected):
                creator.match(data_outputs, data_targets)

    def test_exception_is_raised_when_unknown_element_is_found(self):
        expected = Exception
        data_outputs = [{"name": "NON_DEFINED", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            with self.assertRaises(expected):
                creator.match(data_outputs, data_targets)


if __name__ == '__main__':
    unittest.main()
