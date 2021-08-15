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
CAT_BC_DATA = ['F', 'M', 'B', 'L', 'R']

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
        from models.police_report import ReportTypeA  # Fix Circular Dependencies
        return ReportTypeA()


class ComponentCreator(ReportCreator):
    """
    ComponentCreator override the factory method in order to
    generate a report with crashed components.
    """

    def create(self) -> Report:
        from models.police_report import ReportTypeB  # Fix Circular Dependencies
        return ReportTypeB()


class SideCreator(ReportCreator):
    """
    SideCreator override the factory method in order to
    generate a report with crashed sides.
    """

    def create(self) -> Report:
        from models.police_report import ReportTypeC  # Fix Circular Dependencies
        return ReportTypeC()


class ComponentSideCreator(ReportCreator):
    """
    ComponentSideCreator override the factory method in order to
    generate a report with crashed parts including their component
    and side.
    """

    def create(self) -> Report:
        from models.police_report import ReportTypeD  # Fix Circular Dependencies
        return ReportTypeD()


class ComponentSideShortCreator(ReportCreator):
    """
    ComponentSideShortCreator override the factory method in order to
    generate a report with crashed parts including F M B L R.
    """

    def create(self) -> Report:
        from models.police_report import ReportTypeBC  # Fix Circular Dependencies
        return ReportTypeBC()


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

    @abstractmethod
    def decode_part(self, part: str) -> [str]:
        """
        Decodes the vehicle part based on the type of police report.

        Args:
            part (str): The code name of vehicle part
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
            non_crash_points += 1 if part in (
                    set(non_crashes_from_police_report) & set(non_crashes_from_simulation)) else 0

        return crash_points, non_crash_points

    @staticmethod
    def _validate_output(outputs: list) -> True:
        """
        Verify the validity of given output from simulation
        """
        # Invalid given outputs
        if len(outputs) is EMPTY_CRASH:
            raise Exception("Exception: The simulator did not report any crashes!")
        # The vehicle's crash element should be on the car
        parts = CAT_D_DATA + CAT_C_DATA + CAT_B_DATA + CAT_A_DATA
        for item in [i["name"] for i in outputs]:
            if item not in parts:
                raise Exception(f'Exception: The code {item} is not found in the part dictionary!')
        return True


def categorize_report(report_data: list) -> ReportCreator:
    """
    Categorizes the type of police report
    """
    categories = []
    for part in report_data:
        for category in CATEGORIES:
            if part["name"] in category["data"] and category["type"] not in categories:
                categories.append(category["type"])

    if len(categories) == 1:
        if CAT_A in categories:
            return AnyCreator()
        if CAT_B in categories:
            return ComponentCreator()
        if CAT_C in categories:
            return SideCreator()
        if CAT_D in categories:
            return ComponentSideCreator()
    if len(categories) == 2:
        if CAT_B in categories and CAT_C in categories:
            return ComponentSideShortCreator()

    raise Exception("Exception: Report Type not found!")
