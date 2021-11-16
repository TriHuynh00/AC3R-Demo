import json
from models import ac3r, ac3rp
from matplotlib import pyplot as plt
import matplotlib.patches as patches
from shapely.geometry import LineString, Polygon
from shapely.affinity import translate, rotate
from descartes import PolygonPatch
from math import atan2, pi, degrees
import numpy as np
import seaborn as sns
import pandas as pd

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


class ScenarioVisualizer:
    def __init__(self, case, ylim=[5, 5]):
        self.case = case
        self.ylim = ylim
        self.df_rand_m1, self.df_opo_m1, self.df_rand_m2, self.df_opo_m2, self.df_rand_opo_m1, self.df_rand_opo_m2 = pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame()

    def process_individual(self, path, col_name):
        df = pd.read_csv(path, usecols=["score"])
        df = df.rename({"score": col_name}, axis=1)
        df = df.append([[] for _ in range(31 - len(df.index))], ignore_index=True)
        latest_score = 0
        for val in df[col_name]:
            if not np.isnan(val):
                latest_score = val

        df[col_name].fillna(latest_score, inplace=True)
        return df

    def preprocess_df(self, algorithm, mutator):
        df = pd.DataFrame()
        dfs = []
        for i in np.arange(start=1, stop=11, step=1):
            df_tmp = self.process_individual(f'data/{self.case}/{mutator}_{algorithm}_{self.case}_{i}.csv',
                                             f'score_{i}')
            dfs.append(df_tmp)
        df = pd.concat(dfs, axis=1)
        df[algorithm] = df.mean(numeric_only=True, axis=1)
        df["i"] = np.arange(start=0, stop=31, step=1)
        return df

    def transform_df_boxplot(self, original_df):
        df = original_df.copy()
        dfn = pd.DataFrame([], columns=["Epoch", "Score"])
        for row in range(31):
            for col in range(10):
                dfn = dfn.append({'Epoch': row, "Score": df.iloc[row, col]}, ignore_index=True)
        return dfn

    def generate_dfs(self):
        self.df_rand_m1 = self.preprocess_df("Random", "Single")
        self.df_opo_m1 = self.preprocess_df("OpO", "Single")
        self.df_rand_m2 = self.preprocess_df("Random", "Multi")
        self.df_opo_m2 = self.preprocess_df("OpO", "Multi")

        self.df_rand_opo_m1 = pd.DataFrame()
        self.df_rand_opo_m1["i"] = self.df_rand_m1["i"]
        self.df_rand_opo_m1["Random"] = self.df_rand_m1["Random"]
        self.df_rand_opo_m1["OpO"] = self.df_opo_m1["OpO"]

        self.df_rand_opo_m2 = pd.DataFrame()
        self.df_rand_opo_m2["i"] = self.df_rand_m2["i"]
        self.df_rand_opo_m2["Random"] = self.df_rand_m2["Random"]
        self.df_rand_opo_m2["OpO"] = self.df_opo_m2["OpO"]
        return self.df_rand_m1, self.df_opo_m1, self.df_rand_m2, self.df_opo_m2, self.df_rand_opo_m1, self.df_rand_opo_m2

    def visualize(self):
        df_rand_m1, df_opo_m1, df_rand_m2, df_opo_m2, df_rand_opo_m1, df_rand_opo_m2 = self.generate_dfs()
        fig, ax = plt.subplots(3, 3, figsize=(15, 15))
        axs = []

        ax[0, 0].title.set_text('Single Random')
        ax[0, 0].plot(df_rand_m1["i"], df_rand_m1["Random"], color="steelblue")
        axs.append(ax[0, 0])

        ax[0, 1].title.set_text('Multiple Random')
        ax[0, 1].plot(df_rand_m2["i"], df_rand_m2["Random"], color="orange")
        axs.append(ax[0, 1])

        ax[1, 0].title.set_text('Single OpO')
        ax[1, 0].plot(df_opo_m1["i"], df_opo_m1["OpO"], color="green")
        axs.append(ax[1, 0])

        ax[1, 1].title.set_text('Multiple OpO')
        ax[1, 1].plot(df_opo_m2["i"], df_opo_m2["OpO"], color="red")
        axs.append(ax[1, 1])

        ax[0, 2].title.set_text('Single vs Multiple: Random')
        ax[0, 2].plot(df_rand_opo_m1["i"], df_rand_opo_m1["Random"], label="Single", color="steelblue")
        ax[0, 2].plot(df_rand_opo_m2["i"], df_rand_opo_m2["Random"], label="Multi", color="orange")
        ax[0, 2].legend(loc='lower right')
        axs.append(ax[0, 2])

        ax[1, 2].title.set_text('Single vs Multiple: OpO')
        ax[1, 2].plot(df_rand_opo_m1["i"], df_rand_opo_m1["OpO"], label="Single", color="green")
        ax[1, 2].plot(df_rand_opo_m2["i"], df_rand_opo_m2["OpO"], label="Multi", color="red")
        ax[1, 2].legend(loc='lower right')
        axs.append(ax[1, 2])

        ax[2, 0].title.set_text('Single: Random vs OpO')
        ax[2, 0].plot(df_rand_m1["i"], df_rand_m1["Random"], label="Random", color="steelblue")
        ax[2, 0].plot(df_opo_m1["i"], df_opo_m1["OpO"], label="OpO", color="green")
        ax[2, 0].legend(loc='lower right')
        axs.append(ax[2, 0])

        ax[2, 1].title.set_text('Multiple: Random vs OpO')
        ax[2, 1].plot(df_rand_m2["i"], df_rand_m2["Random"], label="Random", color="orange")
        ax[2, 1].plot(df_opo_m2["i"], df_opo_m2["OpO"], label="OpO", color="red")
        ax[2, 1].legend(loc='lower right')
        axs.append(ax[2, 1])

        ax[2, 2].title.set_text('Random vs OpO')
        ax[2, 2].plot(df_rand_m1["i"], df_rand_m1["Random"], label="S.Rand", color="steelblue")
        ax[2, 2].plot(df_rand_m2["i"], df_rand_m2["Random"], label="M.Rand", color="orange")
        ax[2, 2].plot(df_opo_m1["i"], df_opo_m1["OpO"], label="S.OpO", color="green")
        ax[2, 2].plot(df_opo_m2["i"], df_opo_m2["OpO"], label="M.OpO", color="red")
        ax[2, 2].legend(loc='lower right')
        axs.append(ax[2, 2])

        for ax in axs:
            ax.set_ylim(self.ylim)

        plt.show()
        fig.savefig(f'data/{self.case}/Multiple.png', bbox_inches="tight")

    def visualize_confidence_interval(self):
        def _confidence_interval(i, mean, std, ax=None, color=None):
            x = i
            y = mean
            ci = 0.1 * std / mean
            ax.plot(x, y, color=color)
            ax.fill_between(x, (y - ci), (y + ci), color=color, alpha=0.3)
            return ax

        df_rand_m1, df_opo_m1, df_rand_m2, df_opo_m2, df_rand_opo_m1, df_rand_opo_m2 = self.generate_dfs()
        fig, ax = plt.subplots(2, 2, figsize=(12, 12))
        axs = []

        ax[0, 0].title.set_text('Single Random')
        ax[0, 0] = _confidence_interval(df_rand_m1["i"], df_rand_m1["Random"], df_rand_m1["std"], ax[0, 0],
                                        color="steelblue")
        axs.append(ax[0, 0])

        ax[0, 1].title.set_text('Multiple Random')
        ax[0, 1] = _confidence_interval(df_rand_m2["i"], df_rand_m2["Random"], df_rand_m2["std"], ax[0, 1],
                                        color="orange")
        axs.append(ax[0, 1])

        ax[1, 0].title.set_text('Single OpO')
        ax[1, 0] = _confidence_interval(df_opo_m1["i"], df_opo_m1["OpO"], df_opo_m1["std"], ax[1, 0], color="green")
        axs.append(ax[1, 0])

        ax[1, 1].title.set_text('Multiple OpO')
        ax[1, 1] = _confidence_interval(df_opo_m2["i"], df_opo_m2["OpO"], df_opo_m2["std"], ax[1, 1], color="red")
        axs.append(ax[1, 1])

        for ax in axs:
            ax.set_ylim(self.ylim)
            ax.xaxis.label.set_visible(False)
            ax.yaxis.label.set_visible(False)

        plt.show()
        fig.savefig(f'data/{self.case}/CI.png', bbox_inches="tight")

    def visualize_box_plot(self):
        df_rand_m1, df_opo_m1, df_rand_m2, df_opo_m2, df_rand_opo_m1, df_rand_opo_m2 = self.generate_dfs()

        df_rand_m1 = self.transform_df_boxplot(df_rand_m1)
        df_opo_m1 = self.transform_df_boxplot(df_opo_m1)
        df_rand_m2 = self.transform_df_boxplot(df_rand_m2)
        df_opo_m2 = self.transform_df_boxplot(df_opo_m2)

        fig, ax = plt.subplots(4, 1, figsize=(16, 24))
        axs = []

        ax[0].title.set_text('Single Random')
        sns.violinplot(x='Epoch', y='Score', data=df_rand_m1, color="0.95", ax=ax[0])
        sns.swarmplot(x='Epoch', y='Score', data=df_rand_m1, color="k", ax=ax[0])
        axs.append(ax[0])

        ax[1].title.set_text('Multiple Random')
        sns.violinplot(x='Epoch', y='Score', data=df_rand_m2, color="0.95", ax=ax[1])
        sns.swarmplot(x='Epoch', y='Score', data=df_rand_m2, color="k", ax=ax[1])
        axs.append(ax[1])

        ax[2].title.set_text('Single OpO')
        sns.violinplot(x='Epoch', y='Score', data=df_opo_m1, color="0.95", ax=ax[2])
        sns.swarmplot(x='Epoch', y='Score', data=df_opo_m1, color="k", ax=ax[2])
        axs.append(ax[2])

        ax[3].title.set_text('Multiple OpO')
        sns.violinplot(x='Epoch', y='Score', data=df_opo_m2, color="0.95", ax=ax[3])
        sns.swarmplot(x='Epoch', y='Score', data=df_opo_m2, color="k", ax=ax[3])
        axs.append(ax[3])

        for ax in axs:
            ax.set_ylim(self.ylim)
            ax.xaxis.label.set_visible(False)
            ax.yaxis.label.set_visible(False)

        plt.show()
        fig.savefig(f'data/{self.case}/BoxPlot.png', bbox_inches="tight")