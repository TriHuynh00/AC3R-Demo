from scipy.interpolate import splev, splprep
from numpy import repeat, array, sqrt, inf, cross, dot
from numpy.ma import arange
from shapely.geometry import LineString, Point
from shapely.affinity import translate, rotate
from math import degrees, atan2, copysign

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
