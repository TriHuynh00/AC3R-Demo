import numpy as np
import scipy.stats as stats


class Selector:
    @staticmethod
    def select_by_aggregate(aggregate_func, orig_inds, pop_ind):
        deap_inds = pop_ind[0]  # deap_pop is a list
        if aggregate_func is None:
            aggregate_func = np.mean

        f1 = aggregate_func(orig_inds[0].simulation_results)
        f2 = aggregate_func(deap_inds[0].simulation_results)

        if f1 >= f2:
            return orig_inds
        return deap_inds  # return deap_individual

    @staticmethod
    def select_by_vda(vda_func, orig_inds, pop_ind):
        deap_inds = pop_ind[0]

        f1 = orig_inds[0].simulation_results
        f2 = deap_inds[0].simulation_results

        # Calculate the p-value to know if two populations are different
        alpha = 0.05  # significance level to reject H0
        stat, p = stats.mannwhitneyu(f1, f2)
        print('Test Statistics=%.3f, p=%.3f' % (stat, p))
        if p > alpha:
            print('H0: The two populations are equal versus.')
            return orig_inds
        else:
            print('H1: The two populations are different.')
            # To understand how different they were (the effect size)
            # we used VDA
            if vda_func(f1, f2) > 0.5:
                return deap_inds
            return orig_inds
