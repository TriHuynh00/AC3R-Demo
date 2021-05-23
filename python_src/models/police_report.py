from __future__ import annotations
from typing import Tuple
from abc import ABC, abstractmethod

CAT_A = 'A'  # Report with a crashed scenario
CAT_B = 'B'  # Report provides crashed components (front / middle / back)
CAT_C = 'C'  # Report provides crashed sides (left / right)
CAT_D = 'D'  # Report provides crashed part with its component and side (front left / front right)

CAT_A_DATA = ["ANY"]
CAT_B_DATA = ['F', 'M', 'B']
CAT_C_DATA = ['L', 'R']
CAT_D_DATA = ["FL", "FR", "ML", "MR", "BL", "BR"]

CATEGORIES = [{"type": CAT_A, "data": CAT_A_DATA},
              {"type": CAT_B, "data": CAT_B_DATA},
              {"type": CAT_C, "data": CAT_C_DATA},
              {"type": CAT_D, "data": CAT_D_DATA}]

EMPTY_CRASH = 0


class ReportCreator(ABC):
    """
    The ReportCreator class declares the factory method that returns new report objects.
    The return type of this method must match the Report interface.
    """

    @abstractmethod
    def create(self) -> Report:
        pass

    def match(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        police_report = self.create()
        return police_report.process(outputs, targets)


"""
Concrete Creators override the factory method in order to 
generate a different type of report.
"""


class AnyCreator(ReportCreator):
    """
    AnyCreator override the factory method in order to
    generate a report with any crash.
    """

    def create(self) -> Report:
        return ReportTypeA()


class ComponentCreator(ReportCreator):
    """
    ComponentCreator override the factory method in order to
    generate a report with crashed components.
    """

    def create(self) -> Report:
        return ReportTypeB()


class SideCreator(ReportCreator):
    """
    SideCreator override the factory method in order to
    generate a report with crashed sides.
    """

    def create(self) -> Report:
        return ReportTypeC()


class ComponentSideCreator(ReportCreator):
    """
    ComponentSideCreator override the factory method in order to
    generate a report with crashed parts including their component
    and side.
    """

    def create(self) -> Report:
        return ReportTypeD()


class Report(ABC):
    """
    The Report interface declares the operations that all concrete reports
    must implement.
    """

    @abstractmethod
    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        """
        Return the amount of matched crashed elements(components/sides/parts),
               the amount of matched non-crashed elements(components/sides/parts)
               the amount of "matchable" elements for a specific report
        """
        pass

    @staticmethod
    def _match(category: list,
               crashes_from_police_report: list, crashes_from_simulation: list,
               non_crashes_from_police_report: list, non_crashes_from_simulation: list) -> Tuple[int, int]:
        """
        Count the amount of matched crashed elements(components/sides/parts) and
              the amount of matched non-crashed elements(components/sides/parts)

        Args:
            crashes_from_police_report (list):     List of expected CRASHED element from a police report
            crashes_from_simulation (list):        List of expected CRASHED element from a simulation
            non_crashes_from_police_report (list): List of expected NON-CRASHED element from a police report
            non_crashes_from_simulation (list):    List of expected NON-CRASHED element from a simulation
            category (list):                       Set of vehicle elements are used to compare

        Returns:
            A tuple contains the number of matched crashed
            and non-crashed elements
        """
        crash_points = 0
        non_crash_points = 0
        for part in category:
            # Count matching expected CRASHED component
            crash_points += 1 if part in (set(crashes_from_police_report) & set(crashes_from_simulation)) else 0
            # Count matching expected NON-CRASHED component
            non_crash_points += 1 if part in (set(non_crashes_from_police_report) & set(non_crashes_from_simulation)) else 0

        return crash_points, non_crash_points

    @staticmethod
    def _categorize_part(part: str, report_type: chr) -> chr:
        """
        Categorizes the type of vehicle part based on the type of police report.

        Args:
            part (str): The code name of vehicle part
            report_type (chr): Type of report is used to categorize
        """
        if report_type == CAT_B:
            # Police Report is Component Type: [F]L, [M]L, [B]L -> F, M, B
            return part[0]
        elif report_type == CAT_C:
            # Police Report is Side Type: F[L], B[R] -> L, R
            return part[1]
        return None

    @staticmethod
    def _validate_output(outputs: list) -> True:
        """
        Verify the validity of given output from simulation
        """
        # Invalid given outputs
        if len(outputs) is EMPTY_CRASH:
            raise Exception("The simulator did not report any crashes!")
        # The vehicle's crash element should be on the car
        parts = CAT_D_DATA + CAT_C_DATA + CAT_B_DATA + CAT_A_DATA
        for item in [i["name"] for i in outputs]:
            if item not in parts:
                raise Exception(f'The code {item} is not found in the part dictionary!')
        return True


"""
Concrete Reports provide various implementations of the Report interface.
"""


class ReportTypeA(Report):
    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_A_DATA)
        crash_points, non_crash_points = 0, 0

        # From Simulation:
        # List outputs contains CRASHED parts
        outputs = [i["name"] for i in outputs]

        if any(item in CAT_D_DATA for item in outputs):
            return 1, non_crash_points, point_target

        return crash_points, 1, point_target


class ReportTypeB(Report):
    def process(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_B_DATA)

        # From Police Report:
        # List crashes_from_police_report contains expected CRASHED component
        # List non_crashes_from_police_report contains expected NON-CRASHED component
        targets = [part["name"] for part in targets]
        crashes_from_police_report, non_crashes_from_police_report = targets, list(set(CAT_B_DATA) - set(targets))

        # From Simulation:
        # List crashes_from_simulation contains CRASHED component
        # List non_crashes_from_simulation contains NON-CRASHED component
        # Remove duplicates from a list outputs by dict.fromkeys
        outputs = list(
            (dict.fromkeys([i for output in outputs for i in self._categorize_part_reportB(output["name"])])))
        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CAT_B_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CAT_B_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target


class ReportTypeC(Report):
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
        outputs = list(
            (dict.fromkeys([i for output in outputs for i in self._categorize_part_reportC(output["name"])])))
        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CAT_C_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CAT_C_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target


class ReportTypeD(Report):
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
        outputs = [i["name"] for i in outputs]
        crashes_from_simulation, non_crashes_from_simulation = outputs, list(set(CAT_D_DATA) - set(outputs))

        crash_points, non_crash_points = self._match(CAT_D_DATA,
                                                     crashes_from_police_report, crashes_from_simulation,
                                                     non_crashes_from_police_report, non_crashes_from_simulation)
        return crash_points, non_crash_points, point_target


def categorize_report(report_data: list) -> ReportCreator:
    """
    Categorizes the type of police report
    """
    categories = []
    for part in report_data:
        for category in CATEGORIES:
            if part["name"] in category["data"] and category["type"] not in categories:
                categories.append(category["type"])

    if len(categories) > 1:  # Not support multi-categories
        raise Exception("Report Type not found. More than 1 category reported!")
    else:
        if CAT_A in categories:
            return AnyCreator()
        if CAT_B in categories:
            return ComponentCreator()
        if CAT_C in categories:
            return SideCreator()
        if CAT_D in categories:
            return ComponentSideCreator()
