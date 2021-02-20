import time
import numpy as np
from deap import tools, creator, base

FIRST = 0


class RandomEvolution:
    def __init__(self, orig_ind, fitness, generate, generate_params, select, timeout=None):
        creator.create("FitnessMax", base.Fitness, weights=(1.0,))
        creator.create("Individual", list, fitness=creator.FitnessMax)

        self.toolbox = base.Toolbox()
        # Attribute generator
        self.toolbox.register("random_ind", generate, orig_ind, generate_params)
        # Structure initializers
        self.toolbox.register("individual", tools.initRepeat, creator.Individual, self.toolbox.random_ind, 1)
        self.toolbox.register("population", tools.initRepeat, list, self.toolbox.individual)

        self.toolbox.register("evaluate", fitness)
        self.toolbox.register("select", select)

        stats_fit = tools.Statistics(key=lambda ind: ind.fitness.values)
        stats_size = tools.Statistics(key=len)
        self.mstats = tools.MultiStatistics(fitness=stats_fit, size=stats_size)
        self.mstats.register("avg", np.mean)
        self.mstats.register("std", np.std)
        self.mstats.register("min", np.min)
        self.mstats.register("max", np.max)
        self.logbook = tools.Logbook()

        self.timeout = timeout

    def run(self):
        pop = self.toolbox.population(n=1)
        # Evaluate the entire population
        print("Start evaluation")
        start_time = time.time()
        fitnesses = list(map(self.toolbox.evaluate, pop))
        for ind, fit in zip(pop, fitnesses):
            ind.fitness.values = fit
        print("Evaluation time: ", time.time() - start_time)
        best_ind = tools.selBest(pop, 1)[FIRST]
        epochs = 0
        # Begin the evolution
        print("Start of evolution")
        start_time = time.time()
        timeout = time.time() + 10 if self.timeout is None else time.time() + self.timeout
        while time.time() < timeout:
            epochs = epochs + 1
            # A new generation
            pop = self.toolbox.population(n=1)
            # Evaluate the entire population
            fitnesses = list(map(self.toolbox.evaluate, pop))
            for ind, fit in zip(pop, fitnesses):
                ind.fitness.values = fit

            # Select the next generation individuals
            best_ind = self.toolbox.select(best_ind, pop)
            pop[:] = [best_ind]
            record = self.mstats.compile(pop)
            self.logbook.record(gen=epochs, evals=epochs, **record)

        print("Evolution time: ", time.time() - start_time)
        print("End of evolution")
