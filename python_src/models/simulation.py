import os
import time
from models.sim_factory import SimFactory
from beamngpy import BeamNGpy, Scenario
from models.simulation_data import VehicleStateReader, SimulationDataCollector, SimulationParams, \
    SimulationDataContainer
import traceback

CRASHED = 1
NO_CRASH = 0


class SimulationReport:
    def __init__(self, bng_vehicles, status):
        self.vehicles = bng_vehicles
        self.status = status

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)


class Simulation:
    def __init__(self, sim_factory: SimFactory, debug: bool = False):
        self.bng_roads = sim_factory.generate_roads()
        self.bng_vehicles = sim_factory.generate_vehicles()
        self.status = NO_CRASH
        self.debug = debug

    @staticmethod
    def init_simulation():
        bng_home = os.getenv('BNG_HOME')
        bng_research = os.getenv('BNG_RESEARCH')
        host = '127.0.0.1'
        port = 64257
        return BeamNGpy(host, port, bng_home, bng_research)

    @staticmethod
    def disable_vehicle_ai(vehicle):
        vehicle.ai_set_mode('disable')
        vehicle.ai_set_speed(20 / 3.6, 'set')
        vehicle.control(throttle=0, steering=0, brake=0, parkingbrake=0)
        vehicle.update_vehicle()

    @staticmethod
    def collect_vehicle_position(bng_vehicle):
        vehicle = bng_vehicle.vehicle
        current_position = (vehicle.state['pos'][0], vehicle.state['pos'][1])
        bng_vehicle.collect_positions(current_position)

        return bng_vehicle

    def get_report(self):
        return SimulationReport(self.bng_vehicles, self.status)

    def execute_scenario(self, timeout=None):
        bng_roads = self.bng_roads
        bng_vehicles = self.bng_vehicles
        # Init BeamNG simulation
        bng_instance = self.init_simulation()
        simulation_id = time.strftime('%Y-%m-%d--%H-%M-%S', time.localtime())
        scenario = Scenario('smallgrid', simulation_id)

        for road in bng_roads:
            scenario.add_road(road)

        for bng_vehicle in bng_vehicles:
            scenario.add_vehicle(bng_vehicle.vehicle, pos=bng_vehicle.pos,
                                 rot=bng_vehicle.rot, rot_quat=bng_vehicle.rot_quat)

        scenario.make(bng_instance)
        bng_instance.open(launch=True)
        bng_instance.set_deterministic()

        if timeout is None:
            # 45 seconds for each scenario
            timeout = time.time() + 45
        is_crash = False

        simulation_name = 'beamng_executor/sim_$(id)'.replace('$(id)', simulation_id)
        sim_data_collectors = SimulationDataContainer(debug=self.debug)
        for i in range(len(self.bng_vehicles)):
            bng_vehicle = self.bng_vehicles[i]
            vehicle_state = VehicleStateReader(bng_vehicle.vehicle, bng_instance)
            sim_data_collectors.append(
                SimulationDataCollector(bng_vehicle.vehicle,
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
            for bng_vehicle in bng_vehicles:
                vehicle = bng_vehicle.vehicle
                road_pf = bng_vehicle.road_pf
                # ai_set_script not working for parking vehicle
                if len(road_pf.script) > 2:
                    bng_instance.add_debug_line(road_pf.points, road_pf.sphere_colors,
                                                spheres=road_pf.spheres, sphere_colors=road_pf.sphere_colors,
                                                cling=True, offset=0.1)
                    vehicle.ai_set_mode('manual')
                    vehicle.ai_set_script(road_pf.script, cling=False)

            # Update the vehicle information
            sim_data_collectors.start()
            while time.time() < timeout:
                # Record the vehicle state for every 250ms
                bng_instance.step(25, True)
                sim_data_collectors.collect()

                for bng_vehicle in bng_vehicles:
                    # Find the position of moving car
                    self.collect_vehicle_position(bng_vehicle)
                    # Collect the damage sensor information
                    vehicle = bng_vehicle.vehicle
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
                for bng_vehicle in bng_vehicles:
                    vehicle = bng_vehicle.vehicle
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:
                        bng_vehicle.collect_damage(sensor['part_damage'])

            # Save the last position of vehicle
            for bng_vehicle in bng_vehicles:
                self.collect_vehicle_position(bng_vehicle)
        except Exception as ex:
            sim_data_collectors.save()
            sim_data_collectors.end(success=False, exception=ex)
            traceback.print_exception(type(ex), ex, ex.__traceback__)
            bng_instance.close()
        finally:
            sim_data_collectors.save()
            bng_instance.close()
