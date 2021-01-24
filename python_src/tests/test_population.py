import unittest
from fitness import Population, Individual
import json
from ac3r_plus import CrashScenario
from models import RoadProfiler, ScriptFactory, BNGVehicle
from simulation import Simulation
from beamngpy import Road, Vehicle
from beamngpy.sensors import Damage

individual_1 = Individual({"status": 0, "distance": 30})
individual_2 = Individual({"status": 0, "distance": 20})
individual_3 = Individual({"status": 0, "distance": 10})
individual_4 = Individual({"status": 1, "distance": 10})
individuals = [individual_4, individual_1, individual_2, individual_3]

population = Population(individuals)
population.calculate_fitness()

scenario_1 = {
    'v1_speed': [35],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [55],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}
scenario_2 = {
    'v1_speed': [30],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [30],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}
scenario_3 = {
    'v1_speed': [35, 55],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [55, 75],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}

scenario_4 = {
    'v1_speed': [40],
    'v1_trajectory': [-2, 86.84551724137933, -2, 17.880000000000006],
    'v2_speed': [40],
    'v2_trajectory': [89.845517, 87.845517, 20.88, 18.88]
}

scenarios = [scenario_1, scenario_2, scenario_3, scenario_4]


def collect_scenario_data(scenario):
    with open("C:\\Users\\Harvey\\Documents\\AC3R-Demo\\python_src\\tests\\data\\Case6_data.json") as file:
        scenario_data = json.load(file)
    crash_scenario = CrashScenario.from_json(scenario_data)
    # JSON READ: Building scenario's streets
    bng_roads = []
    for road in crash_scenario.roads:
        bng_road = Road('road_asphalt_2lane', rid=road.name)
        bng_road.nodes.extend(road.road_nodes)
        bng_roads.append(bng_road)

    bng_vehicles = []
    for vehicle in crash_scenario.vehicles:
        if vehicle.name == 'v1':
            p = scenario['v1_trajectory']
            trajectory = ScriptFactory(p[0], p[1], p[2], p[3]).compute_scripts(speeds=scenario['v1_speed'])
        else:
            p = scenario['v2_trajectory']
            trajectory = ScriptFactory(p[0], p[1], p[2], p[3]).compute_scripts(speeds=scenario['v2_speed'])

        initial_position = (trajectory[0][0], trajectory[0][1], 0)
        v = Vehicle("scenario_player_" + str(vehicle.name),
                    model="etk800", licence=vehicle.name, color=vehicle.color)
        v.attach_sensor('damage', Damage())
        road_pf = RoadProfiler()
        road_pf.compute_ai_script(trajectory, vehicle.color)
        bng_vehicles.append(BNGVehicle(v, initial_position, None, vehicle.rot_quat, road_pf))

    return bng_roads, bng_vehicles


class PopulationTest(unittest.TestCase):
    def test_get_fittest(self):
        self.assertEqual(population.get_fittest(), individual_4, "The fittest is scenario 04")

    def test_get_second_fittest(self):
        self.assertEqual(population.get_second_fittest(), individual_3, "The second fittest is scenario 03")

    def test_get_least_fittest(self):
        self.assertEqual(population.get_least_fittest(), individual_1, "The least fittest is scenario 01")

    def test_population(self):
        individuals = []
        for s in scenarios:
            bng_roads, bng_vehicles = collect_scenario_data(s)
            # Execute crash scenario
            simulation = Simulation(bng_roads, bng_vehicles)
            simulation.execute_scenario()
            individuals.append(Individual(simulation.get_result()))

        population = Population(individuals)
        population.calculate_fitness()
        for p in population.individuals:
            print(p)
        print(population.get_fittest())
        print(population.get_second_fittest())
        print(population.get_least_fittest())

        self.assertEqual(0, 0)
