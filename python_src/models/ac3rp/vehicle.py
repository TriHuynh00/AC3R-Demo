from scipy.spatial.transform import Rotation as R
from numpy import linspace, array
from shapely.geometry import LineString, Point
from shapely.affinity import translate, rotate, scale
from scipy.spatial import geometric_slerp
from math import sin, cos, radians
from typing import List, Tuple
from models.ac3rp import common
from models.ac3rp.movement import Movement

SAMPLING_UNIT = 5


class Vehicle:
    @staticmethod
    def from_dict(key, vehicle_dict, roads):
        # Extract the initial location: the first point of the trajectory
        initial_location, initial_rotation = vehicle_dict["pos"], vehicle_dict["rot"]

        # Attach road data to vehicle
        road_data = None
        for road in roads:
            p0 = Point(initial_location[0], initial_location[1])
            delta = road.road_line_equation
            if common.is_inside_polygon(p0, road.road_poly):
                road_data = {
                    "road_poly": road.road_poly,
                    "road_equation": road.road_line_equation,
                    "mutate_equation": common.cal_equation_line_one_point_and_line(p0, delta)
                }

        # Collect trajectory
        trajectory = []
        for p in vehicle_dict["script"]:
            x: float = p['x']
            y: float = p['y']
            speed: float = vehicle_dict["speed"]
            item = (x, y, speed)
            trajectory.append(item)

        return Vehicle(name=key,
                       initial_location=initial_location,
                       initial_rotation=initial_rotation,
                       color=vehicle_dict["color_code"],
                       road_data=road_data,
                       trajectory=trajectory,
                       speed=vehicle_dict["speed"])

    # Rotation defined against NORTH = [0, 1]
    def __init__(self, name, initial_location, initial_rotation,
                 color, road_data, trajectory=[], distance_to_trigger=-1, speed=0):
        self.name = name
        self.initial_location = Point(initial_location[0], initial_location[1], initial_location[2])
        self.initial_rotation = initial_rotation
        self.color = color
        self.road_data = road_data
        self.distance_to_trigger = distance_to_trigger
        self.trajectory = trajectory
        self.speed = speed

    # def generate_trajectory(self):
    #     # First generate the trajectory, then rotate it according to NORTH
    #     # Each piece is generated directly at the origin, rotated and moved in place
    #     # Finally we rotate everything to NORTH.
    #
    #     # Interpolate and re-sample the points before returning
    #
    #     # Collect the trajectory segments from the driving actions
    #     segments = []
    #     for driving_action in self.movement.translate_to_segments():
    #         segments.extend(driving_action["trajectory_segments"])
    #
    #     # Extract the initial location: the first point of the trajectory
    #     self.initial_location, self.initial_rotation = common.compute_initial_state(self.movement.driving_actions)
    #     self.initial_location = Point(self.initial_location[0], self.initial_location[1])
    #     last_location = self.initial_location
    #     last_rotation = self.initial_rotation
    #
    #     trajectory_points = [self.initial_location]
    #     len_coor = []
    #
    #     for s in segments:
    #         # Generate the segment
    #         segment = None
    #         if s.type == "parking":
    #             segment = LineString([(x, 0) for x in linspace(0, s.length, 2)])
    #             segment = rotate(segment, last_rotation, (0, 0))
    #             segment = translate(segment, last_location.x, last_location.y)
    #             last_rotation = last_rotation  # Parking segments do not change the rotation
    #             last_location = Point(list(segment.coords)[-1])
    #         if s.type == 'straight':
    #             # Create an horizontal line of given length from the origin
    #             segment = LineString([(x, 0) for x in linspace(0, s.length, 8)])
    #             # Rotate it
    #             segment = rotate(segment, last_rotation, (0, 0))
    #             # Move it
    #             segment = translate(segment, last_location.x, last_location.y)
    #             # Update last rotation and last location
    #             last_rotation = last_rotation  # Straight segments do not change the rotation
    #             last_location = Point(list(segment.coords)[-1])
    #
    #         elif s.type == 'turn':
    #             # Generate the points over the circle with 1.0 radius
    #             # # Vector (0,1)
    #             # start = array([cos(radians(90.0)), sin(radians(90.0))])
    #             # # Compute this using the angle
    #             # end = array([cos(radians(90.0 - s["angle"])), sin(radians(90.0 - s["angle"]))])
    #             start = array([1, 0])
    #
    #             # Make sure that positive is
    #             # TODO Pay attention to left/right positive/negative
    #             end = array([cos(radians(s.angle)), sin(radians(s.angle))])
    #             # Interpolate over 8 points
    #             t_vals = linspace(0, 1, 8)
    #             result = geometric_slerp(start, end, t_vals)
    #             segment = LineString([Point(p[0], p[1]) for p in result])
    #
    #             # Translate that back to origin
    #             segment = translate(segment, -1.0, 0.0)
    #             # Rotate
    #             if s.angle > 0:
    #                 segment = rotate(segment, -90.0, (0.0, 0.0), use_radians=False)
    #             else:
    #                 segment = rotate(segment, +90.0, (0.0, 0.0), use_radians=False)
    #
    #             # Scale to radius on both x and y
    #             segment = scale(segment, s.radius, s.radius, 1.0, (0.0, 0.0))
    #             # Rotate it
    #             segment = rotate(segment, last_rotation, (0, 0))
    #             # Translate it
    #             segment = translate(segment, last_location.x, last_location.y)
    #             # Update last rotation and last location
    #             last_rotation = last_rotation + s.angle  # Straight segments do not change the rotation
    #             last_location = Point(list(segment.coords)[-1])
    #
    #         if segment is not None:
    #             len_coor.append(len(list(segment.coords)))
    #             trajectory_points.extend([Point(x, y) for x, y in list(segment.coords)])
    #
    #     the_trajectory = LineString(common.remove_duplicates([(p.x, p.y) for p in trajectory_points]))
    #
    #     # Make sure we use as reference the NORTH
    #     the_trajectory = translate(the_trajectory, - self.initial_location.x, - self.initial_location.y)
    #     # Rotate by -90 deg
    #     the_trajectory = rotate(the_trajectory, +90.0, (0, 0))
    #     # Translate it back
    #     the_trajectory = translate(the_trajectory, + self.initial_location.x, + self.initial_location.y)
    #
    #     # Interpolate and resample uniformly - Make sure no duplicates are there. Hopefully we do not change the order
    #     # TODO Sampling unit is 5 meters for the moment. Can be changed later
    #     interpolated_points = common.interpolate([(p[0], p[1]) for p in list(the_trajectory.coords)],
    #                                              sampling_unit=SAMPLING_UNIT)
    #
    #     # Concat the speed to the point
    #     trajectory_points = list(the_trajectory.coords)
    #     start = 0
    #     sls = []
    #     sl_coor = []
    #     for s in len_coor:
    #         sl_coor.append([start, start + s])
    #         start = sl_coor[-1][1] - 1
    #     for s in sl_coor:
    #         sls.append(LineString(trajectory_points[s[0]:s[1]]))
    #     speeds = []
    #     for speed in self.movement.get_speeds():
    #         speeds.append(speed)
    #
    #     trajectory_points = []
    #     for line, speed in zip(sls, speeds):
    #         for p in interpolated_points:
    #             point = Point(p[0], p[1])
    #             if point.distance(line) < 0.5 and (p[0], p[1], speed) not in trajectory_points:
    #                 trajectory_points.append((p[0], p[1], speed))
    #
    #     # Return triplet
    #     return trajectory_points

    def get_speed(self):
        return self.movement.get_mean_speed()

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
