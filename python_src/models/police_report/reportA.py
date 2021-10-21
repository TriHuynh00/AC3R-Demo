from models.police_report import Report
from typing import Tuple

"""
Concrete Reports provide various implementations of the Report interface.
"""


class ReportTypeA(Report):
    def decode_part(self, part: str):
        pass

    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        from models import CONST

        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CONST.CAT_A_DATA)
        crash_points, non_crash_points = 0, 0

        # From Simulation:
        # List outputs contains CRASHED parts
        outputs = [i["name"] for i in outputs]

        if any(item in ["ANY", 'F', 'M', 'B', 'L', 'R', "FL", "FR", "ML", "MR", "BL", "BR"] for item in outputs):
            return 1, non_crash_points, point_target

        return crash_points, 1, point_target
