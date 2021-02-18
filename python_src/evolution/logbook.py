import matplotlib.pyplot as plt


class LogBook:
    def __init__(self, logbook, expectations):
        self.logbook = logbook
        self.expectations = expectations

    def print_logbook(self):
        self.logbook.header = "gen", "evals", "fitness", "size"
        self.logbook.chapters["fitness"].header = "min", "avg", "max"
        print(self.logbook)

    def visualize_evolution(self):
        gen = self.logbook.select("gen")
        fit_maxs = self.logbook.chapters["fitness"].select("max")

        fig, ax1 = plt.subplots()
        line1 = ax1.plot(gen, fit_maxs, "b-", label="Fitness")
        line3 = ax1.plot(gen, self.expectations(len(gen)), "r-", label="Expectation")
        ax1.set_xlabel("Generation")
        ax1.set_ylabel("Fitness", color="b")
        ax1.set_xticks(gen)
        for tl in ax1.get_yticklabels():
            tl.set_color("b")

        lns = line1 + line3
        labs = [l.get_label() for l in lns]
        ax1.legend(lns, labs, loc="lower right")

        plt.show()