from shapely.geometry import LineString
from models.ac3rp import common
from typing import List

THICKNESS = {
    "single": 0.2,
    "double": 0.4
}


class Road:
    @staticmethod
    def from_dict(key, road_dict):
        center = road_dict["center"]
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
        road_width = max([t[3] for t in center["points"]])
        road_nodes = common.interpolate(center["points"])
        road_lst = LineString([(t[0] + dist_x, t[1] + dist_y) for t in road_nodes])
        first, last = road_lst.boundary

        left_line = RoadLine(
            name=f'{key}_left',
            nodes=road_dict["left"]["points"],
            pattern=road_dict["left"]["pattern"],
            thickness=road_dict["left"]["num"],
        )
        right_line = RoadLine(
            name=f'{key}_right',
            nodes=road_dict["right"]["points"],
            pattern=road_dict["right"]["pattern"],
            thickness=road_dict["right"]["num"],
        )
        middle_lines = []
        for i, line in enumerate(road_dict["marks"]):
            line = RoadLine(
                name=f'{key}_middle_{i}',
                nodes=line["points"],
                pattern=line["pattern"],
                thickness=line["num"],
            )
            middle_lines.append(line)

        return Road(
            name=key,
            road_nodes=road_nodes,
            road_width=road_width,
            road_poly=road_lst.buffer(road_width, cap_style=2, join_style=2),
            road_line_equation=common.cal_equation_line_two_points(first, last),
            left_line=left_line,
            right_line=right_line,
            middle_lines=middle_lines

        )

    def __init__(self, name, road_nodes, road_width, road_poly, road_line_equation,
                 left_line, right_line, middle_lines):
        self.name = name
        self.road_nodes = road_nodes
        self.road_width = road_width
        self.road_poly = road_poly
        self.road_line_equation = road_line_equation
        self.left_line: RoadLine = left_line
        self.right_line: RoadLine = right_line
        self.middle_lines: List = middle_lines


class RoadLine:
    def __init__(self, name, nodes, pattern, thickness):
        self.name = name
        self.nodes = [(p[0], p[1], 0, THICKNESS.get(thickness, 0.2)) for p in nodes]
        self.pattern = pattern
        self.thickness = thickness
