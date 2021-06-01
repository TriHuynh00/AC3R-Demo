import matplotlib.pyplot as plt


class LogBook:
    def __init__(self, score: float):
        self.score = score

    @staticmethod
    def print_logbook(logbook):
        logbook.header = "gen", "evals", "fitness", "size"
        logbook.chapters["fitness"].header = "min", "avg", "max"
        print(logbook)

    def visualize_ind(self, logbook, label):
        gen = logbook.select("gen")
        fig, ax1 = plt.subplots()

        # First logbook
        fit_maxs = logbook.chapters["fitness"].select("max")
        line1 = ax1.plot(gen, fit_maxs, "b-", label=label)

        # Expectation
        line3 = ax1.plot(gen, [self.score for _ in range(len(gen))], "r-", label="Expectation")
        ax1.set_xlabel("Generation")
        ax1.set_ylabel("Fitness", color="b")
        ax1.set_xticks(gen)
        for tl in ax1.get_yticklabels():
            tl.set_color("b")

        lns = line1 + line3
        labs = [l.get_label() for l in lns]
        ax1.legend(lns, labs, loc="lower right")

        plt.show()

    def visualize(self, logbook_1, logbook_2, label_1, label_2):
        gen_1 = logbook_1.select("gen")
        gen_2 = logbook_2.select("gen")
        if len(gen_1) != len(gen_2):
            print(len(gen_1), len(gen_2))
            raise Exception("The number of generation is not compatible!")
        gen = gen_1
        fig, ax1 = plt.subplots()

        # First logbook
        fit_maxs = logbook_1.chapters["fitness"].select("max")
        line1 = ax1.plot(gen, fit_maxs, "b-", label=label_1)

        # Second logbook
        fit_maxs = logbook_2.chapters["fitness"].select("max")
        line2 = ax1.plot(gen, fit_maxs, "c-", label=label_2)

        # Expectation
        line3 = ax1.plot(gen, [self.score for _ in range(len(gen))], "r-", label="Expectation")
        ax1.set_xlabel("Generation")
        ax1.set_ylabel("Fitness", color="b")
        ax1.set_xticks(gen)
        for tl in ax1.get_yticklabels():
            tl.set_color("b")

        lns = line1 + line2 + line3
        labs = [l.get_label() for l in lns]
        ax1.legend(lns, labs, loc="lower right")

        plt.show()
