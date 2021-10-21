import json
import time
from evolution import RandomEvolution, OpoEvolution, Mutator, Fitness, Generator, Selector
from models import categorize_mutator, CONST
from models.ac3rp import CrashScenario
from models.mutator import Transformer
from typing import List, Dict


class Experiment:
    def __init__(self, file_path: str, threshold: float, method_name: str, mutators: List[Dict], case_name: str,
                 simulation_name: str = None):

        self.method_name = method_name
        self.case_name = case_name
        # Generate mutators
        self.mutators = [categorize_mutator(m) for m in mutators]

        try:
            tmp_simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', time.strftime('%Y-%m-%d--%H-%M-%S',
                                                                                             time.localtime()))
            self.simulation_name = tmp_simulation_name if simulation_name is None else simulation_name
            self.threshold = threshold
            with open(file_path) as file:
                self.scenario = json.load(file)
        except Exception as ex:
            print(f'Scenario is not found. Exception: {ex}')

    def run(self):
        if self.scenario is None:
            print(f'Scenario is not found')
            return False

        # Run the experiment
        if self.method_name == CONST.RANDOM:
            self._run_rev()
        elif self.method_name == CONST.OPO:
            self._run_opo()

    def _run_rev(self):
        # Write data file
        rev_logfile = open(f'data/{self.case_name}/{self.simulation_name}.csv', "a")
        rev_logfile.write("v1,v2,score\n")
        rev_log_data_file = f'data/{self.case_name}/log_{self.simulation_name}.csv'

        # Experiment run
        rev = RandomEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            # fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params=Transformer(self.mutators),
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
        # Write data file
        opo_logfile = open(f'data/{self.case_name}/{self.simulation_name}.csv', "a")
        opo_logfile.write("v1,v2,score\n")
        opo_log_data_file = f'data/{self.case_name}/log_{self.simulation_name}.csv'

        # Experiment run
        oev = OpoEvolution(
            scenario=CrashScenario.from_json(self.scenario),
            fitness=Fitness.evaluate,
            # fitness_repetitions=5,
            generate=Generator.generate_random_from,
            generate_params=Transformer(self.mutators),
            mutate=Mutator.mutate_from,
            mutate_params=Transformer(self.mutators),
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
