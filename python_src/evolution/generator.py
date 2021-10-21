import copy
from models.ac3rp import CrashScenario
from models.mutator import Transformer


class Generator:
    @staticmethod
    def generate_random_from(scenario: CrashScenario, transformer: Transformer):
        # Create a new crash scenario
        return transformer.mutate_random_from(copy.deepcopy(scenario))
