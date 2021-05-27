from models.police_report import Report, CAT_D_DATA
from typing import Tuple

"""
Concrete Reports provide various implementations of the Report interface.
"""


class ReportTypeD(Report):
    def decode_part(self, part: str) -> [str]:
        if len(part) == 1:
            # Police Report is Side-Component Type and Simulation is Side Type:
            # Side is [L] or [R] -> [ F{Side}, M{Side}, M{Side} ]
            if part in ['L', 'R']:
                return [f'F{part}', f'M{part}', f'B{part}']
            # Police Report is Side-Component Type and Simulation is Component Type:
            # Component is [F], [B], [M] -> [ {Component}L, {Component}R ]
            elif part in ['F', 'M', 'B']:
                return [f'{part}L', f'{part}R']
        else:
            return [part]

    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_D_DATA)

        # From Police Report:
        # List crashes_from_police_report contains expected CRASHED parts
        # List non_crashes_from_police_report contains expected NON-CRASHED parts
        targets = [part["name"] for part in targets]
        crashes_from_police_report, non_crashes_from_police_report = targets, list(set(CAT_D_DATA) - set(targets))

        # From Simulation:
        # List crashes_from_simulation contains CRASHED parts
        # List non_crashes_from_simulation contains NON-CRASHED parts
        outputs = list((dict.fromkeys([i for output in outputs for i in self.decode_part(output["name"])])))
        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CAT_D_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CAT_D_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target
