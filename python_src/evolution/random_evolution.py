import time
import numpy as np
from deap import tools, creator, base

FIRST = 0


class RandomEvolution:
    def __init__(self, orig_ind, fitness, generate, generate_params, select, timeout=None, fitness_repetitions=1, select_aggregate=None):
        creator.create("FitnessMax", base.Fitness, weights=(1.0,))
        creator.create("Individual", list, fitness=creator.FitnessMax)

        self.toolbox = base.Toolbox()
        # Attribute generator
        self.toolbox.register("random_ind", generate, orig_ind, generate_params)
        # Structure initializers
        self.toolbox.register("individual", tools.initRepeat, creator.Individual, self.toolbox.random_ind, 1)
        self.toolbox.register("population", tools.initRepeat, list, self.toolbox.individual)

        self.toolbox.register("evaluate", fitness, fitness_repetitions)
        self.toolbox.register("select", select, select_aggregate)

        stats_fit = tools.Statistics(key=lambda ind: ind.fitness.values)
        stats_size = tools.Statistics(key=len)
        self.mstats = tools.MultiStatistics(fitness=stats_fit, size=stats_size)
        self.mstats.register("avg", np.mean)
        self.mstats.register("std", np.std)
        self.mstats.register("min", np.min)
        self.mstats.register("max", np.max)
        self.logbook = tools.Logbook()

        self.timeout = timeout
        self.orig_ind = orig_ind
        self.fitness_repetitions = fitness_repetitions

    def run(self):
        pop = self.toolbox.population(n=1)
        pop[FIRST][FIRST] = self.orig_ind
        # Evaluate the entire population
        print("Initial Random evaluation")
        start_time = time.time()
        fitnesses = list(map(self.toolbox.evaluate, pop))
        for ind, fit in zip(pop, fitnesses):
            ind.fitness.values = fit
        print("Evaluation time: ", time.time() - start_time)

        best_ind = tools.selBest(pop, 1)[FIRST]
        record = self.mstats.compile(pop)
        self.logbook.record(gen=0, evals=0, **record)

        # Begin the evolution
        print("Start of evolution")
        start_time = time.time()
        timeout = time.time() + 60
        if self.timeout is not None:
            timeout = time.time() + self.timeout

        epochs = 0
        while time.time() < timeout:
            epochs = epochs + 1
            # A new generation
            pop = self.toolbox.population(n=1)

            # Evaluate the entire population
            print("Epoch: ", str(epochs))
            print("Compare 2 scenarios: ")
            s1 = best_ind[0]
            s2 = pop[0][0]
            print("Best ind: ", s1.vehicles[0].get_speed()[0], s1.vehicles[1].get_speed()[0])
            print("Mutant: ", s2.vehicles[0].get_speed()[0], s2.vehicles[1].get_speed()[0])

            fitnesses = list(map(self.toolbox.evaluate, pop))
            for ind, fit in zip(pop, fitnesses):
                ind.fitness.values = fit

            # Select the next generation individuals
            print("Compare 2 fitness score scenarios: ")
            print("Best ind: ", best_ind.fitness.values)
            print("Mutant: ", pop[0].fitness.values)

            best_ind = self.toolbox.select(best_ind, pop)
            pop[:] = [best_ind]
            record = self.mstats.compile(pop)
            self.logbook.record(gen=epochs, evals=epochs, **record)
            print("-------------------")

        print("Evolution time: ", time.time() - start_time)
        print("End of evolution")