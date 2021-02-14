import os
import time
from beamngpy import BeamNGpy, Scenario

CRASHED = 1
NO_CRASH = 0


class SimulationReport:
    def __init__(self, bng_vehicles, status):
        self.vehicles = bng_vehicles
        self.status = status

    def __str__(self):
        return str(self.__class__) + ": " + str(self.__dict__)


class Simulation:
    def __init__(self, bng_roads, bng_vehicles):
        self.bng_roads = bng_roads
        self.bng_vehicles = bng_vehicles
        self.status = NO_CRASH

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
        scenario = Scenario('smallgrid', 'test_01')

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
            while time.time() < timeout:
                bng_instance.step(1)
                for bng_vehicle in bng_vehicles:
                    # Find the position of moving car
                    self.collect_vehicle_position(bng_vehicle)

                    # Collect the damage sensor information
                    vehicle = bng_vehicle.vehicle
                    sensor = bng_instance.poll_sensors(vehicle)['damage']
                    if sensor['damage'] != 0:  # Crash detected
                        # Disable AI control
                        self.disable_vehicle_ai(vehicle)
                        is_crash = True

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
            bng_instance.close()
            bng_instance.kill_beamng()
        finally:
            bng_instance.close()
