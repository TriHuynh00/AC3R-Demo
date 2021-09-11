import json
import unittest
from models import categorize_report


data_targets = json.loads('{"v1":[{"name":"ANY","damage":1.4163608690724}]}')


class TestPoliceReportTypeA(unittest.TestCase):
    def test_simulation_FL_must_match_ANY_report(self):
        expected = (1, 0, 1)
        data_outputs = [{"name": "FL", "damage": 1}]
        for report in data_targets:
            report_data = data_targets[report]
            creator = categorize_report(report_data)
            self.assertEqual(expected, creator.match(data_outputs, [part["name"] for part in report_data]))

    def test_simulation_FL_FR_must_match_ANY_report(self):
        expected = (1, 0, 1)
        data_outputs = [{"name": "FL", "damage": 1}, {"name": "FR", "damage": 1}]
        for report in data_targets:
            report_data = data_targets[report]
            creator = categorize_report(report_data)
            self.assertEqual(expected, creator.match(data_outputs, [part["name"] for part in report_data]))

    def test_exception_is_raised_when_unknown_element_is_found(self):
        expected = Exception
        data_outputs = [{"name": "NON_DEFINED", "damage": 1}]
        for report in data_targets:
            report_data = data_targets[report]
            creator = categorize_report(report_data)
            with self.assertRaises(expected):
                creator.match(data_outputs, [])

    def test_exception_is_raised_when_no_crash(self):
        expected = Exception
        data_outputs = []
        for report in data_targets:
            report_data = data_targets[report]
            creator = categorize_report(report_data)
            with self.assertRaises(expected):
                creator.match(data_outputs, data_targets)


if __name__ == '__main__':
    unittest.main()
