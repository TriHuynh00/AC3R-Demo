from scipy.interpolate import splev, splprep
from numpy import repeat, array, sqrt, inf, cross, dot
from numpy.ma import arange
from shapely.geometry import LineString
from shapely.affinity import translate, rotate
from math import degrees, atan2, copysign
from shapely.geometry import Point
from shapely.geometry.polygon import Polygon
from typing import List, Tuple, Optional

# Constants
rounding_precision = 3
interpolation_distance = 1
smoothness = 0
min_num_nodes = 20
CRASHED = 1
NO_CRASH = 0


def find_radius_and_center(p1, p2, p3):
    """
    Returns the center and radius of the circle passing the given 3 points.
    In case the 3 points form a line, returns (None, infinity).
    """
    temp = p2.x * p2.x + p2.y * p2.y
    bc = (p1.x * p1.x + p1.y * p1.y - temp) / 2
    cd = (temp - p3.x * p3.x - p3.y * p3.y) / 2
    det = (p1.x - p2.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p2.y)

    if abs(det) < 1.0e-6:
        return inf, None

    # Center of circle
    cx = (bc * (p2.y - p3.y) - cd * (p1.y - p2.y)) / det
    cy = ((p1.x - p2.x) * cd - (p2.x - p3.x) * bc) / det

    radius = sqrt((cx - p1.x) ** 2 + (cy - p1.y) ** 2)

    return radius, Point(cx, cy)


def interpolate(road_nodes, sampling_unit=interpolation_distance):
    """
        Interpolate the road points using cubic splines and ensure we handle 4F tuples for compatibility
    """
    old_x_vals = [t[0] for t in road_nodes]
    old_y_vals = [t[1] for t in road_nodes]

    # This is an approximation based on whatever input is given
    road_length = LineString([(t[0], t[1]) for t in road_nodes]).length

    num_nodes = int(road_length / sampling_unit)
    if num_nodes < min_num_nodes:
        num_nodes = min_num_nodes

    assert len(old_x_vals) >= 2, "You need at leas two road points to define a road"
    assert len(old_y_vals) >= 2, "You need at leas two road points to define a road"

    if len(old_x_vals) == 2:
        # With two points the only option is a straight segment
        k = 1
    elif len(old_x_vals) == 3:
        # With three points we use an arc, using linear interpolation will result in invalid road tests
        k = 2
    else:
        # Otheriwse, use cubic splines
        k = 3

    pos_tck, pos_u = splprep([old_x_vals, old_y_vals], s=smoothness, k=k)
    step_size = 1 / num_nodes
    unew = arange(0, 1 + step_size, step_size)
    new_x_vals, new_y_vals = splev(unew, pos_tck)
    new_z_vals = repeat(0.0, len(unew))

    if len(road_nodes[0]) > 2:
        # Recompute width
        old_width_vals = [t[3] for t in road_nodes]
        width_tck, width_u = splprep([pos_u, old_width_vals], s=smoothness, k=k)
        _, new_width_vals = splev(unew, width_tck)

        # Return the 4-tuple with default z and defatul road width
        return list(zip([round(v, rounding_precision) for v in new_x_vals],
                        [round(v, rounding_precision) for v in new_y_vals],
                        [round(v, rounding_precision) for v in new_z_vals],
                        [round(v, rounding_precision) for v in new_width_vals]))
    else:
        return list(zip([round(v, rounding_precision) for v in new_x_vals],
                        [round(v, rounding_precision) for v in new_y_vals]))


def compute_initial_state(driving_actions):
    # "driving-actions": [
    #         {
    #           "name": "follow",
    #           "trajectory": [
    #             [10.0, 18.0, 0.0],
    #             [92.0, 18.0, 0.0]
    #           ]
    #         },
    # Get the points defining the initial part of the first driving actions for the vehicle
    trajectory_points = driving_actions[0]["trajectory"][0]

    initial_location = (trajectory_points[0][0], trajectory_points[0][1])

    if len(trajectory_points) == 2:
        initial_point = Point(trajectory_points[0][0], trajectory_points[0][1])
        final_point = Point(trajectory_points[1][0], trajectory_points[1][1])
    elif len(trajectory_points) == 3:
        # Find the circle
        initial_arc_point = Point(trajectory_points[0][0], trajectory_points[0][1])
        middle_arc_point = Point(trajectory_points[1][0], trajectory_points[1][1])
        final_arc_point = Point(trajectory_points[2][0], trajectory_points[2][1])
        #
        radius, center = find_radius_and_center(initial_arc_point, middle_arc_point, final_arc_point)
        # Take the vector from the center to the initial point
        the_radius_vector = LineString([center, initial_arc_point])
        # Translate that to the origin
        the_radius_vector = translate(the_radius_vector, xoff=-center.x, yoff=-center.y)

        # We really care only about the sign to understand which of the two tangents to take
        # Find the angle between: the vectors center-initial_point, center-final_point
        # initialize arrays
        A = array([initial_arc_point.x - center.x, initial_arc_point.y - center.y])
        B = array([final_arc_point.x - center.x, final_arc_point.y - center.y])
        # For us clockwise must be positive so we need to invert the sign
        direction = -1.0 * copysign(1.0, cross(A, B))
        angle_between_segments = atan2(abs(cross(A, B)), dot(A, B))
        #
        the_angle = degrees(direction * angle_between_segments)
        if the_angle >= 0.0:
            the_radius_vector = rotate(the_radius_vector, 90.0, (0, 0), use_radians=False)
        else:
            the_radius_vector = rotate(the_radius_vector, -90.0, (0, 0), use_radians=False)

        # This should be the origin !
        # initial_point = Point(the_radius_vector.coords[0])
        initial_point = Point(0, 0)
        final_point = Point(the_radius_vector.coords[-1])
    elif len(trajectory_points) == 1:
        return initial_location, 0
    else:
        raise Exception("Not enough points to compute the initial state of vehicle")

    # Find the angle between: the vectors center-initial_point, center-final_point
    # initialize arrays
    NORTH = array([0, 1])
    B = array([final_point.x - initial_point.x, final_point.y - initial_point.y])
    # For us clockwise must be positive so we need to invert the sign
    direction = copysign(1.0, cross(NORTH, B))
    angle_between_segments = atan2(abs(cross(NORTH, B)), dot(NORTH, B))
    #
    intial_rotation = degrees(direction * angle_between_segments)

    return initial_location, intial_rotation


