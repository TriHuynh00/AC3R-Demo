import json
import numpy
import libs

from evolution import LogBook, RandomEvolution, OpoEvolution, Mutator, Fitness, Generator, Selector
from models import SimulationFactory, Simulation, SimulationScore
from models.ac3rp import CrashScenario


class Experiment:
    def __init__(self, file_path: str):
        try:
            with open(file_path) as file:
                self.scenario = json.load(file)
        except Exception as ex:
            print(f'Scenario is not found. Exception: {ex}')

    def run(self):
        if self.scenario is None:
            print(f'Scenario is not found')
            return False

        # Seeds
        numpy.random.seed(64)
        # Calculate the expected score
        sim_factory: SimulationFactory = SimulationFactory(CrashScenario.from_json(self.scenario))
        simulation: Simulation = Simulation(sim_factory=sim_factory)
        expected_score: float = SimulationScore(simulation).get_expected_score()
        logbook: LogBook = LogBook(score=expected_score)

        # Experiment run
        rev = RandomEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            select=Selector.by_vda_f,
            select_aggregate=libs._VD_A,
            epochs=10
        )
        oev = OpoEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            mutate=Mutator.by_speed,
            mutate_params={"mean": 2.1, "std": 1, "min": 10, "max": 50},
            select=Selector.by_vda_f,
            select_aggregate=libs._VD_A,
            epochs=10
        )
        rev.run()
        oev.run()

        # Provide plot chart
        logbook.visualize(rev.logbook, oev.logbook, "Random", "OPO")
