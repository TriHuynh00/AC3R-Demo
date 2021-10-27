from models.police_report import Report
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
                # Empty list - discard since it doesn't tell you the accurate side
                # Assume we have an another broken part will tell us the side
                return []
        else:
            # Police Report is Side Type: F[L], B[R] -> L, R
            return [part[1]]

    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        from models import CONST

        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CONST.CAT_C_DATA)

        # From Police Report:
        # List crashes_from_police_report contains expected CRASHED side
        # List non_crashes_from_police_report contains expected NON-CRASHED side
        targets = [part["name"] for part in targets]
        crashes_from_police_report, non_crashes_from_police_report = targets, list(set(CONST.CAT_C_DATA) - set(targets))

        # From Simulation:
        # List crashes_from_simulation contains CRASHED side
        # List non_crashes_from_simulation contains NON-CRASHED side
        is_contain_components = False
        decode_parts = list()
        for output in outputs:
            if output["name"] in ['F', 'M', 'B']:
                is_contain_components = True
            for i in self.decode_part(output["name"]):
                decode_parts.append(i)

        # TODO:
        # If we don't have any L or R in outputs and the output already contains component parts F M B,
        # we will add 'L', 'R' to the outputs
        if len(decode_parts) == 0 and is_contain_components:
            decode_parts = ['L', 'R']

        # Final outputs
        # Remove duplicates from a list outputs by dict.fromkeys
        outputs = list((dict.fromkeys(decode_parts)))

        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CONST.CAT_C_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CONST.CAT_C_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target
