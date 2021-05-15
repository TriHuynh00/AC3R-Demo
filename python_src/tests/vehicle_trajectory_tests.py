import unittest
import json
import matplotlib.pyplot as plt

from models.ac3rp import Road, Vehicle
from visualization import RoadVisualizer, VehicleTrajectoryVisualizer

class VehicleTrajectoryTest(unittest.TestCase):

    def test_generate_and_visualize_simple_trajectory(self):

        # Setup the figure
        map_size = 150
        the_figure = plt.figure()
        plt.gca().set_aspect('equal', 'box')
        plt.gca().set(xlim=(-30, map_size + 30), ylim=(-30, map_size + 30))

        # Create a Vehicle object that contains the trajectory
        name = "v1"
        initial_location = (10, 10)
        initial_rotation = 0
        driving_actions = []
        driving_actions.append(
            {'name': 'follow',
             'trajectory_segments':
                 [
                     {'type': 'straight', 'length': 20.0}
                 ]
             }
        )
        # RIGHT TURNS have NEGATIVE ANGLES
        driving_actions.append(
            {'name': 'turn-right',
             'trajectory_segments':
                 [
                     {'type': 'turn', 'angle': -90.0, 'radius': 10.0}
                 ]
             }
        )
        # LEFT TURNS have POSITIVE ANGLES
        driving_actions.append(
            {'name': 'turn-left',
             'trajectory_segments':
                 [
                     {'type': 'turn', 'angle': +90.0, 'radius': 10.0}
                 ]
             }
        )

        driving_actions.append(
            {'name': 'follow',
             'trajectory_segments':
                 [
                     {'type': 'straight', 'length': 16.0}
                 ]
             }
        )

        # driving_actions.append(
        #     {'name': 'turn-right',
        #      'trajectory_segments':
        #          [
        #              {'type': 'turn', 'angle': +90.0, 'radius': 10.0}
        #          ]
        #      }
        # )
        #
        # driving_actions.append(
        #     {'name': 'follow',
        #      'trajectory_segments':
        #          [
        #              {'type': 'straight', 'length': 16.0}
        #          ]
        #      }
        # )
        vehicle = Vehicle(name, initial_location, initial_rotation, driving_actions)

        # Visualize the trajectory
        vv = VehicleTrajectoryVisualizer(the_figure)
        vv.visualize(vehicle)
        plt.show()