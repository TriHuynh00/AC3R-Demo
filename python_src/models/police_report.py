from __future__ import annotations
from typing import Tuple
from abc import ABC, abstractmethod

CAT_A = 'A'
CAT_B = 'B'
CAT_C = 'C'
CAT_D = 'D'

CAT_A_DATA = ["ANY"]
CAT_B_DATA = ['F', 'M', 'B']
CAT_C_DATA = ['L', 'R']
CAT_D_DATA = ["FL", "FR", "ML", "MR", "BL", "BR"]

CATEGORIES = [{"type": CAT_A, "data": CAT_A_DATA},
              {"type": CAT_B, "data": CAT_B_DATA},
              {"type": CAT_C, "data": CAT_C_DATA},
              {"type": CAT_D, "data": CAT_D_DATA}]

NO_CRASH = 0


class Creator(ABC):
    @abstractmethod
    def factory_method(self):
        pass

    def match_operation(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        police_report = self.factory_method()
        return police_report.operation(outputs, targets)


"""
Concrete Creators override the factory method in order to change the resulting
report's type.
"""


class ConcreteCreatorA(Creator):
    def factory_method(self) -> Report:
        return ReportTypeA()


class ConcreteCreatorB(Creator):
    def factory_method(self) -> Report:
        return ReportTypeB()


class ConcreteCreatorC(Creator):
    def factory_method(self) -> Report:
        return ReportTypeC()


class ConcreteCreatorD(Creator):
    def factory_method(self) -> Report:
        return ReportTypeD()


class Report(ABC):
    """
    The Report interface declares the operations that all concrete reports
    must implement.
    """

    @abstractmethod
    def operation(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        """
        Return the amount of matched crashed elements(components/sides/parts),
               the amount of matched non-crashed elements(components/sides/parts)
               the amount of "matchable" elements for a specific report
        """
        pass

    @staticmethod
    def matching_operation(category: list,
                           t1: list, o1: list, t2: list, o2: list) -> Tuple[int, int]:
        """
        Count the amount of matched crashed elements(components/sides/parts) and
              the amount of matched non-crashed elements(components/sides/parts)

        Args:
            t1 (list): List of expected CRASHED element from a police report
            o1 (list): List of expected CRASHED element from a simulation
            t2 (list): List of expected NON-CRASHED element from a police report
            o2 (list): List of expected NON-CRASHED element from a simulation
            category (list): Set of vehicle elements are used to compare

        Returns:
            A tuple contains the number of matched crashed
            and non-crashed elements
        """
        crash_points = 0
        non_crash_points = 0
        for part in category:
            # Count matching expected CRASHED component
            crash_points += 1 if part in (set(t1) & set(o1)) else 0
            # Count matching expected NON-CRASHED component
            non_crash_points += 1 if part in (set(t2) & set(o2)) else 0

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
            return part[0]
        elif report_type == CAT_C:
            return part[1]
        return None

    @staticmethod
    def _validate_output(outputs: list) -> True:
        """
        Verify the validity of given output from simulation
        """
        # Invalid given outputs
        if len(outputs) is NO_CRASH:
            raise Exception("Given scenario might not be crashed!")
        # The vehicle's crash element should be on the car
        parts = CAT_D_DATA
        for item in [i["name"] for i in outputs]:
            if item not in parts:
                raise Exception(f'Part name {item} is not found!')
        return True


"""
Concrete Reports provide various implementations of the Report interface.
"""


class ReportTypeA(Report):
    def operation(self, outputs: list, targets: list) -> Tuple[int, int, int]:
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
    def operation(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_B_DATA)

        # From Police Report:
        # List t1 contains expected CRASHED component
        # List t2 contains expected NON-CRASHED component
        targets = [part["name"] for part in targets]
        t1, t2 = targets, list(set(CAT_B_DATA) - set(targets))

        # From Simulation:
        # List o1 contains CRASHED component
        # List o2 contains NON-CRASHED component
        # Remove duplicates from a list outputs
        outputs = list(dict.fromkeys([self._categorize_part(i["name"], CAT_B) for i in outputs]))
        o1, o2 = outputs, list(set(CAT_B_DATA) - set(outputs))

        crash_points, non_crash_points = self.matching_operation(CAT_B_DATA, t1, o1, t2, o2)
        return crash_points, non_crash_points, point_target


class ReportTypeC(Report):
    def operation(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_C_DATA)

        # From Police Report:
        # List t1 contains expected CRASHED side
        # List t2 contains expected NON-CRASHED side
        targets = [part["name"] for part in targets]
        t1, t2 = targets, list(set(CAT_C_DATA) - set(targets))

        # From Simulation:
        # List o1 contains CRASHED side
        # List o2 contains NON-CRASHED side
        # Remove duplicates from a list outputs
        outputs = list(dict.fromkeys([self._categorize_part(i["name"], CAT_C) for i in outputs]))
        o1, o2 = outputs, list(set(CAT_C_DATA) - set(outputs))

        crash_points, non_crash_points = self.matching_operation(CAT_C_DATA, t1, o1, t2, o2)
        return crash_points, non_crash_points, point_target


class ReportTypeD(Report):
    def operation(self, outputs: list, targets: list) -> Tuple[int, int, int]:
        # Validate given output from simulation
        self._validate_output(outputs)

        # The maximum point a scenario can earn
        point_target = len(CAT_D_DATA)

        # From Police Report:
        # List t1 contains expected CRASHED parts
        # List t2 contains expected NON-CRASHED parts
        targets = [part["name"] for part in targets]
        t1, t2 = targets, list(set(CAT_D_DATA) - set(targets))

        # From Simulation:
        # List o1 contains CRASHED parts
        # List o2 contains NON-CRASHED parts
        outputs = [i["name"] for i in outputs]
        o1, o2 = outputs, list(set(CAT_D_DATA) - set(outputs))

        crash_points, non_crash_points = self.matching_operation(CAT_D_DATA, t1, o1, t2, o2)
        return crash_points, non_crash_points, point_target


def _categorize_report(report_data: list) -> Creator:
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
            return ConcreteCreatorA()
        if CAT_B in categories:
            return ConcreteCreatorB()
        if CAT_C in categories:
            return ConcreteCreatorC()
        if CAT_D in categories:
            return ConcreteCreatorD()
