class Vehicle:
    @staticmethod
    def from_dict(vehicle_dict):
        trajectory_points = []
        for driving_action_dict in vehicle_dict["driving_actions"]:
            # Iterate all the trajectory_list, i.e., list of points that define the segments
            for trajectory_list in driving_action_dict["trajectory"]:
                if len(trajectory_list) < 4:
                    for t in trajectory_list:
                        point = (t[0], t[1])
                        if point not in trajectory_points:
                            trajectory_points.append(point)
                else:
                    raise Exception("Too many points in the trajectory_dict")

        return Vehicle(vehicle_dict["name"], trajectory_points)

    def __init__(self, name, trajectory_points):
        self.name = name
        self.trajectory_points = trajectory_points

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)


class CrashScenario:
    @staticmethod
    def from_json(ac3r_json_data):
        vehicles = []
        for vehicle_dict in ac3r_json_data["vehicles"]:
            vehicles.append(Vehicle.from_dict(vehicle_dict))
        return CrashScenario(ac3r_json_data["name"], vehicles)

    def __init__(self, name, vehicles):
        self.name = name
        self.vehicles = vehicles

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
