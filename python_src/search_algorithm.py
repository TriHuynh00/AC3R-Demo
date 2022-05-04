# Implement the classes needed for the search algorithm
import logging
import random
from copy import deepcopy
from deap import tools, creator, base
from models.ac3rp import CrashScenario
from libraries.libs import collect_sim_data, collect_police_report
from models.simulation import Simulation

module_logger = logging.getLogger('Search Algorithm')


def _mutate_scenario_by_speed(scenario, min_speed, max_speed):
    mutated_scenario = deepcopy(scenario)
    for v in mutated_scenario.vehicles:
        idx = 0
        while idx < len(v.driving_actions):
            v.driving_actions[idx]["speed"] = random.randint(min_speed, max_speed)
            idx += 1
    return mutated_scenario


def _eval_one_max(individual):
    crash_scenario, bng_roads, bng_vehicles = collect_sim_data(individual[0])
    # Execute crash scenario and collect simulation's result
    simulation = Simulation(bng_roads, bng_vehicles)
    simulation.execute_scenario()
    crash_scenario.sim_report = simulation.get_report()

    # Fixed sample report data
    # TODO: change the sample police report to dynamic variable
    report_data = collect_police_report("./data/sample_report.json")
    crash_scenario.cal_fitness(report_data) # Calculate fitness score
    return crash_scenario.score,


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
        creator.create("Individual", list, fitness=creator.FitnessMax)

        self.toolbox = base.Toolbox()

        # Define the common search operator
        # self.toolbox.register("mutate", crossover.crossover)
        # self.toolbox.register("mate", crossover.crossover)
        # Define the evaluation function
        self.toolbox.register("evaluate", _eval_one_max)
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

        # Attr generator
        # Make sure that we create random individuals by mutating the original_scenario only
        min_speed, max_speed = 10, 90
        self.toolbox.register("attr_bool", _mutate_scenario_by_speed, original_scenario, 10, 90)
        # Create a random individual of type from the original scenario. For doing so we use high-order-functions
        self.toolbox.register("individual", tools.initRepeat, creator.Individual, self.toolbox.attr_bool, 1)
        # Create the entire population represented as list made of random individuals of type creator.Individual
        self.toolbox.register("population", tools.initRepeat, list, self.toolbox.individual)

        # Execute the original_scenario. Note that we need to wrap the original_scenario object into an instance of
        # creator.Individual
        pop = self.toolbox.population(n=3)
        print(pop)

        fitnesses = list(map(self.toolbox.evaluate, pop))
        for ind, fit in zip(pop, fitnesses):
            ind.fitness.values = fit

        # Extracting all the fitnesses of population
        fits = [ind.fitness.values[0] for ind in pop]
        print(fits)
