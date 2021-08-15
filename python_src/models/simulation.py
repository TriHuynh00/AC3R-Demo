import os
import time
import beamngpy
import traceback
import models
from typing import List
from beamngpy import BeamNGpy, Scenario
from models import SimulationFactory
from models.simulation_data import VehicleStateReader, SimulationDataCollector, SimulationParams, SimulationDataContainer

CRASHED = 1
NO_CRASH = 0


class Simulation:
    def __init__(self, sim_factory: SimulationFactory, file_name=None, debug: bool = False):
        self.roads: List[beamngpy.Road] = sim_factory.generate_roads()
        self.players: List[models.Player] = sim_factory.generate_players()
        self.targets: {} = sim_factory.generate_targets()
        self.status: int = NO_CRASH
        self.debug: bool = debug
        self.file_name: str = file_name

    @staticmethod
    def init_simulation() -> BeamNGpy:
        bng_home = os.getenv('BNG_HOME')
        bng_research = os.getenv('BNG_RESEARCH')
        host = '127.0.0.1'
        port = 64257
        return BeamNGpy(host, port, bng_home, bng_research)

    @staticmethod
    def disable_vehicle_ai(vehicle: beamngpy.vehicle):
        vehicle.ai_set_mode('disable')
        vehicle.ai_set_speed(20 / 3.6, 'set')
        vehicle.control(throttle=0, steering=0, brake=0, parkingbrake=0)
        vehicle.update_vehicle()

    @staticmethod
    def collect_vehicle_position(player: models.Player) -> models.Player:
        vehicle = player.vehicle
        current_position = (vehicle.state['pos'][0], vehicle.state['pos'][1])
        player.collect_positions(current_position)

        return player

    def get_data_outputs(self) -> {}:
        data_outputs = {}
        for player in self.players:
            data_outputs[player.vehicle.vid] = player.get_damage()
        return data_outputs

    def execute_scenario(self, timeout: int = 60):
        def getBboxRect(vehicle):
            bbox = vehicle.get_bbox()
            boundary_x = [
                bbox['front_bottom_left'][0],
                bbox['front_bottom_right'][0],
                bbox['rear_bottom_right'][0],
                bbox['rear_bottom_left'][0],
            ]
            boundary_y = [
                bbox['front_bottom_left'][1],
                bbox['front_bottom_right'][1],
                bbox['rear_bottom_right'][1],
                bbox['rear_bottom_left'][1],
            ]
            return bbox, boundary_x, boundary_y

        start_time = 0
        # Init BeamNG simulation
        bng_instance = self.init_simulation()
        scenario = Scenario("smallgrid", "test_01")

        for road in self.roads:
            scenario.add_road(road)

        for player in self.players:
            scenario.add_vehicle(player.vehicle, pos=player.pos,
                                 rot=player.rot, rot_quat=player.rot_quat)

        scenario.make(bng_instance)
        bng_instance.open(launch=True)
        bng_instance.set_deterministic()
        bng_instance.remove_step_limit()

        is_crash = False
        simulation_id = time.strftime('%Y-%m-%d--%H-%M-%S', time.localtime())
        simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', simulation_id)
        sim_data_collectors = SimulationDataContainer(debug=self.debug)
        for i in range(len(self.players)):
            player = self.players[i]
            vehicle_state = VehicleStateReader(player.vehicle, bng_instance)
            sim_data_collectors.append(
                SimulationDataCollector(player.vehicle,
                                        bng_instance,
                                        SimulationParams(beamng_steps=50,
                                                         delay_msec=int(25 * 0.05 * 1000)),
                                        vehicle_state_reader=vehicle_state,
                                        simulation_name=simulation_name + "_v" + str(i + 1))
            )
        try:
            bng_instance.load_scenario(scenario)
            bng_instance.start_scenario()

            # Drawing debug line and forcing vehicle moving by given trajectory
            for player in self.players:
                vehicle = player.vehicle
                road_pf = player.road_pf
                # ai_set_script not working for parking vehicle
                if len(road_pf.script) > 2:
                    bng_instance.add_debug_line(road_pf.points, road_pf.sphere_colors,
                                                spheres=road_pf.spheres, sphere_colors=road_pf.sphere_colors,
                                                cling=True, offset=0.1)
                    vehicle.ai_set_mode('manual')
                    vehicle.ai_set_script(road_pf.script, cling=False)

            # Update the vehicle information
            sim_data_collectors.start()
            start_time = time.time()
            while time.time() < (start_time + timeout):
                # Record the vehicle state for every 50 steps
                bng_instance.step(50, True)
                sim_data_collectors.collect()

                for player in self.players:
                    player.final_bboxes.append(getBboxRect(player.vehicle)[0])
                    # Find the position of moving car
                    self.collect_vehicle_position(player)
                    # Collect the damage sensor information
                    vehicle = player.vehicle
                    if bool(bng_instance.poll_sensors(vehicle)) is False:
                        raise Exception("Vehicle not found in bng_instance")
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:  # Crash detected
                        # Disable AI control
                        self.disable_vehicle_ai(vehicle)
                        is_crash = True

            sim_data_collectors.end(success=True)
            if not is_crash:
                print("Timed out!")
            else:
                self.status = CRASHED
                for player in self.players:
                    vehicle = player.vehicle
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:
                        if not sensor['part_damage']:
                            # There is a case that a simulation reports a crash damage
                            # without any damaged components
                            # player.collect_damage({"etk800_any": {"name": "Any", "damage": 0}})
                            self.status = NO_CRASH
                            print("Crash detected! But no broken component is specified!")
                        else:
                            print("Crash detected!")
                            player.collect_damage(sensor['part_damage'])

            # Save the last position of vehicle
            for player in self.players:
                self.collect_vehicle_position(player)

            if self.file_name is not None:
                toCSV = []
                for player in self.players:
                    v = dict.fromkeys(['vid', 'vehicle', 'initial_position', 'initial_direction', 'init_bbox',
                                       'final_bboxes', 'crashed_happened', 'simulation_damage', 'crash_side_generic',
                                       'crash_side_specific', 'crash_impact_match', 'crash_side_match',
                                       'crash_impact_hits', 'crash_impact_miss', 'missed_values', 'out_of_bound',
                                       'pos_sim', 'orient_sim', 'cum_iou', 'cum_iou_error', 'displacement_error'])
                    v["vid"] = player.vehicle.vid
                    v["final_bboxes"] = player.final_bboxes
                    v["initial_position"] = player.positions[0]
                    v["crashed_happened"] = is_crash
                    v["simulation_damage"] = player.damage
                    toCSV.append(v)

                import csv
                import json
                keys = toCSV[0].keys()
                fn = self.file_name.split('/')[-1].split('.')[0]
                with open(fn + ".csv", 'w', newline='') as output_file:
                    dict_writer = csv.DictWriter(output_file, keys)
                    dict_writer.writeheader()
                    dict_writer.writerows(toCSV)
                with open(fn + ".json", "w") as output_file:
                    json.dump(toCSV, output_file)

        except Exception as ex:
            sim_data_collectors.save()
            sim_data_collectors.end(success=False, exception=ex)
            traceback.print_exception(type(ex), ex, ex.__traceback__)
            bng_instance.close()
        finally:
            sim_data_collectors.save()
            bng_instance.close()
            print("Simulation Time: ", time.time() - start_time)
