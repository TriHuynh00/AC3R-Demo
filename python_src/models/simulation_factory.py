import beamngpy
import matplotlib.colors as colors
from typing import List, Tuple
from models import RoadProfiler, Player
from models.ac3rp import CrashScenario


class SimulationFactory:
    def __init__(self, scenario: CrashScenario):
        self.scenario: CrashScenario = scenario
        self.roads: List[beamngpy.Road] = []
        self.players: List[Player] = []

    def set_scenario(self, scenario: CrashScenario):
        self.scenario = scenario

    def get_center_scenario(self) -> Tuple:
        min_x, min_y, max_x, max_y = None, None, None, None
        for seg in self.scenario.roads:
            lines = [seg.road_nodes, seg.left_line.nodes, seg.right_line.nodes]
            lines.extend([l.nodes for l in seg.middle_lines])

            for line in lines:
                for p in line:
                    x, y = p[0], p[1]
                    if min_x is None and min_y is None and max_x is None and max_y is None:
                        min_x = x
                        max_x = x
                        min_y = y
                        max_y = y
                    else:
                        min_x = x if x < min_x else min_x
                        max_x = x if x > max_x else max_x
                        min_y = y if y < min_y else min_y
                        max_y = y if y > max_y else max_y

        return (max_x - min_x) / 2, (max_y - min_y) / 2, max(max_x, max_y)

    def generate_roads(self) -> List[beamngpy.Road]:
        for segment in self.scenario.roads:
            left_marking = beamngpy.Road("line_white", rid=segment.left_line.name)
            left_marking.nodes.extend(segment.left_line.nodes)
            self.roads.append(left_marking)

            right_marking = beamngpy.Road("line_white", rid=segment.right_line.name)
            right_marking.nodes.extend(segment.right_line.nodes)
            self.roads.append(right_marking)

            for m in segment.middle_lines:
                marking = beamngpy.Road("line_yellow", rid=m.name)
                marking.nodes.extend(m.nodes)
                self.roads.append(marking)

            sim_road = beamngpy.Road('road_asphalt_2lane', rid=segment.name)
            sim_road.nodes.extend(segment.road_nodes)
            self.roads.append(sim_road)
        return self.roads

    def generate_players(self) -> List[Player]:
        for vehicle in self.scenario.vehicles:
            trajectory = vehicle.trajectory
            initial_position = (trajectory[0][0], trajectory[0][1], 0)
            # Create BeamNG Vehicle for simulation
            sim_vehicle = beamngpy.Vehicle(str(vehicle.name),
                                           model="etk800",
                                           licence=vehicle.name,
                                           color=' '.join([str(x) for x in colors.to_rgb(vehicle.color)]))
            sim_vehicle.attach_sensor('damage', beamngpy.sensors.Damage())

            # Create road profiler to drive BeamNG vehicle
            road_pf = RoadProfiler()
            road_pf.compute_ai_script(trajectory, vehicle.color)

            # Create a mask vehicle to collect data later
            self.players.append(Player(vehicle=sim_vehicle,
                                       road_pf=road_pf,
                                       pos=initial_position,
                                       rot=vehicle.initial_rotation,
                                       rot_quat=None,
                                       distance_to_trigger=vehicle.distance_to_trigger,
                                       speed=vehicle.speed))
        return self.players

    def generate_targets(self) -> {}:
        data_target = {}
        for report in self.scenario.reports:
            data_target[report.name] = report.parts
        return data_target

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)
