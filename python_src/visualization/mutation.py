class Mutation:
    def __init__(self, name, xs, ys, color, short_name, family, method: str="trapz"):
        self.name = name
        self.short_name = short_name
        self.family = family
        self.color = color
        self.xs = xs
        self.ys = ys
        self.method_name = "trapz" if method == "trapz" else "simp"
        self.auc = self.compute_auc(xs, ys, self.method_name)

    @staticmethod
    def compute_auc(xs: [float], ys: [float], method: str = "trapz", debug: bool = False):
        import numpy as np
        from scipy.integrate import simps
        from numpy import trapz

        # The y values.  A numpy array is used here,
        ys = np.asarray(ys, dtype=np.float32)
        auc = -1

        if method == "trapz":
            # Compute the area using the composite trapezoidal rule.
            auc = trapz(ys, dx=len(xs))
        elif method == "simp":
            # Compute the area using the composite Simpson's rule.
            auc = simps(ys, dx=len(xs))

        if debug:
            print(f'Computed AUC using {method}: {auc}')
        return auc

    def get_label(self, use_family: bool = False, use_short: bool = False):
        if use_family:
            return f'{self.family} - AUC: {str(round(self.auc, 2))}'
        if use_short:
            return f'{self.short_name} - AUC: {str(round(self.auc, 2))}'
        return f'{self.name} - AUC: {str(round(self.auc, 2))}'


