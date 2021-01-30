# from self_driving.road_polygon import RoadPolygon
from shapely.geometry import LineString
from scipy.interpolate import splev, splprep
from scipy.spatial.transform import Rotation as R
from numpy import repeat, linspace, array, sqrt, inf, cross, dot
from numpy.ma import arange

from shapely.geometry import LineString, Point
from shapely.affinity import translate, rotate, scale

from scipy.spatial import geometric_slerp
from math import sin, cos, radians, degrees, atan2, copysign

# Constants
rounding_precision = 3
interpolation_distance = 1
smoothness = 0
min_num_nodes = 20
CRASHED = 1
NO_CRASH = 0


def _find_radius_and_center(p1, p2, p3):
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

def _interpolate(road_nodes, sampling_unit=interpolation_distance):
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


def _compute_initial_state(driving_actions):
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
        radius, center = _find_radius_and_center(initial_arc_point, middle_arc_point, final_arc_point)
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

def f7(seq):
    seen = set()
    seen_add = seen.add
    return [x for x in seq if not (x in seen or seen_add(x))]

class Road:

    @staticmethod
    def from_dict(road_dict):
        #     {
        #       "name" : "road1",
        #       "road_type": "roadway",
        #       "road_shape": "I",
        #       "road_node_list": [
        #         [0.0, 20.0, 0.0, 8.0],
        #         [100.0, 20.0, 0.0, 8.0]
        #       ]
        #     },
        interpolated_road_points = _interpolate(road_dict["road_node_list"])
        return Road(road_dict["name"], road_dict["road_type"], road_dict["road_shape"], interpolated_road_points)

    def __init__(self, name, road_type, road_shape, road_nodes):
        self.name = name
        self.road_type = road_type
        self.road_shape = road_shape
        self.road_nodes = road_nodes


