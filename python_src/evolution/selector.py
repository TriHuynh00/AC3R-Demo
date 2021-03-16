class Selector:
    @staticmethod
    def select_by_aggregate(aggregate_func, orig_inds, pop_ind):
        deap_inds = pop_ind[0]  # deap_pop is a list

        f1 = aggregate_func(orig_inds[0].simulation_results)
        f2 = aggregate_func(deap_inds[0].simulation_results)

        if f1 >= f2:
            return orig_inds
        return deap_inds  # return deap_individual
