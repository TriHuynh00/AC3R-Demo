# libraries
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import pandas as pd
import json
from models.ac3rp import CrashScenario
from models import SimulationFactory, Simulation, SimulationScore
from .mutation import Mutation


class ExperimentVisualizer:
    def __init__(self, file_path, ylim=None, bp_ylim=None):
        self.ylim = [5, 5] if ylim is None else ylim
        self.bp_ylim = [5, 5] if bp_ylim is None else bp_ylim
        self.df_rand_m1, self.df_opo_m1, self.df_rand_m2, self.df_opo_m2, self.df_rand_opo_m1, self.df_rand_opo_m2 = pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame()
        with open(file_path) as file:
            scenario = json.load(file)
        crash_scenario = CrashScenario.from_json(scenario)
        sim_factory = SimulationFactory(crash_scenario)
        simulation = Simulation(sim_factory=sim_factory)
        self.target = SimulationScore(simulation).get_expected_score()
        self.case = crash_scenario.name

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
            df_tmp = self.process_individual(f'data/{self.case}/{mutator}_{algorithm}_{i}.csv', f'score_{i}')
            dfs.append(df_tmp)
        df = pd.concat(dfs, axis=1)
        df[algorithm] = df.mean(numeric_only=True, axis=1)
        df["std"] = df.std(numeric_only=True, axis=1)
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
        def _confidence_interval(i, mean, std, ax=None, color=None):
            x = i
            y = mean
            ci = 0.1 * std / mean
            ax.plot(x, y, color=color)
            ax.fill_between(x, (y - ci), (y + ci), color=color, alpha=0.3)
            return ax

        df_rand_m1, df_opo_m1, df_rand_m2, df_opo_m2, df_rand_opo_m1, df_rand_opo_m2 = self.generate_dfs()
        d_mutators = [
            Mutation(xs=df_rand_m1["i"], ys=df_rand_m1["Random"], method="simp", color="steelblue",
                     name="S.Rand", short_name="Single", family="Random"),
            Mutation(xs=df_rand_m2["i"], ys=df_rand_m2["Random"], method="simp", color="orange",
                     name="M.Rand", short_name="Multiple", family="Random"),
            Mutation(xs=df_opo_m1["i"], ys=df_opo_m1["OpO"], method="simp", color="green", name="S.OpO",
                     short_name="Single", family="OpO"),
            Mutation(xs=df_opo_m2["i"], ys=df_opo_m2["OpO"], method="simp", color="red", name="M.OpO",
                     short_name="Multiple", family="OpO"),
        ]

        d_mutators = sorted(d_mutators, key=lambda x: x.auc, reverse=True)
        fig, ax = plt.subplots(3, 3, figsize=(15, 15))
        axs = []

        ax[0, 0].title.set_text('Single Random')
        ax[0, 0] = _confidence_interval(df_rand_m1["i"], df_rand_m1["Random"], df_rand_m1["std"], ax[0, 0],
                                        color="steelblue")
        ax[0, 0].plot(df_rand_m1["i"], [self.target for x in df_rand_m1["i"]], label="Single", color="#0d1487")
        axs.append(ax[0, 0])

        ax[0, 1].title.set_text('Multiple Random')
        ax[0, 1] = _confidence_interval(df_rand_m2["i"], df_rand_m2["Random"], df_rand_m2["std"], ax[0, 1],
                                        color="orange")
        ax[0, 1].plot(df_rand_m1["i"], [self.target for x in df_rand_m1["i"]], label="Single", color="#0d1487")
        axs.append(ax[0, 1])

        ax[1, 0].title.set_text('Single OpO')
        ax[1, 0] = _confidence_interval(df_opo_m1["i"], df_opo_m1["OpO"], df_opo_m1["std"], ax[1, 0], color="green")
        ax[1, 0].plot(df_rand_m1["i"], [self.target for x in df_rand_m1["i"]], label="Single", color="#0d1487")
        axs.append(ax[1, 0])

        ax[1, 1].title.set_text('Multiple OpO')
        ax[1, 1] = _confidence_interval(df_opo_m2["i"], df_opo_m2["OpO"], df_opo_m2["std"], ax[1, 1], color="red")
        ax[1, 1].plot(df_rand_m1["i"], [self.target for x in df_rand_m1["i"]], label="Single", color="#0d1487")
        axs.append(ax[1, 1])

        ax[0, 2].title.set_text('Single vs Multiple: Random')
        for m in [m for m in d_mutators if m.family == "Random"]:
            ax[0, 2].plot(m.xs, m.ys, label=m.get_label(use_short=True), color=m.color)
        ax[0, 2].legend(loc='lower right')
        axs.append(ax[0, 2])

        ax[1, 2].title.set_text('Single vs Multiple: OpO')
        for m in [m for m in d_mutators if m.family == "OpO"]:
            ax[1, 2].plot(m.xs, m.ys, label=m.get_label(use_short=True), color=m.color)
        ax[1, 2].legend(loc='lower right')
        axs.append(ax[1, 2])

        ax[2, 0].title.set_text('Single: Random vs OpO')
        for m in [m for m in d_mutators if m.short_name == "Single"]:
            ax[2, 0].plot(m.xs, m.ys, label=m.get_label(use_family=True), color=m.color)
        ax[2, 0].legend(loc='lower right')
        axs.append(ax[2, 0])

        ax[2, 1].title.set_text('Multiple: Random vs OpO')
        for m in [m for m in d_mutators if m.short_name == "Multiple"]:
            ax[2, 1].plot(m.xs, m.ys, label=m.get_label(use_family=True), color=m.color)
        ax[2, 1].legend(loc='lower right')
        axs.append(ax[2, 1])

        ax[2, 2].title.set_text('Random vs OpO')
        for m in d_mutators:
            ax[2, 2].plot(m.xs, m.ys, label=m.get_label(), color=m.color)
        ax[2, 2].legend(loc='lower right')
        axs.append(ax[2, 2])

        for ax in axs:
            ax.set_ylim(self.ylim)

        plt.show()
        fig.savefig(f'data/{self.case}/Plot - Multiple.png', bbox_inches="tight")

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
            ax.set_ylim(self.bp_ylim)
            ax.xaxis.label.set_visible(False)
            ax.yaxis.label.set_visible(False)

        plt.show()
        fig.savefig(f'data/{self.case}/Plot - BoxPlot.png', bbox_inches="tight")
