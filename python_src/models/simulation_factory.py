import beamngpy
from typing import List
from models import RoadProfiler, Player
from models.ac3rp import CrashScenario


class SimFactory:
    def __init__(self, scenario: CrashScenario):
        self.scenario: CrashScenario = scenario
        self.roads: List[beamngpy.Road] = []
        self.players: List[Player] = []

    def generate_roads(self) -> List[beamngpy.Road]:
        for road in self.scenario.roads:
            sim_road = beamngpy.Road('road_asphalt_2lane', rid=road.name)
            sim_road.nodes.extend(road.road_nodes)
            self.roads.append(sim_road)
        return self.roads

    def generate_players(self) -> List[Player]:
        for vehicle in self.scenario.vehicles:
            trajectory = vehicle.generate_trajectory()
            initial_position = (trajectory[0][0], trajectory[0][1], 0)
            # Create BeamNG Vehicle for simulation
            sim_vehicle = beamngpy.Vehicle(str(vehicle.name),
                                           model="etk800",
                                           licence=vehicle.name,
                                           color=vehicle.color)
            sim_vehicle.attach_sensor('damage', beamngpy.sensors.Damage())

            # Create road profiler to drive BeamNG vehicle
            road_pf = RoadProfiler()
            road_pf.compute_ai_script(trajectory, vehicle.color)

            # Create a mask vehicle to collect data later
            self.players.append(Player(vehicle=sim_vehicle,
                                       road_pf=road_pf,
                                       pos=initial_position,
                                       rot=None, rot_quat=vehicle.rot_quat))
        return self.players

    def generate_targets(self) -> {}:
        data_target = {}
        for report in self.scenario.reports:
            data_target[report.name] = report.parts
        return data_target
