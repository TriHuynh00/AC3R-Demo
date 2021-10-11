from models.mutator import Mutator
from models.ac3rp import Vehicle


class MutatorTypeA(Mutator):
    """
    Concrete Mutator provide an implementations of the Speed Mutator interface.
    """

    def process(self, vehicle: Vehicle) -> Vehicle:
        # Mutate an average speed of given vehicle
        mutated_speed = self.mutate_value(vehicle.get_speed())  # 1 speed / 1 vehicle for all actions
        # Assign new speed
        vehicle.movement.set_speed(mutated_speed)
        return vehicle
