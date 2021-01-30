# Implement the classes needed for the search algorithm

import logging
import random
from deap import tools, creator, base
from ac3r_plus import CrashScenario

module_logger = logging.getLogger('Search Algorithm')


class AC3RPlusSearch:

    def __init__(self, seed=None):
        # Create and configure the toolbox
        # Ensure we use the provided seed for repeatability
        self.seed = seed
        if self.seed is not None:
            random.seed(self.seed)

        # Use a single fitness function.
        creator.create("FitnessMax", base.Fitness, weights=(1.0,))

        # Dynamically define a class named "Individual" that extends CrashScenario with the attribute 'fitness'
        creator.create("Individual", CrashScenario, fitness=creator.FitnessMax)

        self.toolbox = base.Toolbox()
        self.toolbox.register("individual", tools.initRepeat(), creator.Individual)
        self.toolbox.register("population", tools.initRepeat(), list, self.toolbox.individual)

        # Define the common search operator
        # self.toolbox.register("mutate", crossover.crossover)
        # self.toolbox.register("mate", crossover.crossover)
        # Define the evaluation function
        # self.toolbox.register("evaluate", crossover.crossover)

        # TODO Use a standard single objective GA to solve this problem using the given toolbox

        pass

    def generate_random_individual_from(self, original_scenario):
        def _f():
            module_logger.debug("Creating a random scenario from %s", original_scenario)
            # Randomly mutate a scenario according to the "mutate" function registered before
            # TODO We can implement a more complex logic here, but for the moment this is more than ok
            mutated_scenario = self.toolbox.mutate(creator.Individual(original_scenario))
            # TODO Maybe we need to reset this or something?
            module_logger.info("Obtained %s", mutated_scenario)
            return mutated_scenario

        return _f

    def start_from(self, original_scenario: CrashScenario):
        """
        Start the search algorithm from a given individual, called the 'original_scenario'
        """

        # Execute the original_scenario. Note that we need to wrap the original_scenario object into an instance of
        # creator.Individual
        self.toolbox.evaluate(creator.Individual(original_scenario))

        # Make sure that we create random individuals by mutating the original_scenario only

        # Create a random individual of type from the original scenario. For doing so we use high-order-functions
        # TODO Not sure what do I need here... as additional parameters
        self.toolbox.register("random_individual",
                              self.generate_random_individual_from(original_scenario),
                              creator.Individual)

        # Create the entire population represented as list made of random individuals of type creator.Individual
        self.toolbox.register("population", tools.initRepeat, list, self.toolbox.random_individual)

        # TODO What is the size of the population?
        # Start the generation ?
