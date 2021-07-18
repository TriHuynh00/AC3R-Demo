import json
from models import ac3r, ac3rp
from matplotlib import pyplot as plt
import matplotlib.patches as patches
from shapely.geometry import LineString, Polygon
from shapely.affinity import translate, rotate
from descartes import PolygonPatch
from math import atan2, pi, degrees

#
# # https://stackoverflow.com/questions/34764535/why-cant-matplotlib-plot-in-a-different-thread
# class RoadTestVisualizer:
#     """
#         Visualize and Plot RoadTests
#     """
#
#     little_triangle = Polygon([(10, 0), (0, -5), (0, 5), (10, 0)])
#     square = Polygon([(5, 5), (5, -5), (-5, -5), (-5, 5), (5, 5)])
#
#


class RoadVisualizer:

    def __init__(self, the_figure):
        self.the_figure = the_figure

    def visualize(self, road):
        # Be sure we plot on the right figure
        plt.figure(self.the_figure.number)

        # Road Geometry.

        # TODO Assume the road has constant width.
        road_width = road.road_nodes[0][3]
        road_poly = LineString([(t[0], t[1]) for t in road.road_nodes]).buffer(road_width, cap_style=2, join_style=2)
        road_patch = PolygonPatch(road_poly, fc='gray', ec='dimgray')  # ec='#555555', alpha=0.5, zorder=4)
        plt.gca().add_patch(road_patch)

        # Central line
        # TODO Lane/configurations
        sx = [t[0] for t in road.road_nodes]
        sy = [t[1] for t in road.road_nodes]
        plt.plot(sx, sy, 'yellow')

        plt.draw()
        plt.pause(0.001)


class VehicleTrajectoryVisualizer:

    def __init__(self, the_figure):
        self.the_figure = the_figure

    def visualize(self, vehicle):
        # Be sure we plot on the right figure
        plt.figure(self.the_figure.number)

        trajectory_points = vehicle.generate_trajectory()

        xs = [p[0] for p in trajectory_points]
        ys = [p[1] for p in trajectory_points]

        plt.plot(xs, ys, 'o-')

    @staticmethod
    def plot_ac3r(scenario_file):
        with open(scenario_file) as file:
            data = json.load(file)
        ac3r_scenario = ac3r.CrashScenario.from_json(data)
        colors = ["#ffdab9", "#b1c3de"]
        for i, vehicle in enumerate(ac3r_scenario.vehicles):
            trajectory_points = vehicle.trajectory_points
            xs = [p[0] for p in trajectory_points]
            ys = [p[1] for p in trajectory_points]
            plt.plot(xs, ys, 'o-', label=vehicle.name, color=colors[i])
        plt.legend()
        plt.gca().set_aspect('equal')
        # plt.xlim([-100, 100])
        # plt.ylim([-100, 100])
        plt.title(f'AC3R {ac3r_scenario.name}')
        plt.show()

    @staticmethod
    def plot_ac3rp(scenario_file):
        with open(scenario_file) as file:
            data = json.load(file)
        ac3rp_scenario = ac3rp.CrashScenario.from_json(data)
        colors = ["#ff8c00", "#4069e1"]
        for i, v in enumerate(ac3rp_scenario.vehicles):
            trajectory_points = v.generate_trajectory()
            xs = [p[0] for p in trajectory_points]
            ys = [p[1] for p in trajectory_points]
            plt.plot(xs, ys, 'o-', label=v.name, color=colors[i])
        plt.legend()
        plt.gca().set_aspect('equal')
        # plt.xlim([-100, 100])
        # plt.ylim([-100, 100])
        plt.title(f'AC3R Plus {ac3rp_scenario.name}')
        plt.show()


class CrashScenarioVisualizer:

    def __init__(self, map_size):
        self.map_size = map_size
        self.the_figure = None

        # Make sure there's a windows and does not block anything when calling show
        plt.ion()
        plt.show()

        # Setup the figure environment
        self._setup_figure()

    def _setup_figure(self):
        if self.the_figure is None:
            self.the_figure = plt.figure()
        else:
            plt.figure(self.the_figure.number)
            plt.clf()
        plt.gcf().set_title("Scenario Visualizer")
        plt.gca().set_aspect('equal', 'box')
        plt.gca().set(xlim=(-30, self.map_size + 30), ylim=(-30, self.map_size + 30))

    def visualize_scenario(self, crash_scenario):
        self._setup_figure()

        # Visualize the road