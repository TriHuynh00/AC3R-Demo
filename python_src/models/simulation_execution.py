import time
import traceback
from beamngpy import Scenario
from models import Simulation
from models.simulation_data import VehicleStateReader, SimulationDataCollector
from models.simulation_data import SimulationParams, SimulationDataContainer

CRASHED = 1
NO_CRASH = 0


class SimulationExec:
    def __init__(self, simulation: Simulation, is_birdview: bool = False):
        self.simulation = simulation
        self.is_birdview: bool = is_birdview

    def execute_scenario(self, timeout: int = 60):
        start_time = 0
        is_crash = False
        # Condition to start the 2nd vehicle after driving 1st for a while
        # -1: 1st and 2nd start at the same time
        distance_to_trigger = -1
        vehicleId_to_trigger = 0
        # Init BeamNG simulation
        bng_instance = self.simulation.init_simulation()
        scenario = Scenario("smallgrid", self.simulation.name)

        # Import roads from scenario obj to beamNG instance
        for road in self.simulation.roads:
            scenario.add_road(road)

        # Import vehicles from scenario obj to beamNG instance
        for player in self.simulation.players:
            scenario.add_vehicle(player.vehicle, pos=player.pos,
                                 rot=player.rot, rot_quat=player.rot_quat)

        # BeamNG scenario init
        scenario.make(bng_instance)
        bng_instance.open(launch=True)
        bng_instance.set_deterministic()
        bng_instance.remove_step_limit()

        # Prepare simulation data collection
        simulation_id = time.strftime('%Y-%m-%d--%H-%M-%S', time.localtime())
        simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', simulation_id)
        sim_data_collectors = SimulationDataContainer(debug=self.simulation.debug)
        for i in range(len(self.simulation.players)):
            player = self.simulation.players[i]
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

            # Enable bird view
            if self.is_birdview:
                self.simulation.enable_free_cam(bng_instance)

            # Drawing debug line and forcing vehicle moving by given trajectory
            idx = 0
            for player in self.simulation.players:
                road_pf = player.road_pf
                if player.distance_to_trigger > 0:
                    distance_to_trigger = player.distance_to_trigger
                    vehicleId_to_trigger = idx
                # ai_set_script not working for parking vehicle, so
                # the number of node from road_pf.script must > 2
                if len(road_pf.script) > 2:
                    self.simulation.trigger_vehicle(player)
                    bng_instance.add_debug_line(road_pf.points, road_pf.sphere_colors,
                                                spheres=road_pf.spheres, sphere_colors=road_pf.sphere_colors,
                                                cling=True, offset=0.1)
                idx += 1

            # We need to compute distance between vehicles if and only if one of two vehicle
            # has a distance_to_trigger property > 0
            # In addition, this variable will prevent the function keep running after 2nd car moving
            is_require_computed_distance = distance_to_trigger > -1
            # Update the vehicle information
            sim_data_collectors.start()
            start_time = time.time()

            # Begin a scenario
            while time.time() < (start_time + timeout):
                # Record the vehicle state for every 10 steps
                bng_instance.step(10, True)
                sim_data_collectors.collect()

                # Compute the distance between two vehicles
                if is_require_computed_distance:
                    distance_change = self.simulation.get_vehicles_distance(debug=self.simulation.debug)
                    # Trigger the 2nd vehicle
                    if self.simulation.trigger_vehicle(player=self.simulation.players[vehicleId_to_trigger],
                                                       distance_report=distance_change,
                                                       debug=self.simulation.debug):
                        is_require_computed_distance = False  # No need to compute distance anymore

                for player in self.simulation.players:
                    # Find the position of moving car
                    self.simulation.collect_vehicle_position(player)
                    # Collect the damage sensor information
                    vehicle = player.vehicle
                    # Check whether the imported vehicle existed in beamNG instance or not
                    if bool(bng_instance.poll_sensors(vehicle)) is False:
                        raise Exception("Exception: Vehicle not found in bng_instance!")
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:  # Crash detected
                        # Disable AI control
                        self.simulation.disable_vehicle_ai(vehicle)
                        is_crash = True

            sim_data_collectors.end(success=True)
            if not is_crash:
                print("Timed out!")
            else:
                status_players = [NO_CRASH] * len(self.simulation.players)  # zeros list e.g [0, 0]
                for i, player in enumerate(self.simulation.players):
                    vehicle = player.vehicle
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:
                        if not sensor['part_damage']:
                            # There is a case that a simulation reports a crash damage
                            # without any damaged components
                            # player.collect_damage({"etk800_any": {"name": "Any", "damage": 0}})
                            status_players[i] = NO_CRASH
                            print("Crash detected! But no broken component is specified!")
                        else:
                            status_players[i] = CRASHED
                            print("Crash detected!")
                            player.collect_damage(sensor['part_damage'])

                if CRASHED in status_players:  # [1, 0] or [0, 1]
                    self.simulation.status = CRASHED

            # Save the last position of vehicle
            for player in self.simulation.players:
                self.simulation.collect_vehicle_position(player)
        except Exception as ex:
            sim_data_collectors.save()
            sim_data_collectors.end(success=False, exception=ex)
            traceback.print_exception(type(ex), ex, ex.__traceback__)
            bng_instance.close()
        finally:
            sim_data_collectors.save()
            bng_instance.close()
            print("Simulation Time: ", time.time() - start_time)
