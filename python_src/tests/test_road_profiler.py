import unittest
from models import RoadProfiler, ScriptFactory


class RoadProfilerTest(unittest.TestCase):
    def test_compute_ai_script(self):
        p1_test = [-2.0, 86.84551724137933, 0]
        p2_test = [-2.0, 17.880000000000006, 0]
        points = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1]).compute_scripts()
        color = "1 0 0"
        road_pf = RoadProfiler()
        road_pf.compute_ai_script(points, color)

        sample_script = [{'x': -2.0, 'y': 86.84551724137933, 'z': 0, 't': 0},
                         {'x': -2.0, 'y': 81.84551724137933, 'z': 0, 't': 0.6},
                         {'x': -2.0, 'y': 76.84551724137933, 'z': 0, 't': 1.2},
                         {'x': -2.0, 'y': 71.84551724137933, 'z': 0, 't': 1.7999999999999998}]
        sample_points = [[-2.0, 86.84551724137933, 0], [-2.0, 81.84551724137933, 0],
                         [-2.0, 76.84551724137933, 0], [-2.0, 71.84551724137933, 0]]
        sample_spheres = [[-2.0, 86.84551724137933, 0, 0.25], [-2.0, 81.84551724137933, 0, 0.25],
                          [-2.0, 76.84551724137933, 0, 0.25], [-2.0, 71.84551724137933, 0, 0.25]]
        sample_sphere_colors = [(1.0, 0.0, 0.0, 1), (1.0, 0.0, 0.0, 1),
                                (1.0, 0.0, 0.0, 1), (1.0, 0.0, 0.0, 1)]
        self.assertEqual(sample_script, road_pf.script[0:4])
        self.assertEqual(sample_points, road_pf.points[0:4])
        self.assertEqual(sample_spheres, road_pf.spheres[0:4])
        self.assertEqual(sample_sphere_colors, road_pf.sphere_colors[0:4])

    def test_init_object(self):
        road_pf = RoadProfiler()
        self.assertEqual([], road_pf.script)
        self.assertEqual([], road_pf.points)
        self.assertEqual([], road_pf.spheres)
        self.assertEqual([], road_pf.sphere_colors)


if __name__ == '__main__':
    unittest.main()
