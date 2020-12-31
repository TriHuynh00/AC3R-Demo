import unittest
import json
import matplotlib.pyplot as plt

from ac3r_plus import Road, Vehicle
from visualization import RoadVisualizer, VehicleTrajectoryVisualizer

class RoadVisualizationTest(unittest.TestCase):

    # TODO FIX SIMPLE as each driving action is a LIST of segments, not just one segment !!!

    def test_load_roads_from_simple_and_visualize_them(self):

        simple_file = "./data/simple.json"

        with open(simple_file, 'r') as input_file:
            scenario_data = json.load(input_file)

        roads = []
        for road_dict in scenario_data["roads"]:
            roads.append(Road.from_dict(road_dict))

        # Setup the figure
        map_size = 150
        the_figure = plt.figure()
        plt.gca().set_aspect('equal', 'box')
        plt.gca().set(xlim=(-30, map_size + 30), ylim=(-30, map_size + 30))

        # Visualize the roads
        rv = RoadVisualizer(the_figure)

        rv.visualize(roads[0])
        rv.visualize(roads[1])

        plt.show()

    def test_load_vehicles_from_simple_and_visualize_them(self):

        ## TODO Test this code by starting the car in a TURN !

        simple_file = "./data/simple.json"

        with open(simple_file, 'r') as input_file:
            scenario_data = json.load(input_file)

        vehicle = []
        for vehicle_dict in scenario_data["vehicles"]:
            vehicle.append(Vehicle.from_dict(vehicle_dict))

        # # Setup the figure
        map_size = 150
        the_figure = plt.figure()
        plt.gca().set_aspect('equal', 'box')
        plt.gca().set(xlim=(-30, map_size + 30), ylim=(-30, map_size + 30))

        # Visualize the car trajectory (And defining points if necessary)
        vv = VehicleTrajectoryVisualizer(the_figure)

        vv.visualize(vehicle[0])
        vv.visualize(vehicle[1])

        plt.show()