def remove_duplicates(seq):
    seen = set()
    seen_add = seen.add
    return [x for x in seq if not (x in seen or seen_add(x))]


def interpolate(road_nodes, sampling_unit=interpolation_distance):
    """
        Interpolate the road points using cubic splines and ensure we handle 4F tuples for compatibility
    """
    old_x_vals = [t[0] for t in road_nodes]
    old_y_vals = [t[1] for t in road_nodes]

    # This is an approximation based on whatever input is given
    road_length = LineString([(t[0], t[1]) for t in road_nodes]).length

    num_nodes = int(road_length / sampling_unit)
    if num_nodes < min_num_nodes:
        num_nodes = min_num_nodes

    assert len(old_x_vals) >= 2, "You need at leas two road points to define a road"
    assert len(old_y_vals) >= 2, "You need at leas two road points to define a road"

    if len(old_x_vals) == 2:
        # With two points the only option is a straight segment
        k = 1
    elif len(old_x_vals) == 3:
        # With three points we use an arc, using linear interpolation will result in invalid road tests
        k = 2
    else:
        # Otherwise, use cubic splines
        k = 3

    pos_tck, pos_u = splprep([old_x_vals, old_y_vals], s=smoothness, k=k)
    step_size = 1 / num_nodes
    unew = arange(0, 1 + step_size, step_size)
    new_x_vals, new_y_vals = splev(unew, pos_tck)
    new_z_vals = repeat(0.0, len(unew))

    if len(road_nodes[0]) > 2:
        # Recompute width
        old_width_vals = [t[3] for t in road_nodes]
        width_tck, width_u = splprep([pos_u, old_width_vals], s=smoothness, k=k)
        _, new_width_vals = splev(unew, width_tck)

        # Return the 4-tuple with default z and default road width
        return list(zip([round(v, rounding_precision) for v in new_x_vals],
                        [round(v, rounding_precision) for v in new_y_vals],
                        [round(v, rounding_precision) for v in new_z_vals],
                        [round(v, rounding_precision) for v in new_width_vals]))
    else:
        return list(zip([round(v, rounding_precision) for v in new_x_vals],
                        [round(v, rounding_precision) for v in new_y_vals]))


def is_parallel(line1: List, line2: List):
    """
    Take 2 list of coordinates and check if they are parallel.
    """
    ax = line1[1][0] - line1[0][0]
    ay = line1[1][1] - line1[0][1]
    bx = line2[1][0] - line2[0][0]
    by = line2[1][1] - line2[0][1]

    if ax * by == ay * bx:
        return True
    return False


def is_inside_polygon(point: Point, polygon: Polygon):
    """
    Check if the point belongs to the given polygon.
    """
    return polygon.contains(point)


def generate_random_point_within_circle(center: Point, minR: int, maxR: int):
    """
    Generate a random point whose radius is less than maxR
    and larger than minR.

    Returns:
        Tuple (x, y)
    """
    import math
    import random
    r = math.sqrt(random.random() * (maxR ** 2 - minR ** 2) + minR ** 2)
    theta = random.random() * 2 * math.pi
    x = center.x + r * math.cos(theta)
    y = center.y + r * math.sin(theta)
    return x, y