class Vehicle:

    @staticmethod
    def from_dict(vehicle_dict):
        # TODO This code seems to be a bit buggy !
        # Interpolate the road points to get the road
        # "vehicles": [
        #     {
        #       "name": "v1",
        #       "model": null,
        #       "color": "1 0.9 0",
        #       "damage_components": ["side", "left"],
        # 	  "driving-actions": [
        #         {
        #           "name": "follow",
        #           "trajectory": [[
        #             [10.0, 18.0, 0.0],
        #             [92.0, 18.0, 0.0]
        #           ]]
        #         },
        #         { "name" : "turn-right",
        #           "trajectory" : [[
        #             [92.0, 18.0, 0.0],
        #             [93.41, 17.41, 0.0],
        #             [94.0, 16.0, 0.0]
        #           ]]
        #         },
        #         {
        #           "name": "follow",
        #           "trajectory": [[
        #             [94.0, 16.0, 0.0],
        #             [94.0, 0.0, 0.0]
        #           ]]
        #         }
        #       ]
        #     },

        # segments.append({"type": "straight", "length": 100.0})
        # # SX
        # segments.append({"type": "turn", "angle": -60.0, "radius": 50.0})
        # segments.append({"type": "straight", "length": 30.0})
        # # DX
        # segments.append({"type": "turn", "angle": 60.0, "radius": 50.0})

        # # SX
        # segments.append({"type": "turn", "angle": -90.0, "radius": 10.0})
        # Extract all the actions

        driving_actions = []

        for driving_action_dict in vehicle_dict["driving_actions"]:

            trajectory_segments = []

            # Iterate all the trajectory_list, i.e., list of poitns that define the segments
            for trajectory_list in driving_action_dict["trajectory"]:

                if len(trajectory_list) == 1:
                    trajectory_segments.append({
                        "type": "parking",
                        "length": 0
                    })
                elif len(trajectory_list) == 2:
                    # Straight segment - Note we care about length only at this point
                    # Rotation is implied by the previous elements or initial rotation
                    initial_point = Point(trajectory_list[0][0], trajectory_list[0][1])
                    final_point = Point(trajectory_list[1][0], trajectory_list[1][1])

                    trajectory_segments.append({
                        "type": "straight",
                        "length": initial_point.distance(final_point)
                    })

                elif len(trajectory_list) == 3:

                    initial_point = Point(trajectory_list[0][0], trajectory_list[0][1])
                    middle_point = Point(trajectory_list[1][0], trajectory_list[1][1])
                    final_point = Point(trajectory_list[2][0], trajectory_list[2][1])

                    # Interpolate the arc and find the corresponding values...
                    radius, center = _find_radius_and_center(initial_point, middle_point, final_point)

                    # Find the angle between: the vectors center-initial_point, center-final_point
                    # initialize arrays
                    A = array([initial_point.x - center.x, initial_point.y - center.y])
                    B = array([final_point.x - center.x, final_point.y - center.y])
                    # For us clockwise must be positive so we need to invert the sign
                    direction = -1.0 * copysign(1.0, cross(A, B))
                    angle_between_segments = atan2(abs(cross(A, B)), dot(A, B))
                    #
                    the_angle = degrees(direction * angle_between_segments)

                    trajectory_segments.append({
                        "type": "turn",
                        "angle": the_angle,
                        "radius": radius
                    })

                else:
                    raise Exception("Too many points in the trajectory_dict")

            # TODO Refactor to class
            driving_actions.append({
                "name": driving_action_dict["name"],
                "trajectory_segments": trajectory_segments,
                "speed": driving_action_dict["speed"]
            })

        # Extract the initial location: the first point of the trajectory
        initial_location, initial_rotation = _compute_initial_state(vehicle_dict["driving_actions"])


        # Extract the initial rotation. The rotation the first point of the trajectory
        ## TODO rotation is extracted by first interpolating the points

        rot_quat = vehicle_dict["rot_quat"] if "rot_quat" in vehicle_dict else R.from_euler('z', initial_rotation,
                                                                                            degrees=True).as_quat()

        return Vehicle(vehicle_dict["name"],
                       initial_location,
                       initial_rotation,
                       driving_actions,
                       vehicle_dict["color"],
                       rot_quat)

    # Rotation defined against NORTH = [0, 1]
    def __init__(self, name, initial_location, initial_rotation, driving_actions, color, rot_quat):
        self.name = name
        self.initial_location = Point(initial_location[0], initial_location[1])
        self.initial_rotation = initial_rotation
        self.driving_actions = driving_actions
        self.color = color
        self.rot_quat = rot_quat

    def generate_trajectory(self):
        # First generate the trajectory, then rotate it according to NORTH
        # Each piece is generated directly at the origin, rotated and moved in place
        # Finally we rotate everything to NORTH.

        # Interpolate and re-sample the points before returning

        # Collect the trajectory segments from the driving actions
        segments = []
        for driving_action in self.driving_actions:
            segments.extend(driving_action["trajectory_segments"])

        last_location = self.initial_location
        last_rotation = self.initial_rotation

        trajectory_points = [self.initial_location]
        len_coor = []

        for s in segments:
            # Generate the segment
            segment = None
            if s["type"] == "parking":
                return [(last_location.x, last_location.y, self.driving_actions[0]['speed'])]
            if s["type"] == 'straight':
                # Create an horizontal line of given length from the origin
                segment = LineString([(x, 0) for x in linspace(0, s["length"], 8)])
                # Rotate it
                segment = rotate(segment, last_rotation, (0, 0))
                # Move it
                segment = translate(segment, last_location.x, last_location.y)
                # Update last rotation and last location
                last_rotation = last_rotation  # Straight segments do not change the rotation
                last_location = Point(list(segment.coords)[-1])

            elif s["type"] == 'turn':
                # Generate the points over the circle with 1.0 radius
                # # Vector (0,1)
                # start = array([cos(radians(90.0)), sin(radians(90.0))])
                # # Compute this using the angle
                # end = array([cos(radians(90.0 - s["angle"])), sin(radians(90.0 - s["angle"]))])
                start = array([1, 0])

                # Make sure that positive is
                # TODO Pay attention to left/right positive/negative
                end = array([cos(radians(s["angle"])), sin(radians(s["angle"]))])
                # Interpolate over 8 points
                t_vals = linspace(0, 1, 8)
                result = geometric_slerp(start, end, t_vals)
                segment = LineString([Point(p[0], p[1]) for p in result])

                # Translate that back to origin
                segment = translate(segment, -1.0, 0.0)
                # Rotate
                if s["angle"] > 0:
                    segment = rotate(segment, -90.0, (0.0, 0.0), use_radians=False)
                else:
                    segment = rotate(segment, +90.0, (0.0, 0.0), use_radians=False)

                # Scale to radius on both x and y
                segment = scale(segment, s["radius"], s["radius"], 1.0, (0.0, 0.0))
                # Rotate it
                segment = rotate(segment, last_rotation, (0, 0))
                # Translate it
                segment = translate(segment, last_location.x, last_location.y)
                # Update last rotation and last location
                last_rotation = last_rotation + s["angle"]  # Straight segments do not change the rotation
                last_location = Point(list(segment.coords)[-1])

            if segment is not None:
                len_coor.append(len(list(segment.coords)))
                trajectory_points.extend([Point(x, y) for x, y in list(segment.coords)])

        the_trajectory = LineString(f7([(p.x, p.y) for p in trajectory_points]))

        # Make sure we use as reference the NORTH
        the_trajectory = translate(the_trajectory, - self.initial_location.x, - self.initial_location.y)
        # Rotate by -90 deg
        the_trajectory = rotate(the_trajectory, +90.0, (0,0))
        # Translate it back
        the_trajectory = translate(the_trajectory, + self.initial_location.x, + self.initial_location.y)

        # Interpolate and resample uniformly - Make sure no duplicates are there. Hopefully we do not change the order
        # TODO Sampling unit is 5 meters for the moment. Can be changed later
        interpolated_points = _interpolate([(p[0], p[1]) for p in list(the_trajectory.coords)], sampling_unit=5)

        # Concat the speed to the point
        trajectory_points = list(the_trajectory.coords)
        start = 0
        sls = []
        sl_coor = []
        for s in len_coor:
            sl_coor.append([start, start + s])
            start = sl_coor[-1][1] - 1
        for s in sl_coor:
            sls.append(LineString(trajectory_points[s[0]:s[1]]))
        speeds = []
        for a in self.driving_actions:
            speeds.append(a['speed'])

        trajectory_points = []
        for line, speed in zip(sls, speeds):
            for p in interpolated_points:
                point = Point(p[0], p[1])
                if point.distance(line) < 0.5 and p not in trajectory_points:
                    trajectory_points.append((p[0], p[1], speed))

        # Return triplet
        return trajectory_points

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)

