from shapely.geometry import Point
from numpy import array, cross, dot, deg2rad
from math import degrees, atan2, copysign, cos, sin, radians
import numpy as np
import random

NORTH = array([0, 1])
EAST = array([1, 0])


def _round(deg):
    return np.around(deg, decimals=2)


def _find_angle_with_north(p1, p2):
    # print("N", _find_angle_with_north(Point(0, 0), Point(0, 1)))
    # print("E", _find_angle_with_north(Point(0, 0), Point(1, 0)))
    # print("S", _find_angle_with_north(Point(0, 0), Point(0, -1)))
    # print("W", _find_angle_with_north(Point(0, 0), Point(-1, 0)))
    # NORTH = 0 | EAST = -90 | SOUTH = -180 | WEST = 90
    # Find the angle between given vector and the NORTH vector
    target_vector = array([p2.x - p1.x, p2.y - p1.y])
    angle_between_segments = atan2(abs(cross(EAST, target_vector)), dot(EAST, target_vector))
    direction = copysign(1.0, cross(EAST, target_vector))
    degree = degrees(direction * angle_between_segments)
    if degree < 0:
        degree = degree + 360
    return degree


class ScriptFactory:
    def __init__(self, p1_x, p1_y, p2_x, p2_y):
        self.p1 = Point(p1_x, p1_y)
        self.p2 = Point(p2_x, p2_y)
        self.angle = _find_angle_with_north(self.p1, self.p2)
        self.script = []

    def compute_script_point(self, point, distance=5):
        p = [0, 0]
        p[0] = point.x + distance * _round(cos(radians(self.angle)))
        p[1] = point.y + distance * _round(sin(radians(self.angle)))
        return Point(p[0], p[1])

    def compute_scripts(self, distance=5, speeds=[30]):
        idx = 1
        points = [self.p1]
        while True:
            points.append(self.compute_script_point(self.p1, distance * idx))
            if points[0].distance(points[-1]) > self.p1.distance(self.p2) + distance*5:
                break
            idx += 1
        self.script = [(p.x, p.y, random.choice(speeds)) for p in points]
        return self.script
