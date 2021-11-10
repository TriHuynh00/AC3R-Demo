import numpy
from models import SimulationFactory, SimulationScore, SimulationExec
from models.simulation import Simulation
from models.ac3rp import CrashScenario


def _write_log_file(simulation: Simulation, simulation_score: SimulationScore, fn,
                    scenario: CrashScenario, ex_mes=None):
    toCSV = []
    s = dict.fromkeys(["speeds", "initial_position", "vehicles_dam", "sim_dam",
                       "crashed_happened", "sim_score", "expected_score", "exception",
                       "vehicles_damage_full", "sim_damage_full"])
    s["speeds"], s["vehicles_damage_full"], s["vehicles_dam"], s["sim_dam"], s["initial_position"] = [], [], [], [], []
    for player in simulation.players:
        s["speeds"].append({player.vehicle.vid: player.speed})
        s["vehicles_damage_full"].append({player.vehicle.vid: player.get_damage()})
        s["vehicles_dam"].append({player.vehicle.vid: [part["name"] for part in player.get_damage()]})
    s["sim_damage_full"] = simulation.get_data_outputs()
    for vehicle in scenario.vehicles:
        s["initial_position"].append({vehicle.name: vehicle.movement.get_driving_points()[0]})
    for key, value in simulation.get_data_outputs().items():
        s["sim_dam"].append({key: [part["name"] for part in value]})
    s["crashed_happened"] = simulation.status
    s["sim_score"] = simulation_score.simulation_score
    s["expected_score"] = simulation_score.expected_score
    s['exception'] = None if ex_mes == "" else ex_mes
    toCSV.append(s)
    keys = toCSV[0].keys()

    import csv
    import os
    isFileExist = os.path.exists(fn)
    with open(fn, 'a' if isFileExist else 'w') as f:
        if isFileExist:
            dict_writer = csv.DictWriter(f, keys)
            dict_writer.writerows(toCSV)
        else:
            dict_writer = csv.DictWriter(f, keys)
            dict_writer.writeheader()
            dict_writer.writerows(toCSV)


class Fitness:
    @staticmethod
    def evaluate(repetitions: int, log_data_file, deap_inds):
        individual: CrashScenario = deap_inds[0]
        scores = []
        for _ in range(repetitions):
            sim_factory = SimulationFactory(individual)
            simulation = Simulation(sim_factory=sim_factory)
            simulation_score = SimulationScore(simulation)

            # Execute scenario
            SimulationExec(simulation).execute_scenario(timeout=40)
            scores.append(simulation_score.calculate())  # get the score

            # Logging
            simulation_score.get_expected_score()
            _write_log_file(fn=log_data_file,
                            simulation=simulation,
                            simulation_score=simulation_score,
                            scenario=individual)
        individual.scores = scores
        print(f'Scores: {scores}')
        return numpy.mean(scores),
