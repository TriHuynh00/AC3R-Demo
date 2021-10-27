import time
import numpy as np
from deap import tools, creator, base
from models.ac3rp import CrashScenario

FIRST = 0


class OpoEvolution:
    def __init__(self, scenario, fitness, generate, generate_params, select, mutate, mutate_params,
                 logfile, threshold=None, epochs=1, fitness_repetitions=1, select_aggregate=None, log_data_file=None):
        creator.create("FitnessMax", base.Fitness, weights=(1.0,))
        creator.create("Individual", list, fitness=creator.FitnessMax)

        self.toolbox = base.Toolbox()
        # Attribute generator
        self.toolbox.register("random_ind", generate, scenario, generate_params)
        # Structure initializers
        self.toolbox.register("individual", tools.initRepeat, creator.Individual, self.toolbox.random_ind, 1)
        self.toolbox.register("population", tools.initRepeat, list, self.toolbox.individual)
        self.toolbox.register("mutate", mutate, mutate_params)
        self.toolbox.register("evaluate", fitness, fitness_repetitions, log_data_file)
        self.toolbox.register("select", select, select_aggregate)

        stats_fit = tools.Statistics(key=lambda ind: ind.fitness.values)
        stats_size = tools.Statistics(key=len)
        self.mstats = tools.MultiStatistics(fitness=stats_fit, size=stats_size)
        self.mstats.register("avg", np.mean)
        self.mstats.register("std", np.std)
        self.mstats.register("min", np.min)
        self.mstats.register("max", np.max)
        self.logbook = tools.Logbook()

        self.epochs = epochs
        self.orig_ind = scenario
        self.logfile = logfile
        self.threshold = threshold

    def run(self):
        # Random generate first individual and 
        # replace it by the original crash scenario        
        pop = self.toolbox.population(n=1)
        pop[FIRST][FIRST] = self.orig_ind

        start_time = time.time()
        fitnesses = list(map(self.toolbox.evaluate, pop))
        for ind, fit in zip(pop, fitnesses):
            ind.fitness.values = fit

        # Write original scenario
        self.logfile.write(f'{pop[FIRST][FIRST].vehicles[0].get_speed()},'
                           f'{pop[FIRST][FIRST].vehicles[1].get_speed()},'
                           f'{pop[FIRST].fitness.values[0]}\n')

        # Evaluate the entire population
        print("Initial OpO evaluation")
        print("Evaluation time: ", time.time() - start_time)
        ##############################################################################

        best_ind = tools.selBest(pop, 1)[FIRST]
        record = self.mstats.compile(pop)
        self.logbook.record(gen=0, evals=0, **record)

        # Begin the evolution
        print("Start of evolution")

        epoch = 1
        is_exceed_threshold = False
        while epoch <= self.epochs and is_exceed_threshold is False:
            # A new generation - by mutate the best individual
            mutant = self.toolbox.mutate(best_ind)
            pop[:] = [mutant]

            # Calculate the fitness score for the new individual
            fitnesses = list(map(self.toolbox.evaluate, pop))
            for ind, fit in zip(pop, fitnesses):
                ind.fitness.values = fit

            # DEBUG - Compare 2 scenarios
            print("-----------------------------------------------------------------------------------------------")
            print("Epoch: ", str(epoch))
            print("We have 2 scenarios: ")
            s1: CrashScenario = best_ind[0]
            s2: CrashScenario = pop[0][0]
            print(f'Last Ind: '
                  f'(Speed v1-{s1.vehicles[0].get_speed()}) '
                  f'(Speed v2-{s1.vehicles[1].get_speed()}) '
                  f'(Fitness Value-{best_ind.fitness.values[0]})')
            print(f'New Ind: '
                  f'(Speed v1-{s2.vehicles[0].get_speed()}) '
                  f'(Speed v2-{s2.vehicles[1].get_speed()}) '
                  f'(Fitness Value-{pop[0].fitness.values[0]})')
            ##############################################################################

            best_ind = self.toolbox.select(best_ind, pop)
            pop[:] = [best_ind]
            record = self.mstats.compile(pop)
            self.logbook.record(gen=epoch, evals=epoch, **record)
            epoch = epoch + 1

            # Check if the best individual exceeds given threshold
            if self.threshold is not None and self.threshold <= best_ind.fitness.values[0]:
                is_exceed_threshold = True

            # DEBUG - Compare 2 scenarios
            print("We select the best scenario: ")
            s: CrashScenario = best_ind[0]
            print(f'Best Ind: '
                  f'(Speed v1-{s.vehicles[0].get_speed()}) '
                  f'(Speed v2-{s.vehicles[1].get_speed()}) '
                  f'(Fitness Value-{best_ind.fitness.values[0]})')
            if is_exceed_threshold:
                print("Search Algorithm is stopped by threshold!")
            print("-----------------------------------------------------------------------------------------------")
            ##############################################################################

            # Write to logfile
            self.logfile.write(f'{s.vehicles[0].get_speed()},'
                               f'{s.vehicles[1].get_speed()},'
                               f'{best_ind.fitness.values[0]}\n')

        print("Evolution time: ", time.time() - start_time)
        print("End of evolution")
