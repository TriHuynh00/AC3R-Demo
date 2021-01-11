import matplotlib.pyplot as plt
import matplotlib.colors as colors
import numpy as np
import math


def pairs(lst):
    for i in range(1, len(lst)):
        yield lst[i - 1], lst[i]


def pair_start_end(lst):
    yield lst[-1], lst[0]


def segment_step(obj):
    segment = obj[0]
    step = obj[0][-1] - obj[0][-2]
    return segment, step


def extra_segment(segment, step, num_points, extra_points):
    if step != 0:
        return np.arange(segment[0], segment[-1] + (extra_points + 1) * step, step)
    else:
        # Same x or y
        return np.full((num_points + extra_points,), segment[0])


class RoadProfiler:
    def __init__(self, color=None):
        self = self
        self.color = colors.to_rgba(list(map(float, color.split())))
        self.points = []
        self.spheres = []
        self.sphere_colors = []
        self.max_distance = 0

    def _compute_road_length(self, point_a, point_b):
        return math.sqrt((point_b[0] - point_a[0]) ** 2 + (point_b[1] - point_a[1]) ** 2)

    def _draw_plot(self, x, y):
        # plotting the points
        plt.plot(x, y)

        # naming the x axis
        plt.xlabel('x - axis')
        # naming the y axis
        plt.ylabel('y - axis')

        # function to show the plot
        plt.scatter(x, y, c='red')
        plt.show()

    def compute_ai_script(self, vehicle_nodes, speed):
        segment_x = [segment[0] for segment in vehicle_nodes]
        segment_y = [segment[1] for segment in vehicle_nodes]

        segment_times = [0]
        for segments in pairs(vehicle_nodes):
            segment_length = self._compute_road_length(segments[0], segments[1])
            if segment_length == 0: continue
            segment_speed = speed / 3.6
            segment_time = segment_length / segment_speed
            # print(segment_length, segment_speed, segment_time)
            segment_times.append(segment_time + segment_times[-1])

        # Find the maximum length of the generated road
        for segments in pair_start_end(vehicle_nodes):
            self.max_distance = self._compute_road_length(segments[0], segments[1])

        script = list()
        for x, y, t in zip(segment_x, segment_y, segment_times):
            node = {
                'x': x,
                'y': y,
                'z': 0,
                't': t
            }
            script.append(node)
            self.points.append([node['x'], node['y'], node['z']])
            self.spheres.append([node['x'], node['y'], node['z'], 0.25])
            self.sphere_colors.append(self.color)

        return script