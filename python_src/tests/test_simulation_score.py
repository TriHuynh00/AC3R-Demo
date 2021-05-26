import unittest
from models import SimulationScore


class TestSimScore(unittest.TestCase):
    def test_formula_for_Case06(self):
        expected = 1.7
        target = SimulationScore.formula(alpha=0.2, beta=0.1, tpl=(2, 3, 5))
        self.assertEqual(expected, target)

    def test_formula_for_sample_multi_reports(self):
        expected = 1.9
        target = SimulationScore.formula(alpha=0.2, beta=0.1, tpl=(2, 5, 11))
        self.assertEqual(expected, target)


if __name__ == '__main__':
    unittest.main()
