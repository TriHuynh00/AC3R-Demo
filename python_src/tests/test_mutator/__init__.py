import unittest

from .test_mutator_A import TestMutatorTypeA
from .test_mutator_B import TestMutatorTypeB


def load_tests(suite, loader):
    suite.addTests(loader.loadTestsFromTestCase(TestMutatorTypeA))
    suite.addTests(loader.loadTestsFromTestCase(TestMutatorTypeB))
    return suite
