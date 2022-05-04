from scipy.interpolate import splev, splprep
from numpy import repeat
from numpy.ma import arange
from shapely.geometry import LineString

# Constants
rounding_precision = 3
interpolation_distance = 1
smoothness = 0
min_num_nodes = 20
CRASHED = 1
NO_CRASH = 0


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


def generate_left_marking(road_nodes, distance=3.9):
    return _generate_lane_marking(road_nodes, "left", distance)


def generate_right_marking(road_nodes, distance=3.9):
    return _generate_lane_marking(road_nodes, "right", distance)


def _generate_lane_marking(road_nodes, side, distance):
    """
    BeamNG has troubles rendering/interpolating textures when nodes are too close to each other, so we need
    to resample them.
    To Generate Lane marking:
     1 Compute offset from the road spice (this creates points that are too close to each other to be interpolated by BeamNG)
     2 Reinterpolate those points using Cubic-splines
     3 Resample the spline at 10m distance
    """
    road_spine = LineString([(rn[0], rn[1]) for rn in road_nodes])
    if side == "left":
        x, y = road_spine.parallel_offset(distance, side, resolution=16, join_style=1, mitre_limit=5.0).coords.xy
    else:
        tmp_ls = road_spine.parallel_offset(distance, side, resolution=16, join_style=1, mitre_limit=5.0)
        laneline_list = list(tmp_ls.coords).copy()
        laneline_list.reverse()
        x, y = LineString(laneline_list).coords.xy

    interpolated_points = interpolate([(p[0], p[1]) for p in zip(x, y)], sampling_unit=10)
    return [(p[0], p[1], 0, 0.1) for p in interpolated_points]
