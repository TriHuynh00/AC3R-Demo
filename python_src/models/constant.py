class Constants:
    def __init__(self):
        self.MUTATE_SPEED_CLASS = "MUTATE_SPEED_CLASS"                  # Mutator mutates a speed
        self.MUTATE_INITIAL_POINT_CLASS = "MUTATE_INITIAL_POINT_CLASS"  # Mutator mutates an initial point

        self.RANDOM = "RANDOM"
        self.OPO = "OPO"

        self.CAT_A = 'A'  # Report with a crashed scenario
        self.CAT_B = 'B'  # Report provides crashed components (front / middle / back)
        self.CAT_C = 'C'  # Report provides crashed sides (left / right)
        self.CAT_D = 'D'  # Report provides crashed part with its component and side (front left / front right)
        self.CAT_A_DATA = ["ANY"]
        self.CAT_B_DATA = ['F', 'M', 'B']
        self.CAT_C_DATA = ['L', 'R']
        self.CAT_D_DATA = ["FL", "FR", "ML", "MR", "BL", "BR"]
        self.CAT_BC_DATA = ['F', 'M', 'B', 'L', 'R']
        self.CATEGORIES = [{"type": self.CAT_A, "data": self.CAT_A_DATA},
                           {"type": self.CAT_B, "data": self.CAT_B_DATA},
                           {"type": self.CAT_C, "data": self.CAT_C_DATA},
                           {"type": self.CAT_D, "data": self.CAT_D_DATA}]
        self.EMPTY_CRASH = 0

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)


CONST = Constants()
