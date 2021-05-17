import unittest
import test_police_report
from test_script_factory import ScriptVisualizationTest
from test_road_profiler import RoadProfilerTest


if __name__ == "__main__":
    runner = unittest.TextTestRunner(verbosity=2)
    loader = unittest.TestLoader()
    suite = test_police_report.load_tests(loader)
    suite.addTests(loader.loadTestsFromTestCase(ScriptVisualizationTest))
    suite.addTests(loader.loadTestsFromTestCase(RoadProfilerTest))
    runner.run(suite)