class CrashScenario:

    @staticmethod
    def key_exist_in_list(k, lis):
        for d in lis:
            if d['name'] == k:
                return True
        return False

    @staticmethod
    def from_json(ac3r_json_data):
        """
        Take the JSON file generated by AC3R and instantiate a proper CrashScenario object.
        """
        # Define the car trajectories by interpolating the given points according to the following heuristic
        # if only one point is given, that's a location (crash location, parked position, etc)
        # if two points are given, that's a straight segment
        # if three points are given, that's an arc

        roads = []
        for road_dict in ac3r_json_data["roads"]:
            roads.append(Road.from_dict(road_dict))

        vehicles = []
        for vehicle_dict in ac3r_json_data["vehicles"]:
            vehicles.append(Vehicle.from_dict(vehicle_dict))

        return CrashScenario(ac3r_json_data["name"], roads, vehicles)

    def __init__(self, name, roads, vehicles, original_scenario=False):
        # Meta Data
        self.name = name
        self.original_scenario = original_scenario

        # Road Geometry
        self.roads = roads

        # Vehicle Information and trajectory
        self.vehicles = vehicles

        # Scenario score
        self.score = 0

        self.simulation = None

    def cal_fitness(self, police_report=None):
        simulation = self.simulation
        status = simulation["status"]
        if police_report is None or status == NO_CRASH:
            # For testing purpose
            if "distance" in simulation:
                self.score = -simulation["distance"]
            else:
                v1 = simulation["vehicles"][0]
                v2 = simulation["vehicles"][1]
                p1 = Point(v1.positions[-1][0], v1.positions[-1][1])
                p2 = Point(v2.positions[-1][0], v2.positions[-1][1])
                self.score = -p1.distance(p2)
        elif status == CRASHED:
            point = 1
            for v in simulation["vehicles"]:
                police_data = police_report[v.vehicle.vid] # Extract data from report to compare
                v_damage = v.get_damage()
                for k in v_damage:
                    if self.key_exist_in_list(k, police_data): # Damage component exists in police report
                        point += 1
            self.score = point
        # Remove unnecessary attribute
        delattr(self, 'simulation')

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)