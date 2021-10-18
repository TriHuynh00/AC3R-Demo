import unittest

from .test_mutate_speed_class import TestMutateSpeedClass
from .test_mutate_initial_point_class import TestMutateInitialPointClass


def load_tests(suite, loader):
    suite.addTests(loader.loadTestsFromTestCase(TestMutateSpeedClass))
    suite.addTests(loader.loadTestsFromTestCase(TestMutateInitialPointClass))
    return suite
