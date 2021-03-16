import time
from simulation import Simulation
from libs import _collect_sim_data, _collect_police_report


class Fitness:
    @staticmethod
    def evaluate(repetitions, deap_inds):
        individual = deap_inds[0]
        scores = []
        for _ in range(repetitions):
            crash_scenario, bng_roads, bng_vehicles = _collect_sim_data(individual)
            # Execute crash scenario and collect simulation's result
            simulation = Simulation(bng_roads, bng_vehicles)
            start_time = time.time()
            simulation.execute_scenario(time.time() + 60 * 1)
            print("Execution time: ", time.time() - start_time)
            crash_scenario.sim_report = simulation.get_report()

            # Fixed sample report data
            # TODO: change the sample police report to dynamic variable
            report_data = _collect_police_report("./data/sample_report.json")
            crash_scenario.cal_fitness(report_data)  # Calculate fitness score
            scores.append(crash_scenario.score)

        individual.simulation_results = scores
        return 0,
