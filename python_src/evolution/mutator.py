import numpy
import copy
from numpy.random import default_rng
from models.ac3rp import CrashScenario
from shapely.geometry import LineString, Point
from models.ac3rp import common, Vehicle
from stats.thinkstats2 import Pmf


def _mutate_val(value: float, params: dict):
    value += numpy.random.normal(params["mean"], params["std"], 1)[0]
    if value < params['min']:
        value = params['min']
    if value > params['max']:
        value = params['max']
    return value


def _mutate_speed(vehicle: Vehicle, mutate_speed_params: dict):
    mutated_speed = _mutate_val(vehicle.get_speed(), mutate_speed_params)  # 1 speed / 1 vehicle for all actions
    vehicle.movement.set_speed(mutated_speed)


def _mutate_initial_point(vehicle: Vehicle, mutate_point_params: dict):
    expected_distance = _mutate_val(0, mutate_point_params)
    vehicle_lst = LineString(vehicle.movement.get_driving_points())
    mutated_point = None
    while mutated_point is None:
        points = common.mutate_initial_point(lst=vehicle_lst,
                                             delta=vehicle.road_data["mutate_equation"],
                                             distance=expected_distance, num_points=1)
        # Since we have one generated point, so convert a new point to Point object
        point = Point(points[0][0], points[0][1])
        # Check if the new point stays in the vehicle road
        if common.is_inside_polygon(point, vehicle.road_data["road_poly"]):
            mutated_point = point

    # With a new origin, we can compute a new mutated driving actions (LineString) for vehicle
    mutated_driving_action = common.translate_ls_to_new_origin(lst=vehicle_lst,
                                                               new_origin=mutated_point)
    # Replace the old driving actions by a new one
    vehicle.movement.set_driving_actions(list(mutated_driving_action.coords))


class Mutator:
    @staticmethod
    def mutate_from(mutate_speed_params, mutate_point_params, deap_inds, distribution, v1_initial_points,
                    v2_initial_points):
        mutant = copy.deepcopy(deap_inds)
        individual: CrashScenario = mutant[0]  # deap_individual is a list
        score = deap_inds.fitness.values[0]
        pmf = Pmf(distribution)

        is_trigger_mutate_point = False
        if pmf[score] > 0.25:
            print(f'P({score}): {pmf[score]} > 0.25, so triggering the _mutate_initial_point()')
            is_trigger_mutate_point = True

        for vehicle in individual.vehicles:
            _mutate_speed(vehicle, mutate_speed_params)
            if is_trigger_mutate_point:
                _mutate_initial_point(vehicle, mutate_point_params)

            first_point = vehicle.movement.get_driving_points()[0]
            if vehicle.name == "v1":
                v1_initial_points.append(first_point)
            else:
                v2_initial_points.append(first_point)

        distribution.append(score)

        return mutant  # return deap_individual
