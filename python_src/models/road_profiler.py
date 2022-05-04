import matplotlib.colors as colors
from shapely.geometry import Point


def pairs(lst):
    for i in range(1, len(lst)):
        yield lst[i - 1], lst[i]


class RoadProfiler:
    def __init__(self):
        self.script = []
        self.points = []
        self.spheres = []
        self.sphere_colors = []

    def compute_ai_script(self, trajectory, color=None):
        segment_times = [0]
        segment_x = [p[0] for p in trajectory]
        segment_y = [p[1] for p in trajectory]

        for segment in pairs(trajectory):
            p1, p2 = segment[0], segment[1]
            length = Point(p1[0], p1[1]).distance(Point(p2[0], p2[1]))
            if length == 0:
                continue
            speed = p1[2]
            time = length / speed
            segment_times.append(time + segment_times[-1])

        for x, y, t in zip(segment_x, segment_y, segment_times):
            node = {
                'x': x,
                'y': y,
                'z': 0,
                't': t
            }
            self.script.append(node)
            self.points.append([node['x'], node['y'], node['z']])
            self.spheres.append([node['x'], node['y'], node['z'], 0.25])
            self.sphere_colors.append(color)

