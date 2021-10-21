from models.police_report import Report
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
                # Empty list - discard since it doesn't tell you the accurate side
                # Assume we have an another broken part will tell us the side
                return []
        else:
            return [part]

    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        from models import CONST

        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CONST.CAT_D_DATA)

        # From Police Report:
        # List crashes_from_police_report contains expected CRASHED parts
        # List non_crashes_from_police_report contains expected NON-CRASHED parts
        targets = [part["name"] for part in targets]
        crashes_from_police_report, non_crashes_from_police_report = targets, list(set(CONST.CAT_D_DATA) - set(targets))

        # From Simulation:
        # List crashes_from_simulation contains CRASHED parts
        # List non_crashes_from_simulation contains NON-CRASHED parts
        is_contain_components = False
        component_parts = list()
        decode_parts = list()
        for output in outputs:
            if output["name"] in ['F', 'M', 'B']:
                is_contain_components = True
                if output["name"] not in component_parts:
                    component_parts.append(output["name"])
            for i in self.decode_part(output["name"]):
                decode_parts.append(i)

        # TODO:
        # If we don't have any L or R in outputs and the output already contains component parts F M B,
        # we will add 'L', 'R' to the outputs
        if len(decode_parts) == 0 and is_contain_components:
            for part in component_parts:
                decode_parts.append(f'{part}L')
                decode_parts.append(f'{part}R')

        # Final outputs
        # Remove duplicates from a list outputs by dict.fromkeys
        outputs = list((dict.fromkeys(decode_parts)))

        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CONST.CAT_D_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CONST.CAT_D_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target
