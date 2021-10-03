from shapely.geometry import LineString
from models.ac3rp import common


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
        dist_x, dist_y = 0, 0
        road_width = max([t[3] for t in road_dict["road_node_list"]])
        road_nodes = common.interpolate(road_dict["road_node_list"])
        road_lst = LineString([(t[0] + dist_x, t[1] + dist_y) for t in road_nodes])
        first, last = road_lst.boundary
        return Road(
            road_dict["name"],
            road_dict["road_type"],
            road_dict["road_shape"],
            road_nodes,
            road_width,
            road_poly=road_lst.buffer(road_width, cap_style=2, join_style=2),
            road_line_equation=common.cal_equation_line_two_points(first, last)
        )

    def __init__(self, name, road_type, road_shape, road_nodes, road_width, road_poly, road_line_equation):
        self.name = name
        self.road_type = road_type
        self.road_shape = road_shape
        self.road_nodes = road_nodes
        self.road_width = road_width
        self.road_poly = road_poly
        self.road_line_equation = road_line_equation
