import numpy
from models import SimulationFactory, SimulationScore, SimulationExec
from models.simulation import Simulation
from models.ac3rp import CrashScenario


class Fitness:
    @staticmethod
    def evaluate(repetitions: int, deap_inds):
        individual: CrashScenario = deap_inds[0]
        scores = []
        for _ in range(repetitions):
            sim_factory = SimulationFactory(individual)
            simulation = Simulation(sim_factory=sim_factory)
            try:
                SimulationExec(simulation).execute_scenario(timeout=40)
                scores.append(SimulationScore(simulation).calculate())
            except Exception as e:
                print(str(e))
                scores.append(0)
        individual.scores = scores
        print(f'Scores: {scores}')
        return numpy.mean(scores),
