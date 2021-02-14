import numpy as np
import matplotlib.pyplot as plt
from deap import tools, creator, base
from threading import Thread, Event

# Event object used to send signals from one thread to another
STOP_EVENT = Event()
FIRST = 0


class RandomEvolution:
    def __init__(self, orig_ind, fitness, generate_random_ind, select, mutate, expectations):
        creator.create("FitnessMax", base.Fitness, weights=(1.0,))
        creator.create("Individual", list, fitness=creator.FitnessMax)

        self.toolbox = base.Toolbox()
        # Attribute generator
        self.toolbox.register("random_ind", generate_random_ind, orig_ind, params={"min": 10, "max": 50, "dim": 1})
        # Structure initializers
        self.toolbox.register("individual", tools.initRepeat, creator.Individual, self.toolbox.random_ind, 1)
        self.toolbox.register("population", tools.initRepeat, list, self.toolbox.individual)

        self.toolbox.register("evaluate", fitness)
        self.toolbox.register("mutate", mutate, mutate_params={"std": 0.25, "dim": 1, "min": 10, "max": 50})
        self.toolbox.register("select", select, fitness=fitness)

        stats_fit = tools.Statistics(key=lambda ind: ind.fitness.values)
        stats_size = tools.Statistics(key=len)
        self.mstats = tools.MultiStatistics(fitness=stats_fit, size=stats_size)
        self.mstats.register("avg", np.mean)
        self.mstats.register("std", np.std)
        self.mstats.register("min", np.min)
        self.mstats.register("max", np.max)
        self.logbook = tools.Logbook()

        self.expectations = expectations
        self.orig_ind = orig_ind

    def start_from(self, orig_fitness, timeout):
        pop = self.toolbox.population(n=1)
        # Evaluate the entire population
        fitnesses = list(map(self.toolbox.evaluate, pop))
        for ind, fit in zip(pop, fitnesses):
            ind.fitness.values = fit

        # Extracting all the fitness of
        fits = [ind.fitness.values[0] for ind in pop]

        def _run():
            # Begin the evolution
            epochs = 0
            while True:
                # A new generation
                epochs = epochs + 1
                # Select the next generation individuals
                offspring = self.toolbox.select(self.orig_ind, pop, orig_fitness)
                self.toolbox.mutate(offspring)
                # The population is entirely replaced by the offspring
                pop[:] = [offspring]
                best_ind = tools.selBest(pop, 1)[0]
                record = self.mstats.compile(pop)
                self.logbook.record(gen=epochs, evals=epochs, **record)
                if STOP_EVENT.is_set():
                    break

        # Start the thread evolution within given time
        action_thread = Thread(target=_run)
        action_thread.start()
        action_thread.join(timeout=timeout)
        STOP_EVENT.set()

        print("End of evolution")

    def print_logbook(self):
        self.logbook.header = "gen", "evals", "fitness", "size"
        self.logbook.chapters["fitness"].header = "min", "avg", "max"
        self.logbook.chapters["size"].header = "min", "avg", "max"
        print(self.logbook)

    def visualize_evolution(self):
        gen = self.logbook.select("gen")
        fit_mins = self.logbook.chapters["fitness"].select("min")
        size_avgs = self.logbook.chapters["size"].select("avg")

        fig, ax1 = plt.subplots()
        line1 = ax1.plot(gen, fit_mins, "b-", label="Fitness")
        line3 = ax1.plot(gen, self.expectations(len(gen)), "b--", label="Expectation")
        ax1.set_xlabel("Generation")
        ax1.set_ylabel("Fitness", color="b")
        for tl in ax1.get_yticklabels():
            tl.set_color("b")

        ax2 = ax1.twinx()
        line2 = ax2.plot(gen, size_avgs, "r-", label="Size")
        ax2.set_ylabel("Size", color="r")
        for tl in ax2.get_yticklabels():
            tl.set_color("r")

        lns = line1 + line2 + line3
        # lns = line1
        labs = [l.get_label() for l in lns]
        ax1.legend(lns, labs, loc="lower right")

        plt.show()
