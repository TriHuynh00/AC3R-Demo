from models.ac3rp import segment
from shapely.geometry import Point
import numpy


class Movement:
    """
    The Movement class declares the interface that capture all driving action of vehicle.

    Args:
        driving_actions (list): e.g.
            [
                {'name': 'follow', 'trajectory': [[[55, 77, 0.0], [-2.0, 2.0, 0.0]]], 'speed': 20},
                {'name': 'follow', 'trajectory': [[[99, 11, 0.0], [-2.0, 2.0, 0.0]]], 'speed': 20}
            ]
    """

    def __init__(self, driving_actions):
        self.driving_actions = driving_actions

    def translate_to_segments(self):
        """
        Translate list of driving actions to suitable segment dictionary
        which serves the trajectory generation in AC3RPlus

        Return: driving_actions (list)
        """
        driving_actions = []
        for driving_action_dict in self.driving_actions:
            trajectory_segments = []
            # Iterate all the trajectory_list, i.e., list of points that define the segments
            for trajectory_list in driving_action_dict["trajectory"]:
                if len(trajectory_list) == 1:
                    trajectory_segments.append(segment.Parking(
                        Point(trajectory_list[0][0], trajectory_list[0][1])
                    ))
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
        return driving_actions

    def get_driving_actions(self):
        """
        Translate list of driving actions to list of trajectory with its coordinates

        Return: segments (list) e.g:
            [
                [(59, 74), (13, 8)], # straight
                [(13, 8), (2, -0)],  # straight
                [(2, 17), (2, 2), (-30, -49)]  # curve
            ]
        """
        return [driving_action["trajectory"][0] for driving_action in self.driving_actions]

    def get_driving_points(self):
        """
        Translate list of driving actions to list of continuous points

        Return: segments (list) e.g:
            [
                [(59, 74), (13, 8)] # straight, (13, 8), (2, -0) # straight, (2, 17), (2, 2), (-30, -49) # curve]
            ]
        """
        segments = list()
        for s in self.get_driving_actions():
            segments.extend(s)
        return segments

    def set_driving_actions(self, driving_actions):
        """
        Replace the current the list of trajectory with new points
        """
        target = driving_actions.copy()
        for driving_action in self.driving_actions:
            driving_action["trajectory"][0] = [list(t) for t in target[:len(driving_action["trajectory"][0])]]
            target = target[len(driving_action["trajectory"][0]):]

    def set_speed(self, speed):
        """
        Replace the current speed the list of trajectory with new speed value
        """
        for action in self.driving_actions:
            action["speed"] = speed

    def get_mean_speed(self):
        """
        Return the average speed of trajectories
        """
        return numpy.mean([i["speed"] for i in self.driving_actions])

    def get_speeds(self):
        """
        Return the list of speed from different driving action
        """
        return [i["speed"] for i in self.driving_actions]

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
