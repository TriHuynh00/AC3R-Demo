import json
from models import ac3r, ac3rp
from matplotlib import pyplot as plt
from shapely.geometry import LineString
from descartes import PolygonPatch


class Scenario:

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
        dist_x, dist_y = 0, 0
        for i, road in enumerate(ac3rp.CrashScenario.from_json(data).roads):
            road_nodes = road.road_nodes
            road_poly = LineString([(t[0] + dist_x, t[1] + dist_y) for t in road_nodes]).buffer(road.road_width,
                                                                                                cap_style=2,
                                                                                                join_style=2)
            road_patch = PolygonPatch(road_poly, fc='gray', ec='dimgray')  # ec='#555555', alpha=0.5, zorder=4)
            plt.gca().add_patch(road_patch)
            xs = [p[0] + dist_x for p in road_nodes]
            ys = [p[1] + dist_y for p in road_nodes]
            plt.plot(xs, ys, '-', color="#9c9c9c")
        colors = ["#ffdab9", "#b1c3de"]
        fig = plt.gcf()
        for i, vehicle in enumerate(ac3r_scenario.vehicles):
            trajectory_points = vehicle.trajectory_points
            xs = [p[0] for p in trajectory_points]
            ys = [p[1] for p in trajectory_points]
            plt.plot(xs, ys, 'o-', label=vehicle.name, color=colors[i])
        plt.legend()
        plt.gca().set_aspect('equal')
        plt.xlim([-100, 100])
        plt.ylim([-100, 100])
        plt.title(f'AC3R {ac3r_scenario.name}')
        plt.show()
        fig.savefig(f'data/{ac3r_scenario.name}_ac3r.png', bbox_inches="tight")

    @staticmethod
    def plot_ac3rp(scenario_file):
        with open(scenario_file) as file:
            data = json.load(file)
        ac3rp_scenario = ac3rp.CrashScenario.from_json(data)
        colors = ["#ff8c00", "#4069e1"]
        fig = plt.gcf()
        for i, v in enumerate(ac3rp_scenario.vehicles):
            trajectory_points = v.generate_trajectory()
            xs = [p[0] for p in trajectory_points]
            ys = [p[1] for p in trajectory_points]
            plt.plot(xs, ys, 'o-', label=v.name, color=colors[i])
        plt.legend()
        plt.gca().set_aspect('equal')
        plt.xlim([-100, 100])
        plt.ylim([-100, 100])
        plt.title(f'AC3R Plus {ac3rp_scenario.name}')
        plt.show()
        fig.savefig(f'data/{ac3rp_scenario.name}_ac3rp.png', bbox_inches="tight")
