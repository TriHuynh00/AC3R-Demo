import json
import unittest
from models import _categorize_report


data_targets = json.loads('{"v1":[{"name":"ANY","damage":1.4163608690724}]}')


class TestPoliceReportTypeA(unittest.TestCase):
    def test_case_01(self):
        expected = (1, 0, 1)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets:
            report_data = data_targets[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_02(self):
        expected = (1, 0, 1)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets:
            report_data = data_targets[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_03(self):
        expected = (0, 1, 1)
        data_outputs = [{"name": "NON_DEFINED", "damage": 1}]
        for report in data_targets:
            report_data = data_targets[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            self.assertEqual(expected, creator.match_operation(data_outputs, targets))

    def test_case_04(self):
        expected = Exception
        data_outputs = []
        for report in data_targets:
            report_data = data_targets[report]
            creator = _categorize_report(report_data)
            targets = [part["name"] for part in report_data]
            with self.assertRaises(expected):
                creator.match_operation(data_outputs, targets)


if __name__ == '__main__':
    unittest.main()
