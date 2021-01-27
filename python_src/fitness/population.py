class Population:
    def __init__(self, individuals):
        self.individuals = individuals
        self.fittest = 0

    # Get the fittest individual
    def get_fittest(self):
        return self.individuals[0]

    # Get the second most fittest individual
    def get_second_fittest(self):
        return self.individuals[1]

    # Get index of least fittest individual
    def get_least_fittest(self):
        return self.individuals[-1]

    # Calculate fitness of each individual
    def calculate_fitness(self):
        for i in range(len(self.individuals)):
            self.individuals[i].cal_fitness()
        self.individuals = sorted(self.individuals, key=lambda x: x.fitness, reverse=True)
        self.get_fittest()
