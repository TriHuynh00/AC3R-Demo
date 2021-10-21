import json
import time
from evolution import RandomEvolution, OpoEvolution, Mutator, Fitness, Generator, Selector
from models import categorize_mutator, CONST
from models.ac3rp import CrashScenario
from models.mutator import Transformer


class Experiment:
    def __init__(self, file_path: str, threshold: float, simulation_name: str = None):
        self.mutators = []
        try:
            tmp_simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', time.strftime('%Y-%m-%d--%H-%M-%S',
                                                                                             time.localtime()))
            self.simulation_name = tmp_simulation_name if simulation_name is None else simulation_name
            self.threshold = threshold
            with open(file_path) as file:
                self.scenario = json.load(file)
        except Exception as ex:
            print(f'Scenario is not found. Exception: {ex}')

    def set_mutators(self, mutators):
        self.mutators = mutators

    def run(self, method_name: str):
        if self.scenario is None:
            print(f'Scenario is not found')
            return False

        # Run the experiment
        if method_name == CONST.RANDOM:
            self._run_rev()
        elif method_name == CONST.OPO:
            self._run_opo()

    def _run_rev(self):
        # Write data file
        rev_logfile = open(f'data/{self.simulation_name[0:5]}/{self.simulation_name}.csv', "a")
        rev_logfile.write("v1,v2,score\n")
        rev_log_data_file = f'data/{self.simulation_name[0:5]}/log_{self.simulation_name}.csv'

        # Experiment run
        rev = RandomEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            # fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            select=Selector.by_fitness_value,
            # select_aggregate=numpy.mean,
            epochs=30,
            logfile=rev_logfile,
            log_data_file=rev_log_data_file,
            threshold=self.threshold
        )
        rev.run()

        # Close logfiles
        rev_logfile.close()

    def _run_opo(self):
        # Generate mutators
        mutators = [categorize_mutator(m) for m in self.mutators]

        # Write data file
        opo_logfile = open(f'data/{self.simulation_name[0:5]}/{self.simulation_name}.csv', "a")
        opo_logfile.write("v1,v2,score\n")
        opo_log_data_file = f'data/{self.simulation_name[0:5]}/log_{self.simulation_name}.csv'

        # Experiment run
        oev = OpoEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            # fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params={"min": 10, "max": 50},
            mutate=Mutator.mutate_from,
            mutate_params=Transformer(mutators),
            select=Selector.by_fitness_value,
            # select_aggregate=libs._VD_A,
            epochs=30,
            logfile=opo_logfile,
            log_data_file=opo_log_data_file,
            threshold=self.threshold
        )
        oev.run()

        # Close logfiles
        opo_logfile.close()
