import numpy
from models import SimulationFactory, SimulationScore, SimulationExec
from models.simulation import Simulation
from models.ac3rp import CrashScenario


class Fitness:
    @staticmethod
    def evaluate(repetitions: int, log_data_file, deap_inds):
        individual: CrashScenario = deap_inds[0]
        scores = []
        for _ in range(repetitions):
            sim_factory = SimulationFactory(individual)
            simulation = Simulation(sim_factory=sim_factory)
            simulation_score = SimulationScore(simulation)
            simulation_score.get_expected_score()
            try:
                SimulationExec(simulation).execute_scenario(timeout=40)
                scores.append(simulation_score.calculate())
                simulation_score.write_log_file(log_data_file)
            except Exception as e:
                print(f'Fitness Exception: {str(e)}')
                simulation_score.write_log_file(log_data_file, str(e))
                scores.append(0)
        individual.scores = scores
        print(f'Scores: {scores}')
        return numpy.mean(scores),
