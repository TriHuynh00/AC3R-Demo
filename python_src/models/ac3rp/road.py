from models.ac3rp.common import interpolate


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
        interpolated_road_points = interpolate(road_dict["road_node_list"])
        return Road(road_dict["name"], road_dict["road_type"], road_dict["road_shape"], interpolated_road_points)

    def __init__(self, name, road_type, road_shape, road_nodes):
        self.name = name
        self.road_type = road_type
        self.road_shape = road_shape
        self.road_nodes = road_nodes
