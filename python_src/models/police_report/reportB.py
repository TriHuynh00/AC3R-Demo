from models.police_report import Report
from typing import Tuple

"""
Concrete Reports provide various implementations of the Report interface.
"""


class ReportTypeB(Report):
    def decode_part(self, part: str) -> [str]:
        if len(part) == 1:
            if part in ['L', 'R']:  # Police Report is Side Type: [L], [R] -> ['F', 'M', 'B']
                return ['F', 'M', 'B']
            elif part in ['F', 'M', 'B']:  # Police Report is Side Type: [F], [B], [M] -> same
                return [part[0]]
        else:
            # Police Report is Side Type: F[L], B[R] -> L, R
            return [part[0]]

    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        from models import CONST

        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CONST.CAT_B_DATA)

        # From Police Report:
        # List crashes_from_police_report contains expected CRASHED component
        # List non_crashes_from_police_report contains expected NON-CRASHED component
        targets = [part["name"] for part in targets]
        crashes_from_police_report, non_crashes_from_police_report = targets, list(set(CONST.CAT_B_DATA) - set(targets))

        # From Simulation:
        # List crashes_from_simulation contains CRASHED component
        # List non_crashes_from_simulation contains NON-CRASHED component
        # Remove duplicates from a list outputs by dict.fromkeys
        outputs = list((dict.fromkeys([i for output in outputs for i in self.decode_part(output["name"])])))
        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CONST.CAT_B_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CONST.CAT_B_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target
