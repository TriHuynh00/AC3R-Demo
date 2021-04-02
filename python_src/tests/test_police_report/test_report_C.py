import json
import unittest
from models import _categorize_report


data_targets_01 = json.loads('{"v1":[{"name":"R","damage":1.4163608690724},{"name":"L","damage":0.095952279865742}]}')
data_targets_02 = json.loads('{"v1":[{"name":"L","damage":0.053689561784267}]}')


class TestPoliceReportTypeC(unittest.TestCase):
    def test_case_01(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = _categorize_report(data_targets)
            self.assertEqual(expected, creator.match_operation(data_outputs, data_targets))

    def test_case_02(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "ML", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = _categorize_report(data_targets)
            self.assertEqual(expected, creator.match_operation(data_outputs, data_targets))

    def test_case_03(self):
        expected = (2, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_01:
            data_targets = data_targets_01[report]
            creator = _categorize_report(data_targets)
            self.assertEqual(expected, creator.match_operation(data_outputs, data_targets))

    def test_case_04(self):
        expected = (1, 1, 2)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = _categorize_report(data_targets)
            self.assertEqual(expected, creator.match_operation(data_outputs, data_targets))

    def test_case_05(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "MR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = _categorize_report(data_targets)
            self.assertEqual(expected, creator.match_operation(data_outputs, data_targets))

    def test_case_06(self):
        expected = (1, 0, 2)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = _categorize_report(data_targets)
            self.assertEqual(expected, creator.match_operation(data_outputs, data_targets))

    def test_case_07(self):
        expected = Exception
        data_outputs = []
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = _categorize_report(data_targets)
            with self.assertRaises(expected):
                creator.match_operation(data_outputs, data_targets)

    def test_case_08(self):
        expected = Exception
        data_outputs = [{"name": "NON_DEFINED", "damage": 1}]
        for report in data_targets_02:
            data_targets = data_targets_02[report]
            creator = _categorize_report(data_targets)
            with self.assertRaises(expected):
                creator.match_operation(data_outputs, data_targets)


if __name__ == '__main__':
    unittest.main()
