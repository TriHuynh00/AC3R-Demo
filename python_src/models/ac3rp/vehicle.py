from scipy.spatial.transform import Rotation as R
from numpy import linspace, array, cross, dot
from shapely.geometry import LineString, Point
from shapely.affinity import translate, rotate, scale
from scipy.spatial import geometric_slerp
from math import sin, cos, radians, degrees, atan2, copysign
from models.ac3rp import common
from models.ac3rp import segment

SAMPLING_UNIT = 5


class Vehicle:
    @staticmethod
    def from_dict(vehicle_dict):
        driving_actions = []
        for driving_action_dict in vehicle_dict["driving_actions"]:
            trajectory_segments = []
            # Iterate all the trajectory_list, i.e., list of poitns that define the segments
            for trajectory_list in driving_action_dict["trajectory"]:
                if len(trajectory_list) == 1:
                    trajectory_segments.append(segment.Parking())
                elif len(trajectory_list) == 2:
                    trajectory_segments.append(segment.Straight(
                        Point(trajectory_list[0][0], trajectory_list[0][1]),
                        Point(trajectory_list[1][0], trajectory_list[1][1])
                    ))
                elif len(trajectory_list) == 3:
                    trajectory_segments.append(segment.Turn(
                        Point(trajectory_list[0][0], trajectory_list[0][1]),
                        Point(trajectory_list[1][0], trajectory_list[1][1]),
                        Point(trajectory_list[2][0], trajectory_list[2][1])
                    ))
                else:
                    raise Exception("Too many points in the trajectory_dict")

            # TODO Refactor to class
            driving_actions.append({
                "name": driving_action_dict["name"],
                "trajectory_segments": trajectory_segments,
                "speed": driving_action_dict["speed"]
            })

        # Extract the initial location: the first point of the trajectory
        initial_location, initial_rotation = common.compute_initial_state(vehicle_dict["driving_actions"])

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
            if s.type == "parking":
                segment = LineString([(x, 0) for x in linspace(0, s.length, 2)])
                segment = rotate(segment, last_rotation, (0, 0))
                segment = translate(segment, last_location.x, last_location.y)
                last_rotation = last_rotation  # Parking segments do not change the rotation
                last_location = Point(list(segment.coords)[-1])
            if s.type == 'straight':
                # Create an horizontal line of given length from the origin
                segment = LineString([(x, 0) for x in linspace(0, s.length, 8)])
                # Rotate it
                segment = rotate(segment, last_rotation, (0, 0))
                # Move it
                segment = translate(segment, last_location.x, last_location.y)
                # Update last rotation and last location
                last_rotation = last_rotation  # Straight segments do not change the rotation
                last_location = Point(list(segment.coords)[-1])

            elif s.type == 'turn':
                # Generate the points over the circle with 1.0 radius
                # # Vector (0,1)
                # start = array([cos(radians(90.0)), sin(radians(90.0))])
                # # Compute this using the angle
                # end = array([cos(radians(90.0 - s["angle"])), sin(radians(90.0 - s["angle"]))])
                start = array([1, 0])

                # Make sure that positive is
                # TODO Pay attention to left/right positive/negative
                end = array([cos(radians(s.angle)), sin(radians(s.angle))])
                # Interpolate over 8 points
                t_vals = linspace(0, 1, 8)
                result = geometric_slerp(start, end, t_vals)
                segment = LineString([Point(p[0], p[1]) for p in result])

                # Translate that back to origin
                segment = translate(segment, -1.0, 0.0)
                # Rotate
                if s.angle > 0:
                    segment = rotate(segment, -90.0, (0.0, 0.0), use_radians=False)
                else:
                    segment = rotate(segment, +90.0, (0.0, 0.0), use_radians=False)

                # Scale to radius on both x and y
                segment = scale(segment, s.radius, s.radius, 1.0, (0.0, 0.0))
                # Rotate it
                segment = rotate(segment, last_rotation, (0, 0))
                # Translate it
                segment = translate(segment, last_location.x, last_location.y)
                # Update last rotation and last location
                last_rotation = last_rotation + s.angle  # Straight segments do not change the rotation
                last_location = Point(list(segment.coords)[-1])

            if segment is not None:
                len_coor.append(len(list(segment.coords)))
                trajectory_points.extend([Point(x, y) for x, y in list(segment.coords)])

        the_trajectory = LineString(common.f7([(p.x, p.y) for p in trajectory_points]))

        # Make sure we use as reference the NORTH
        the_trajectory = translate(the_trajectory, - self.initial_location.x, - self.initial_location.y)
        # Rotate by -90 deg
        the_trajectory = rotate(the_trajectory, +90.0, (0, 0))
        # Translate it back
        the_trajectory = translate(the_trajectory, + self.initial_location.x, + self.initial_location.y)

        # Interpolate and resample uniformly - Make sure no duplicates are there. Hopefully we do not change the order
        # TODO Sampling unit is 5 meters for the moment. Can be changed later
        interpolated_points = common.interpolate([(p[0], p[1]) for p in list(the_trajectory.coords)], sampling_unit=SAMPLING_UNIT)

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

    def get_speed(self):
        speed = [i["speed"] for i in self.driving_actions]
        return speed

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
