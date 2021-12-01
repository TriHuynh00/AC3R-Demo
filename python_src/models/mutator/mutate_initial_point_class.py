from models.mutator import Mutator
from models.ac3rp import Vehicle
from shapely.geometry import LineString, Point
from models.ac3rp import common


class MutateInitialPointClass(Mutator):
    """
    Concrete Mutator provide an implementations of the Initial Point Mutator interface.
    """

    def process(self, vehicle: Vehicle, is_random=False) -> Vehicle:
        # Not working for parked car
        if len(vehicle.movement.get_driving_actions()) == 1:
            return vehicle

        # Define an expected distance to move an initial point
        expected_distance = self.random_value() if is_random else self.mutate_value(0)
        vehicle_lst = LineString(vehicle.movement.get_driving_points())
        mutated_point = None

        # Run until point staying the road
        count_iteration = 0
        while mutated_point is None:
            points = common.mutate_initial_point(lst=vehicle_lst,
                                                 delta=vehicle.road_data["mutate_equation"],
                                                 distance=expected_distance, num_points=1)
            # Since we have one generated point, so convert a new point to Point object
            point = Point(points[0][0], points[0][1])
            # Check if the new point stays in the vehicle road
            if common.is_inside_polygon(point, vehicle.road_data["road_poly"]):
                mutated_point = point
            count_iteration += 1
            if count_iteration % 100 == 0:
                expected_distance = self.random_value() if is_random else self.mutate_value(0)
                print(f'MutateInitialPointClass object took {count_iteration} iterations to find the mutated point!')


        # With a new origin, we can compute a new mutated driving actions (LineString) for vehicle
        mutated_driving_action = common.translate_ls_to_new_origin(lst=vehicle_lst,
                                                                   new_origin=mutated_point)
        # Replace the old driving actions by a new one
        vehicle.movement.set_driving_actions(list(mutated_driving_action.coords))
        return vehicle
