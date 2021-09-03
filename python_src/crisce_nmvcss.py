import time
from beamngpy import BeamNGpy, Scenario
import matplotlib.pyplot as plt
import os
import json
from descartes import PolygonPatch
from shapely.geometry import LineString
from pathlib import Path



class Car:
    def __init__(self, id, name):
        self.id = id
        self.name = name
        self.trajectories = []

    def set_trajectories(self, point):
        self.trajectories.append(point)


if __name__ == '__main__':
    AC3R_PATH = "C:\\Users\\harve\\Documents\\NMVCSS"
    CRISCE_ROAD_PATH = "C:\\Users\\harve\\Documents\\BeamNG.research\\levels\\smallgrid\\scenarios"
    CRISCE_VEHICLE_PATH = "C:\\Users\\harve\\Documents\\NMVCCS_vehicles"
    files = os.listdir(AC3R_PATH)
    ac3r_cases = []
    for i, f in enumerate(files):
        case_name = f.split(".")[0]
        if case_name not in ac3r_cases:
            ac3r_cases.append(case_name)

    crisce_cases = []
    for ac3r_case in ac3r_cases:
        case_name = ac3r_case.split('_')[-1]
        if case_name not in crisce_cases:
            crisce_cases.append([ac3r_case, case_name])

    for case in crisce_cases:
        ac3r_case, case_name = case[0], case[1]
        road_path = f'{CRISCE_ROAD_PATH}\\{case_name}.prefab'
        vehicle_path = f'{CRISCE_VEHICLE_PATH}\\{case_name}\\output.json'
        try:
            if Path(road_path).is_file():
                pass
            with open(vehicle_path) as file_vehicle:
                csc_vehicle = json.load(file_vehicle)
            ROADS = []
            timeout = 10
            start_time = 0
            is_crash = False
            bng_home = os.getenv('BNG_HOME')
            bng_research = os.getenv('BNG_RESEARCH')
            host = "127.0.0.1"
            port = 64257
            bng_instance = BeamNGpy(host, port, bng_home, bng_research)

            scenario = Scenario("smallgrid", case_name)
            scenario.find(bng_instance)
            bng_instance.open(launch=True)
            bng_instance.set_deterministic()
            bng_instance.remove_step_limit()
            bng_instance.load_scenario(scenario)
            for dr in bng_instance.find_objects_class("DecalRoad"):
                road = Car(dr.id, dr.name)
                for point in bng_instance.get_road_edges(dr.name):
                    road.set_trajectories([point["middle"][0], point["middle"][1]])
                ROADS.append(road)
            try:
                bng_instance.start_scenario()
                start_time = time.time()
                while time.time() < (start_time + timeout):
                    bng_instance.step(50, True)
            except Exception as ex:
                bng_instance.close()
            finally:
                bng_instance.close()

            roads = []
            for road in ROADS:
                roads.append({"id": road.id, "name": road.name, "trajectory_points": road.trajectories})
            toJSON = {"roads": roads}

            fig = plt.figure()
            dist_x, dist_y = 0, 0

            for color in ["red", "blue"]:
                csc_cp = csc_vehicle["vehicles"][color]["crash_point"]["coordinates"]
                aspect_ratio = csc_vehicle["vehicles"][color]["dimensions"]["car_length_sim"] / \
                               csc_vehicle["vehicles"][color]["dimensions"][
                                   "car_length"]
                csc_cp = [csc_cp[0] * aspect_ratio, csc_cp[1] * aspect_ratio]
                # assign coordinates
                dist_x = - csc_cp[0]
                dist_y = - csc_cp[1]

            for i, road in enumerate(toJSON["roads"]):
                trajectory_points = road["trajectory_points"]
                road_width = 8
                road_poly = LineString([(t[0] + dist_x, t[1] + dist_y) for t in trajectory_points]).buffer(road_width,
                                                                                                    cap_style=2,
                                                                                                    join_style=2)
                road_patch = PolygonPatch(road_poly, fc='gray', ec='dimgray')  # ec='#555555', alpha=0.5, zorder=4)
                plt.gca().add_patch(road_patch)
                xs = [p[0] + dist_x for p in trajectory_points]
                ys = [p[1] + dist_y for p in trajectory_points]
                plt.plot(xs, ys, '-', color="#9c9c9c")

            for color in ["red", "blue"]:
                v1_line = csc_vehicle["vehicles"][color]["trajectories"]["simulation_trajectory"]
                xs = [p[0] + dist_x for p in v1_line]
                ys = [p[1] + dist_y for p in v1_line]
                plt.plot(xs, ys, '.-', label=f'Vehicle {color}', color=color)

            plt.legend()
            plt.gca().set_aspect("equal")
            plt.xlim([-100, 100])
            plt.ylim([-100, 100])
            plt.title(f'CRISCEs {case_name}')
            plt.show()
            fig.savefig(f'{ac3r_case}_crisce.png')
            print(f'CRISCE {case_name} - {ac3r_case} plotting is successful!')
        except Exception as e:
            print(f'AC3R case {ac3r_case} is not found in CRISCE NMVCSS dataset!')

        print("==================")
