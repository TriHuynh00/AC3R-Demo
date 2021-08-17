import unittest

from .test_report_A import TestPoliceReportTypeA
from .test_report_B import TestPoliceReportTypeB
from .test_report_C import TestPoliceReportTypeC
from .test_report_D import TestPoliceReportTypeD
from .test_report_BC import TestPoliceReportTypeBC
from .test_multi_reports import TestMultiReports


def load_tests(loader):
    suite = unittest.TestSuite()
    suite.addTests(loader.loadTestsFromTestCase(TestPoliceReportTypeA))
    suite.addTests(loader.loadTestsFromTestCase(TestPoliceReportTypeB))
    suite.addTests(loader.loadTestsFromTestCase(TestPoliceReportTypeC))
    suite.addTests(loader.loadTestsFromTestCase(TestPoliceReportTypeD))
    suite.addTests(loader.loadTestsFromTestCase(TestPoliceReportTypeBC))
    suite.addTests(loader.loadTestsFromTestCase(TestMultiReports))

    return suite
