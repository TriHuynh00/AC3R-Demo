import json
import numpy
import libs
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd
import time

from evolution import LogBook, RandomEvolution, OpoEvolution, Mutator, Fitness, Generator, Selector
from models import SimulationFactory, Simulation, SimulationScore
from models.ac3rp import CrashScenario


class Experiment:
    def __init__(self, file_path: str, simulation_name: str = None):
        try:
            tmp_simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', time.strftime('%Y-%m-%d--%H-%M-%S', time.localtime()))
            self.simulation_name = tmp_simulation_name if simulation_name is None else simulation_name
            with open(file_path) as file:
                self.scenario = json.load(file)
        except Exception as ex:
            print(f'Scenario is not found. Exception: {ex}')

    def run(self):
        if self.scenario is None:
            print(f'Scenario is not found')
            return False

        # Seeds
        # numpy.random.seed(64)
        # Calculate the expected score
        sim_factory: SimulationFactory = SimulationFactory(CrashScenario.from_json(self.scenario))
        simulation: Simulation = Simulation(sim_factory=sim_factory)
        expected_score: float = SimulationScore(simulation).get_expected_score()
        logbook: LogBook = LogBook(score=expected_score)

        # Write data file
        rev_logfile = open("data/random/" + self.simulation_name + ".csv", "a")
        rev_logfile.write("v1,v2,score\n")

        # Experiment run
        rev = RandomEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            # fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            select=Selector.by_fitness_value,
            # select_aggregate=numpy.mean,
            epochs=3,
            logfile=rev_logfile
        )
        oev = OpoEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            # fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            mutate=Mutator.by_speed,
            mutate_params={"mean": 2.1, "std": 1, "min": 10, "max": 50},
            select=Selector.by_fitness_value,
            # select_aggregate=libs._VD_A,
            epochs=30
        )
        rev.run()
        # oev.run()

        # Close logfiles
        rev_logfile.close()

        # Provide plot chart
        # logbook.visualize(rev.logbook, oev.logbook, "Random", "OPO")

    def visualize(self):
        root_path = "data/rov/"
        case_paths = ["c0/", "c1/", "c2/", "c3/", "c4/", "c5/", "c6/"]
        # case_paths = ["c6/"]
        for case in case_paths:
            df = pd.DataFrame()
            case_path = root_path + case
            for file in ["a.csv", "b.csv", "c.csv", "d.csv", "e.csv"]:
                data = pd.read_csv(case_path + file)
                a = pd.DataFrame({'group': numpy.repeat(file[0].upper(), 30), 'value': data["score"].to_numpy()})
                df = df.append(a)

            plt.figure(figsize=(8, 6))
            # boxplot
            ax = sns.boxplot(x='group', y='value', data=df)
            # add stripplot
            ax = sns.stripplot(x='group', y='value', data=df, color="orange", jitter=0.2, size=5)
            # add maximum
            plt.plot(numpy.repeat(1.7, 5), label="Maximum", linewidth=1, color='#66af78')
            plt.plot(numpy.repeat(1.2, 5), label="Original", linewidth=1, color='#c6595d')

            # add title
            plt.title(f'Boxplot of Case {case[1]} by Different Repetition')
            plt.xlabel('Repetition')
            plt.ylabel('Fitness Score')

            # show the graph
            plt.legend()
            plt.show()