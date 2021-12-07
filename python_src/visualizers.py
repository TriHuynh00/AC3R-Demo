from matplotlib import pyplot as plt
from shapely.geometry import LineString, Polygon
from descartes import PolygonPatch


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
