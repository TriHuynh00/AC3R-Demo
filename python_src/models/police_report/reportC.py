from models.police_report import Report, CAT_C_DATA
from typing import Tuple

"""
Concrete Reports provide various implementations of the Report interface.
"""


class ReportTypeC(Report):
    def decode_part(self, part: str) -> [str]:
        if len(part) == 1:
            if part in ['L', 'R']:  # Police Report is Side Type: [L], [R] -> same
                return [part[0]]
            elif part in ['F', 'M', 'B']:  # Police Report is Side Type: [F], [B], [M] -> L and R
                return ['L', 'R']
        else:
            # Police Report is Side Type: F[L], B[R] -> L, R
            return [part[1]]

    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_C_DATA)

        # From Police Report:
        # List crashes_from_police_report contains expected CRASHED side
        # List non_crashes_from_police_report contains expected NON-CRASHED side
        targets = [part["name"] for part in targets]
        crashes_from_police_report, non_crashes_from_police_report = targets, list(set(CAT_C_DATA) - set(targets))

        # From Simulation:
        # List crashes_from_simulation contains CRASHED side
        # List non_crashes_from_simulation contains NON-CRASHED side
        # Remove duplicates from a list outputs by dict.fromkeys
        outputs = list((dict.fromkeys([i for output in outputs for i in self.decode_part(output["name"])])))
        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CAT_C_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CAT_C_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target
