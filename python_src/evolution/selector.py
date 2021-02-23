class Selector:
    @staticmethod
    def select_best_ind(orig_inds, pop_ind):
        deap_inds = pop_ind[0]  # deap_pop is a list

        f1 = orig_inds.fitness.values
        f2 = deap_inds.fitness.values

        if f1 >= f2:
            return orig_inds
        return deap_inds  # return deap_individual
