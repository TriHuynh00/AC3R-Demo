# from self_driving.road_polygon import RoadPolygon
from shapely.geometry import LineString
from scipy.interpolate import splev, splprep
from numpy import repeat, linspace, array
from numpy.ma import arange

from shapely.geometry import LineString, Point
from shapely.affinity import translate, rotate, scale

from scipy.spatial import geometric_slerp
from math import sin, cos, radians, degrees, atan2

# Constants
rounding_precision = 3
interpolation_distance = 1
smoothness = 0
min_num_nodes = 20


def _interpolate(road_nodes):
    """
        Interpolate the road points using cubic splines and ensure we handle 4F tuples for compatibility
    """
    old_x_vals = [t[0] for t in road_nodes]
    old_y_vals = [t[1] for t in road_nodes]
    old_width_vals = [t[3] for t in road_nodes]

    # This is an approximation based on whatever input is given
    road_lenght = LineString([(t[0], t[1]) for t in road_nodes]).length

    num_nodes = int(road_lenght / interpolation_distance)
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

    width_tck, width_u = splprep([pos_u, old_width_vals], s=smoothness, k=k)
    _, new_width_vals = splev(unew, width_tck)

    # Return the 4-tuple with default z and defatul road width
    return list(zip([round(v, rounding_precision) for v in new_x_vals],
                    [round(v, rounding_precision) for v in new_y_vals],
                    [round(v, rounding_precision) for v in new_z_vals],
                    [round(v, rounding_precision) for v in new_width_vals]))


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
        #           "trajectory": [
        #             [10.0, 18.0, 0.0],
        #             [92.0, 18.0, 0.0]
        #           ]
        #         },
        #         { "name" : "turn-right",
        #           "trajectory" : [
        #             [92.0, 18.0, 0.0],
        #             [93.41, 17.41, 0.0],
        #             [94.0, 16.0, 0.0]
        #           ]
        #         },
        #         {
        #           "name": "follow",
        #           "trajectory": [
        #             [94.0, 16.0, 0.0],
        #             [94.0, 0.0, 0.0]
        #           ]
        #         }
        #       ]
        #     },

        # segments.append({"type": "straight", "lenght": 100.0})
        # # SX
        # segments.append({"type": "turn", "angle": -60.0, "radius": 50.0})
        # segments.append({"type": "straight", "lenght": 30.0})
        # # DX
        # segments.append({"type": "turn", "angle": 60.0, "radius": 50.0})
        # segments.append({"type": "straight", "lenght": 20.0})
        # # SX
        # segments.append({"type": "turn", "angle": -90.0, "radius": 10.0})

        # Extract the initial location and rotation from the trajectory
        initial_location = vehicle_dict["driving-actions"][0]["trajectory"][0]
        ## TODO rotation is extracted by first interpolating the points

        driving_actions = []
        return Vehicle(vehicle_dict["name"], initial_location, initial_rotation, driving_actions)

    # Rotation defined against NORTH = [0, 1]
    def __init__(self, name, initial_location, initial_rotation, driving_actions):
        self.name = name
        self.initial_location = initial_location
        self.initial_rotation = initial_rotation
        self.driving_actions = driving_actions

    def generate_trajectory(self):
        # starting from the initial position and rotation render the vehicle trajectory using the information
        # stored as driving actions

        # Collect the trajectory segments from the driving actions
        segments = []
        for driving_action in self.driving_actions:
            segments.extend(driving_action.trajectory_segments)

        last_rotation = self.initial_rotation
        trajectory_points = [self.initial_point]
        for s in segments:
            # Generate the segment
            segment = None
            if s["type"] == 'straight':
                # Create an vertical line of given lenght
                segment = LineString([(0, y) for y in linspace(0, s["lenght"], 8)])
            elif s["type"] == 'turn':
                # Generate the points over the circle with 1.0 radius
                # Vector (0,1)
                start = array([cos(radians(90.0)), sin(radians(90.0))])
                # Compute this using the angle
                end = array([cos(radians(90.0 - s["angle"])), sin(radians(90.0 - s["angle"]))])
                # Interpolate over 4 points
                t_vals = linspace(0, 1, 8)
                result = geometric_slerp(start, end, t_vals)
                segment = LineString([Point(p[0], p[1]) for p in result])

                # Translate to origin
                segment = translate(segment, 0.0, -1.0, 0.0)
                # Rotate
                if s["angle"] > 0:
                    segment = rotate(segment, +90.0, (0.0, 0.0), use_radians=False)
                else:
                    segment = rotate(segment, -90.0, (0.0, 0.0), use_radians=False)
                # Scale to radius
                segment = scale(segment, s["radius"], s["radius"], 1.0, (0.0, 0.0))

            # Place the segment in the right position by rotating and translating it. Might not be accurate
            if segment is not None:
                # Rotate this piece
                segment = rotate(segment, last_rotation, (0.0, 0.0), use_radians=False)
                # Translate this piece to be attached to the last point generated
                segment = translate(segment, trajectory_points[-1].x, trajectory_points[-1].y)
                # Copy the points into the trajectory. Might not be accurate
                trajectory_points.extend([Point(x, y) for x, y in segment.coords[:]])
                # update the last_rotation based on the last two points of the trajectory

                delta_x = trajectory_points[-1].x - trajectory_points[-2].x
                delta_y = trajectory_points[-1].y - trajectory_points[-2].y

                # Make sure we account for the North vector
                last_rotation = degrees(atan2(delta_y, delta_x)) - 90.0

        # Return triplet
        return [(p.x, p.y, 0.0) for p in trajectory_points]


class CrashScenario:

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
        for road_dict in ac3r_json_data.vehicles["roads"]:
            roads.append(Road.from_dict(road_dict))

        # for vehicle_data in ac3r_json_data.vehicles["vehicles"]:
        #
        #     pass


        pass

    def __init__(self, id, roads, vehicles, crash_point, original_scenario=False):
        # Meta Data
        self.id = id
        self.original_scenario = original_scenario

        # Road Geometry
        self.roads = roads

        # Vehicle Information and trajectory
        self.vehicles = vehicles
        self.crash_point = crash_point