def generate_random_point_within_line(center: Point, delta: Tuple,
                                      distance: int = None,
                                      minR: int = None, maxR: int = None,
                                      mode: int = 0):
    """
    Generate a random point by given a line equation,
    central point, min and max distance.

    Args:
        center (Point): Given a center point
        minR (int): Maximum distance between a new point and center point
        maxR (int): Maximum distance between a new point and center point
        distance (int): Distance between a new point and center point
        delta (Tuple): Given line equation (a, c) e.g y = ax + b
        mode (int): How to compute a new initial point.
            0: A random point belongs to delta with given distance
            1: A random point belongs to delta with min and max distance

    Returns:
        Tuple (x, y)
    """
    import math
    import random
    # Reference:
    # https://math.stackexchange.com/questions/426807/how-does-this-vector-addition-work-in-geometry

    if mode == 0:
        distance = distance
        random_direction = 1
    else:
        distance = random.randint(minR, maxR)
        random_direction = 1 if random.random() < 0.5 else -1

    if delta[0] is None:  # any x, y unchanged
        point2 = Point(center.x + random.random(), delta[1])
    elif delta[1] is None:  # any y, x unchanged
        point2 = Point(delta[0], center.y + random.random())
    else:
        a, b = delta
        x = center.x + random.random()
        point2 = Point(x, a * x + b)

    v = (point2.x - center.x, point2.y - center.y)
    length_v = math.sqrt(v[0] ** 2 + v[1] ** 2)
    x_new = center.x + distance / length_v * v[0] * random_direction
    y_new = center.y + distance / length_v * v[1] * random_direction

    return x_new, y_new


def translate_ls_to_new_origin(lst: LineString, new_origin: Point):
    """
    Translate an existed linestring to a new origin.

    Args:
        lst (LineString): An existed linestring
        new_origin (Point): A new origin of a new linestring eg (x, y)
    Returns:
        new_lst (LineString)
    """
    import math
    first, last = lst.boundary
    dx = first.x - new_origin.x
    dy = first.y - new_origin.y

    new_lst = list()
    for p in list(lst.coords):
        new_lst.append((p[0] - dx, p[1] - dy))

    if math.isclose(lst.length, LineString(new_lst).length, rel_tol=1e-3) is False:
        # two values are approximately equal or “close” to each other, 3 digits after comma
        print(f'Exception: Generated new line is not the same length! lst: '
              f'{lst.length} vs new_lst {LineString(new_lst).length}')
    if not is_parallel(lst.coords, new_lst):
        print(f'Exception: Generated new line is not parallel to the old line!')
        print(f'Old line: ')
        print(lst.coords)
        print(f'New line: ')
        print(new_lst)
        print("====")
    return LineString(new_lst)


def mutate_initial_point(lst: LineString,
                         delta: Tuple,
                         distance: int = None,
                         minR: int = None, maxR: int = None,
                         num_points: int = 1,
                         mode: int = 0):
    """
    Mutate an initial point of vehicle trajectory by generated a new initial point

    Args:
        lst (LineString): LineString of vehicle's trajectory
        delta (Tuple): Given line equation (a, c) y = ax + b going through the initial point of vehicle. This equation
                       also guarantees the generated point is in the same line of the initial point
        distance (int): A distance between a new point and center point
        minR (int): Maximum distance between a new point and center point
        maxR (int): Maximum distance between a new point and center point
        num_points (int): An expected number of point we would generate
        mode (int): How to compute a new initial point.
            0: A random point belongs to delta with given distance
            1: A random point belongs to delta with min and max distance
            2: A random point belongs to circle with min and max radius


    Returns:
        random_points (List): A list of new initial points on the circle or on the same line of an old initial point
    """
    first, last = lst.boundary
    if mode == 0:
        return [generate_random_point_within_line(center=first, delta=delta, distance=distance, mode=mode) for i in
                range(num_points)]
    elif mode == 1:
        return [generate_random_point_within_line(center=first, delta=delta, minR=minR, maxR=maxR, mode=mode) for i in
                range(num_points)]

    random_points = [generate_random_point_within_circle(first, minR, maxR) for i in range(num_points)]
    return random_points


def cal_equation_line_two_points(p1, p2):
    """
    Compute a line equation going through 2 random points.

    Returns:
        Tuple (a, b): y = ax + b
    """
    if p1.x == p2.x:
        # print("Line Solution is x = {x0}".format(x0=p1.x))
        return p1.x, None
    if p1.y == p2.y:
        # print("Line Solution is y = {y0}".format(y0=p1.y))
        return None, p1.y

    a = (p1.y - p2.y) / (p1.x - p2.x)
    b = (p1.x * p2.y - p2.x * p1.y) / (p1.x - p2.x)
    # print("Line Solution is y = {a}x + {b}".format(a=a, b=b))
    return a, b


def cal_equation_line_one_point_and_line(p1: Point, d2: tuple):
    """
    Compute a line equation going through 1 point and making a right angle with
    another line equation.

    Returns:
        Tuple (a, b): y = ax + b
    """
    x1, y1 = p1.x, p1.y

    # d1: y1 = a1*x1 + b1
    # d2: y2 = a2*x2 + b2

    # d1 right angle d2 => a1 * a2 = -1
    a2, b2 = d2
    if a2 == 0:
        # print("Line Solution is y = {y0}".format(y0=y1))
        return None, y1
    if a2 is None:
        # print("Line Solution is x = {x0}".format(x0=x1))
        return x1, None
    a1 = -1 / a2

    # p1 in d1
    b1 = y1 - (a1 * x1)
    # print("Line Solution is y = {a}x + {b}".format(a=a1, b=b1))
    return a1, b1
