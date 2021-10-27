from models.mutator import Mutator
from models.ac3rp import Vehicle


class MutateSpeedClass(Mutator):
    """
    Concrete Mutator provide an implementations of the Speed Mutator interface.
    """

    def process(self, vehicle: Vehicle, is_random=False) -> Vehicle:
        # Mutate an average speed of given vehicle
        # 1 speed / 1 vehicle for all actions
        mutated_speed = self.random_value() if is_random else self.mutate_value(vehicle.get_speed())
        # Assign new speed
        vehicle.movement.set_speed(mutated_speed)
        return vehicle
