from beamngpy import Vehicle, BeamNGpy
from .simulation_data import SimulationParams, SimulationDataRecords, SimulationData, SimulationDataRecord
from simulation_data import VehicleStateReader


class SimulationDataCollector:
    def __init__(self, vehicle: Vehicle, beamng: BeamNGpy,
                 params: SimulationParams,
                 vehicle_state_reader: VehicleStateReader = None,
                 simulation_name: str = None):
        self.vehicle_state_reader = vehicle_state_reader if vehicle_state_reader \
            else VehicleStateReader(vehicle, beamng)
        self.beamng: BeamNGpy = beamng
        self.params: SimulationParams = params
        self.name = simulation_name
        self.states: SimulationDataRecords = []
        self.simulation_data: SimulationData = SimulationData(simulation_name)
        self.simulation_data.set(self.params, self.road, self.states)
        self.simulation_data.clean()

    def collect_current_data(self):
        """If oob_bb is True, then the out-of-bound (OOB) examples are calculated
        using the bounding box of the car."""
        self.vehicle_state_reader.update_state()
        car_state = self.vehicle_state_reader.get_state()

        sim_data_record = SimulationDataRecord(**car_state._asdict())
        self.states.append(sim_data_record)

    def get_simulation_data(self) -> SimulationData:
        return self.simulation_data

    def save(self):
        self.simulation_data.save()
