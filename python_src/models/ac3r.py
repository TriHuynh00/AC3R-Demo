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


class Road:
    @staticmethod
    def from_dict(road_dict):
        trajectory_points = []
        for node in road_dict["road_node_list"]:
            trajectory_points.append((node[0], node[1], node[3]))

        return Road(road_dict["name"], trajectory_points)

    def __init__(self, name, trajectory_points):
        self.name = name
        self.trajectory_points = trajectory_points

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)


class CrashScenario:
    @staticmethod
    def from_json(ac3r_json_data):
        vehicles, roads = [], []
        for vehicle_dict in ac3r_json_data["vehicles"]:
            vehicles.append(Vehicle.from_dict(vehicle_dict))
        for road_dict in ac3r_json_data["roads"]:
            roads.append(Road.from_dict(road_dict))
        return CrashScenario(ac3r_json_data["name"], vehicles, roads)

    def __init__(self, name, vehicles, roads):
        self.name = name
        self.vehicles = vehicles
        self.roads = roads

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
