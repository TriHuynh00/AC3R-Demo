import unittest
from models import KMeans


class TestKMeans(unittest.TestCase):
    def test_kmeans_exception_when_number_elements_are_less_than_number_clusters(self):
        expected = Exception
        data = [10, 10]
        with self.assertRaises(expected):
            target = KMeans(data).get_clusters()

    def test_kmeans_exception_when_all_elements_are_equals(self):
        expected = Exception
        data = [10, 10, 10, 10, 10]
        with self.assertRaises(expected):
            target = KMeans(data).get_clusters()

    def test_kmeans_6_elements_are_not_equals(self):
        expected = {'LOW': [0.002332531525621401, 0.041666666666666664, 0.05673758865248227],
                    'MED': [0.19858156028368795, 0.4], 'HIGH': [1.0]}
        data = [0.19858156028368795, 0.041666666666666664, 0.002332531525621401, 0.4, 0.05673758865248227, 1]
        k_means = KMeans(data)
        target = k_means.get_clusters()
        self.assertEqual(expected, target)


if __name__ == '__main__':
    unittest.main()
