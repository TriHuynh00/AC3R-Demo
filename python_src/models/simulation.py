import os
import time
import beamngpy
import traceback
import models
from typing import List
from beamngpy import BeamNGpy, Scenario
from models.sim_factory import SimFactory
from models.simulation_data import VehicleStateReader, SimulationDataCollector, SimulationParams, SimulationDataContainer

CRASHED = 1
NO_CRASH = 0


class SimulationReport:
    def __init__(self, vehicles: List[models.SimVehicle], status: int):
        self.vehicles = vehicles
        self.status = status

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)


class Simulation:
    def __init__(self, sim_factory: SimFactory, debug: bool = False):
        self.roads = sim_factory.generate_roads()
        self.vehicles = sim_factory.generate_vehicles()
        self.status = NO_CRASH
        self.debug = debug

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
    def collect_vehicle_position(sim_vehicle: models.sim_vehicle) -> models.sim_vehicle:
        vehicle = sim_vehicle.vehicle
        current_position = (vehicle.state['pos'][0], vehicle.state['pos'][1])
        sim_vehicle.collect_positions(current_position)

        return sim_vehicle

    def get_report(self) -> SimulationReport:
        return SimulationReport(self.vehicles, self.status)

    def execute_scenario(self, timeout: int = 60):
        start_time = 0
        # Init BeamNG simulation
        bng_instance = self.init_simulation()
        scenario = Scenario("smallgrid", "test_01")

        for road in self.roads:
            scenario.add_road(road)

        for sim_vehicle in self.vehicles:
            scenario.add_vehicle(sim_vehicle.vehicle, pos=sim_vehicle.pos,
                                 rot=sim_vehicle.rot, rot_quat=sim_vehicle.rot_quat)

        scenario.make(bng_instance)
        bng_instance.open(launch=True)
        bng_instance.set_deterministic()
        bng_instance.remove_step_limit()

        is_crash = False
        simulation_id = time.strftime('%Y-%m-%d--%H-%M-%S', time.localtime())
        simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', simulation_id)
        sim_data_collectors = SimulationDataContainer(debug=self.debug)
        for i in range(len(self.vehicles)):
            sim_vehicle = self.vehicles[i]
            vehicle_state = VehicleStateReader(sim_vehicle.vehicle, bng_instance)
            sim_data_collectors.append(
                SimulationDataCollector(sim_vehicle.vehicle,
                                        bng_instance,
                                        SimulationParams(beamng_steps=25,
                                                         delay_msec=int(25 * 0.05 * 1000)),
                                        vehicle_state_reader=vehicle_state,
                                        simulation_name=simulation_name + "_v" + str(i + 1))
            )
        try:
            bng_instance.load_scenario(scenario)
            bng_instance.start_scenario()

            # Drawing debug line and forcing vehicle moving by given trajectory
            for sim_vehicle in self.vehicles:
                vehicle = sim_vehicle.vehicle
                road_pf = sim_vehicle.road_pf
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

                for sim_vehicle in self.vehicles:
                    # Find the position of moving car
                    self.collect_vehicle_position(sim_vehicle)
                    # Collect the damage sensor information
                    vehicle = sim_vehicle.vehicle
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
                print("Crash detected!")
                self.status = CRASHED
                for sim_vehicle in self.vehicles:
                    vehicle = sim_vehicle.vehicle
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:
                        sim_vehicle.collect_damage(sensor['part_damage'])

            # Save the last position of vehicle
            for sim_vehicle in self.vehicles:
                self.collect_vehicle_position(sim_vehicle)
        except Exception as ex:
            sim_data_collectors.save()
            sim_data_collectors.end(success=False, exception=ex)
            traceback.print_exception(type(ex), ex, ex.__traceback__)
            bng_instance.close()
        finally:
            sim_data_collectors.save()
            bng_instance.close()
            print("Simulation Time: ", time.time() - start_time)
