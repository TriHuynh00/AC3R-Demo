from models.ac3rp import segment
from shapely.geometry import Point
import numpy


class Movement:
    """
    The Movement class declares the interface that capture all driving action of vehicle.

    Args:
        driving_actions (tuple): (x,y,z) tuple specifying the position of the vehicle.
    """

    def __init__(self, driving_actions):
        self.driving_actions = driving_actions

    def translate_to_segments(self):
        driving_actions = []
        for driving_action_dict in self.driving_actions:
            trajectory_segments = []
            # Iterate all the trajectory_list, i.e., list of poitns that define the segments
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

    def get_speed(self):
        speeds = [i["speed"] for i in self.driving_actions]
        return numpy.mean(speeds)

    def get_speeds(self):
        return [i["speed"] for i in self.driving_actions]

    def get_driving_actions(self):
        segments = list()
        for driving_action in self.driving_actions:
            segments.append(driving_action["trajectory"][0])
        return segments

    def set_driving_actions(self, driving_actions):
        target = driving_actions.copy()
        for driving_action in self.driving_actions:
            driving_action["trajectory"][0] = target[:len(driving_action["trajectory"][0])]
            target = target[len(driving_action["trajectory"][0]):]
