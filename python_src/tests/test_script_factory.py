from models import ScriptFactory
import unittest
import matplotlib.pyplot as plt
from shapely.geometry import Point


class ScriptVisualizationTest(unittest.TestCase):
    def test_load_point(self):
        p1_test = [-2.0, 86.84551724137933, 0]
        p2_test = [-2.0, 17.880000000000006, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(script.p1, Point(-2.0, 86.84551724137933), "P1 should be equal to (-2, 86)")
        self.assertEqual(script.p2, Point(-2.0, 17.880000000000006), "P2 should be equal to (-2, 17)")

    def test_compute_angle_for_south(self):
        p1_test = [-2.0, 86.84551724137933, 0]
        p2_test = [-2.0, 17.880000000000006, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(270, script.angle, "Angle South should be 270")

    def test_compute_angle_for_west(self):
        p1_test = [-2.0, 86.84551724137933, 0]
        p2_test = [-30.0, 86.84551724137933, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(180, script.angle, "Angle West should be 180")

    def test_compute_angle_for_east(self):
        p1_test = [-2.0, 86.84551724137933, 0]
        p2_test = [30.0, 86.84551724137933, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(0, script.angle, "Angle East should be 0")

    def test_compute_angle_for_north(self):
        p1_test = [-2.0, 17.880000000000006, 0]
        p2_test = [-2.0, 86.84551724137933, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(90, script.angle, "Angle North should be 90")

    def test_compute_angle_for_normal(self):
        p1_test = [89.845517, 87.845517, 0]
        p2_test = [20.88, 18.88, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(225, script.angle, "Angle should be 225")

    def test_compute_script_point(self):
        p1_test = [89.845517, 87.845517, 0]
        p2_test = [20.88, 18.88, 0]
        script = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1])
        self.assertEqual(Point(86.295517, 84.295517), script.compute_script_point(script.p1, 5))

    def test_visualize_normal_scripts(self):
        p1_test = [89.845517, 87.845517, 0]
        p2_test = [20.88, 18.88, 0]
        points = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1]).compute_scripts(speeds=[30])
        segment_x = [p[0] for p in points]
        segment_y = [p[1] for p in points]

        plt.plot(segment_x, segment_y, 'black')
        plt.scatter(segment_x, segment_y, c='y')
        plt.scatter(segment_x[0], segment_y[0], c='r')
        plt.show()

        sample_script = [(89.845517, 87.845517, 30), (86.295517, 84.295517, 30), (82.745517, 80.745517, 30),
                         (79.195517, 77.195517, 30), (75.645517, 73.645517, 30), (72.095517, 70.095517, 30)]
        self.assertEqual(sample_script, points[0:6], "Should be equal to sample script")

    def test_visualize_scripts(self):
        p1_test = [-2.0, 86.84551724137933, 0]
        p2_test = [-2.0, 17.880000000000006, 0]
        points = ScriptFactory(p1_test[0], p1_test[1], p2_test[0], p2_test[1]).compute_scripts(speeds=[30])
        segment_x = [p[0] for p in points]
        segment_y = [p[1] for p in points]

        plt.plot(segment_x, segment_y, 'black')
        plt.scatter(segment_x, segment_y, c='y')
        plt.scatter(segment_x[0], segment_y[0], c='r')
        plt.show()

        sample_script = [(-2.0, 86.84551724137933, 30), (-2.0, 81.84551724137933, 30), (-2.0, 76.84551724137933, 30),
                         (-2.0, 71.84551724137933, 30), (-2.0, 66.84551724137933, 30), (-2.0, 61.845517241379326, 30)]

        self.assertEqual(sample_script, points[0:6], "Should be equal to sample script")


if __name__ == '__main__':
    unittest.main()
