from shapely.geometry import Point
from math import degrees, atan2, copysign
from numpy import array, cross, dot
from models.ac3rp import common

PARKING_CONSTRAINT = 0.0001


class Parking:
    def __init__(self, point_a: Point):
        self.type = "parking"
        self.length = PARKING_CONSTRAINT
        self.coords = [point_a]


class Straight:
    def __init__(self, point_a: Point, point_b: Point):
        # Straight segment - Note we care about length only at this point
        # Rotation is implied by the previous elements or initial rotation
        self.type = "straight"
        self.length = point_a.distance(point_b)
        self.coords = [point_a, point_b]


class Turn:
    def __init__(self, point_a: Point, point_b: Point, point_c: Point):
        self.type = "turn"
        # Interpolate the arc and find the corresponding values...
        radius, center = common.find_radius_and_center(point_a, point_b, point_c)

        # Find the angle between: the vectors center-initial_point, center-final_point
        # initialize arrays
        A = array([point_a.x - center.x, point_a.y - center.y])
        B = array([point_c.x - center.x, point_c.y - center.y])
        # For us clockwise must be positive so we need to invert the sign
        direction = 1.0 * copysign(1.0, cross(A, B))
        angle_between_segments = atan2(abs(cross(A, B)), dot(A, B))
        #
        the_angle = degrees(direction * angle_between_segments)
        self.type = "turn"
        self.angle = the_angle
        self.radius = radius
        self.coords = [point_a, point_b, point_c]
