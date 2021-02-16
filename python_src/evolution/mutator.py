import numpy


class Mutator:
    @staticmethod
    def mutate(deap_inds, mutate_params):
        individual = deap_inds[0]  # deap_individual is a list

        # value = get_speed_v1(individual)  # extract attribute value from an individual
        # std, dim = mutate_params['std'], mutate_params['dim']
        value = numpy.random.uniform(mutate_params["min"], mutate_params["max"], mutate_params["dim"])[0]
        if value < mutate_params['min']:
            value = mutate_params['min']
        if value > mutate_params['max']:
            value = mutate_params['max']

        # change_speed(individual, value)
        return individual  # return deap_individual
