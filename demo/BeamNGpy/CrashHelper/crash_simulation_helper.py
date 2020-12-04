import math
from math import atan2,degrees

road_a_distance = 15
road_b_distance = 15

# road_a = [(241, 72),(238, 143)]
# road_b =  [(167, 139),(238, 143)]

# road_a = [(-2.0,17.880000000000006),(-2.0,2.0)]
# road_b =  [(20.88,18.88),(-2.0,2.0)]

road_a = [(-2.0,86.84551724137933,0),(-2.0,2.0,0)]
road_b =  [(89.845517,87.845517,0),(-2.0,2.0,0)]

def AngleBtw2Points(pointA, pointB):
    changeInX = pointB[0] - pointA[0]
    changeInY = pointB[1] - pointA[1]
    return degrees(atan2(changeInY,changeInX)) #remove degrees if you want your answer in radians

def getPolyLineCoordinates(node_a,node_b, distance,width):
    #print("get polyline coordinate")
    # Assumption. width from the center of the road.
    real_distance = getDistance(node_a,node_b)
    t = distance / real_distance

    if t == 0.0:
        t = 0.05

    point2 = (((1 - t) * node_a[0] + t * node_b[0]), ((1 - t) * node_a[1] + t * node_b[1]))

    dx = float(point2[0] - node_a[0])
    dy = float(point2[1] - node_a[1])

    L = float(math.sqrt(float(float(dx * dx) + float(dy * dy)))) # handle division by zero
    U = (float(dy / L), float(dx / L))
    F = float(width)

    # Point on one side
    x2p = float(point2[0] + U[0] * F)
    y2p = float(point2[1] + U[1] * F)

    return x2p,y2p


def getDistance(node_a,node_b):
    dist = math.sqrt((node_a[1] - node_b[1]) ** 2 + (node_a[0] - node_b[0]) ** 2)
    return dist


def getV1BeamNGCoordinaes(total_distance_v1, width):
    global road_a
    v1_roads = road_a # coordinates of roads.
    v1_roads_distance = road_a_distance
    #print(v1_roads_distance)
    v1_road_max = float(total_distance_v1 * v1_roads_distance)
    #print(v1_road_max)
    beamng_pos = ""
    v1_poly_distance = v1_road_max
    for node in v1_roads:
        #node_distance = getDistance(beamng_dict[node[0]],beamng_dict[node[1]])
        node_distance = getDistance(v1_roads[0], v1_roads[1])
        v1_poly_distance = v1_poly_distance - node_distance
        if v1_poly_distance < 0:
            v1_poly_distance = v1_poly_distance + node_distance
            #print("road found")
            #beamng_pos =   getPolyLineCoordinates(beamng_dict[node[0]],beamng_dict[node[1]],v1_poly_distance,width)
            beamng_pos = getPolyLineCoordinates(v1_roads[0], v1_roads[1], v1_poly_distance, width)
            break

    #print(beamng_pos)
    return beamng_pos




def getV2BeamNGCoordinaes(total_distance_v2, width):
    global  road_b
    #print("beamng v2 coordinates")
    #print(total_distance_v2)
    v2_roads = road_b
    v2_roads_distance = road_b_distance
    v2_road_max = float(total_distance_v2 * v2_roads_distance)
    #print(v2_road_max)
    beamng_pos = ""
    v2_poly_distance = v2_road_max
    for node in v2_roads:
        #node_distance = getDistance(beamng_dict[node[0]],beamng_dict[node[1]])
        node_distance = getDistance(v2_roads[0], v2_roads[1])
        v2_poly_distance = v2_poly_distance - node_distance

        if v2_poly_distance < 0:
            v2_poly_distance = v2_poly_distance + node_distance
            #print("road found")
            #beamng_pos = getPolyLineCoordinates(beamng_dict[node[0]],beamng_dict[node[1]], v2_poly_distance, width)
            beamng_pos = getPolyLineCoordinates(v2_roads[0], v2_roads[1], v2_poly_distance, width)
            break

    print(beamng_pos)
    return beamng_pos




