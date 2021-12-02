import json
import unittest
from models import categorize_report


data_targets_01 = json.loads('{"v1":[{"name":"FR","damage":1.4163608690724},{"name":"FL","damage":0.095952279865742}]}')
data_targets_02 = json.loads('{"v1":[{"name":"FL","damage":0.053689561784267}, {"name":"FR","damage":0.053689561784267}, {"name":"ML","damage":0.053689561784267}]}')
data_targets_03 = json.loads('{"v1":[{"name":"ML","damage":0.053689561784267}]}')


class TestPoliceReportTypeD(unittest.TestCase):
    def test_simulation_FL_must_match_SIDECOMP_report_FR_FL(self):
        expected = (1, 4, 6)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_must_match_SIDECOMP_report_FR_FL(self):
        expected = (2, 4, 6)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_must_match_SIDECOMP_report_FR_FL_ML(self):
        expected = (1, 3, 6)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_must_match_SIDECOMP_report_FR_FL_ML(self):
        expected = (2, 3, 6)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_not_match_SIDECOMP_report_ML(self):
        expected = (0, 4, 6)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_FL_FR_not_match_SIDECOMP_report_ML(self):
        expected = (0, 3, 6)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_L_match_SIDECOMP_report_ML(self):
        expected = (1, 3, 6)
        data_outputs = [{"name": "L", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_R_match_SIDECOMP_report_ML(self):
        expected = (0, 2, 6)
        data_outputs = [{"name": "R", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_F_match_SIDECOMP_report_ML(self):
        expected = (0, 3, 6)
        data_outputs = [{"name": "F", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_M_match_SIDECOMP_report_ML(self):
        expected = (1, 4, 6)
        data_outputs = [{"name": "M", "damage": 1}]
        for report in data_targets_03:
            data_targets = data_targets_03[report]
            creator = categorize_report(data_targets)
            self.assertEqual(expected, creator.match(data_outputs, data_targets))

    def test_simulation_B_match_SIDECOMP_report_ML(self):
        expected = (0, 3, 6)
        data_outputs = [{"name": "B", "damage": 1}]
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
