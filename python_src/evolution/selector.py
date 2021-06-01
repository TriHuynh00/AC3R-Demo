import numpy as np
import scipy.stats as stats
from models.ac3rp import CrashScenario


class Selector:
    @staticmethod
    def by_fitness_value(null_func, orig_inds, pop_ind):
        deap_inds = pop_ind[0]  # deap_pop is a list

        f1 = orig_inds.fitness.values
        f2 = deap_inds.fitness.values

        if f1 >= f2:
            return orig_inds
        return deap_inds  # return deap_individual

    @staticmethod
    def by_aggregate_f(aggregate_func, orig_inds, pop_ind):
        deap_inds = pop_ind[0]  # deap_pop is a list
        if aggregate_func is None:
            aggregate_func = np.mean

        orig_ind: CrashScenario = orig_inds[0]
        deap_ind: CrashScenario = deap_inds[0]
        f1 = aggregate_func(orig_ind.scores)
        f2 = aggregate_func(deap_ind.scores)

        if f1 >= f2:
            return orig_inds
        return deap_inds  # return deap_individual

    @staticmethod
    def by_vda_f(vda_func, orig_inds, pop_ind):
        deap_inds = pop_ind[0]

        orig_ind: CrashScenario = orig_inds[0]
        deap_ind: CrashScenario = deap_inds[0]
        f1 = orig_ind.scores
        f2 = deap_ind.scores

        # Calculate the p-value to know if two populations are different
        alpha = 0.05  # significance level to reject H0
        stat, p = stats.mannwhitneyu(f1, f2)
        print('Test Statistics=%.3f, p=%.3f' % (stat, p))
        if p > alpha:
            print('H0: The two populations are equal versus.')
            return orig_inds
        else:
            print('H1: The two populations are different.')
            print("Compare: ")
            print(f'orig_inds: {f1}')
            print(f'deap_inds: {f2}')
            # To understand how different they were (the effect size)
            # we used VDA
            if vda_func(f1, f2) > 0.5:
                print(f'orig_inds wins: {f1}')
                return orig_inds
            print(f'deap_inds wins: {f2}')
            return deap_inds
