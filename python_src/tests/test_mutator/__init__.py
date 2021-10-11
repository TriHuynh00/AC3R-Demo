import unittest

from .test_mutator_A import TestMutatorTypeA


def load_tests(suite, loader):
    suite.addTests(loader.loadTestsFromTestCase(TestMutatorTypeA))
    return suite
