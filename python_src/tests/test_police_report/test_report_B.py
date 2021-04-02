import json
import unittest
from models import _categorize_report


data_targets_01 = json.loads('{"v1":[{"name":"F","damage":1.4163608690724}]}')
data_targets_02 = json.loads('{"v1":[{"name":"F","damage":0.053689561784267}, {"name":"M","damage":0.053689561784267}]}')
data_targets_03 = json.loads('{"v1":[{"name":"M","damage":0.053689561784267}]}')


class TestPoliceReportTypeB(unittest.TestCase):
    def test_case_01(self):
        expected = (1, 2, 3)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_01:
            report_data = data_targets_01[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_02(self):
        expected = (1, 2, 3)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_01:
            report_data = data_targets_01[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_03(self):
        expected = (1, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_02:
            report_data = data_targets_02[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_04(self):
        expected = (1, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_02:
            report_data = data_targets_02[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_05(self):
        expected = (0, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets_03:
            report_data = data_targets_03[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_06(self):
        expected = (0, 1, 3)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets_03:
            report_data = data_targets_03[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_07(self):
        expected = Exception
        data_outputs = []
        for report in data_targets_03:
            report_data = data_targets_03[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            with self.assertRaises(expected):
                creator.match_operation(data_outputs, targets)

    def test_case_08(self):
        expected = (0, 2, 3)
        data_outputs = [{"name": "NON_DEFINED", "damage": 1}]
        for report in data_targets_03:
            report_data = data_targets_03[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))


if __name__ == '__main__':
    unittest.main()
