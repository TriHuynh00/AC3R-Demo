from math import atan2,degrees


# critical and non critical scenarios.
def DamageExtraction(striker_damage, victim_damage, actual_striker_damage, actual_victim_damage):
    print("damage extraction")

    striker_damage_area = ""
    victim_damage_area = ""

    striker_damage_score = 0
    victim_damage_score = 0

    if 'damage' in striker_damage:

        # Striker Damage
        if striker_damage['damage'] > 100:
            stkeys = striker_damage['part_damage']
            #front damage
            for keys in stkeys:
                if 'bumperbar_F' in keys or 'body_wagon' in keys or 'door_FL' in keys or 'headlight_R' in keys or 'bumper_F' in keys or  'headlight_L' in keys or 'fender_R' in keys or 'fender_L' in keys or 'hood' in keys or 'door_FR' in keys:
                    #print("striker front")
                    striker_damage_area = striker_damage_area + 'F'
                    break


            # back damage
            for keys in stkeys:
                if 'tailgateglass' in keys or 'tailgate' in keys or 'taillight_L' in keys or 'exhaust_i6_petrol' in keys or 'bumper_R' in keys:
                    #print("striker back")
                    striker_damage_area = striker_damage_area + 'B'
                    break

            # right damage
            for keys in stkeys:
                if 'mirror_R' in keys or 'doorglass_FR' in keys or 'door_RR_wagon' in keys or 'door_FR' in keys:
                    #print("striker right")
                    striker_damage_area = striker_damage_area + 'R'
                    break

            # left damage
            for keys in stkeys:
                if 'fender_L' in keys or 'door_FL' in keys or 'doorglass_RL_wagon' in keys or 'door_RL_wagon' in keys or 'mirror_L' in keys or 'door_RL' in keys:
                    #print("striker left")
                    striker_damage_area = striker_damage_area + 'L'
                    break


            print('striker damage area ' + striker_damage_area)

            if actual_striker_damage in striker_damage_area:
                striker_damage_score = 1.0
                print(striker_damage_score)
            else:
                striker_damage_score = 0.5
                print(striker_damage_score)



        # Victim Damage
        if victim_damage['damage'] > 100:
            vikeys = victim_damage['part_damage']
            # front damage
            for keys in vikeys:
                if 'bumperbar_F' in keys or 'door_FL' in keys or 'headlight_R' in keys or 'bumper_F' in keys or 'headlight_L' in keys or 'fender_R' in keys or 'fender_L' in keys or 'hood' in keys or 'door_FR' in keys:
                    #print("victim front")
                    victim_damage_area = victim_damage_area + 'F'
                    break

            # back damage
            for keys in vikeys:
                if 'tailgateglass' in keys or 'tailgate' in keys or 'exhaust_i6_petrol' in keys or 'bumper_R' in keys:
                    #print("victim back")
                    victim_damage_area = victim_damage_area + 'B'
                    break

            # right damage
            for keys in vikeys:
                if 'mirror_R' in keys or 'doorglass_FR' in keys or 'door_RR_wagon' in keys or 'door_FR' in keys:
                    #print("victim right")
                    victim_damage_area = victim_damage_area + 'R'
                    break

            # left damage
            for keys in vikeys:
                if 'fender_L' in keys or 'door_FL' in keys or 'doorglass_RL_wagon' in keys or 'door_RL_wagon' in keys or 'mirror_L' in keys:
                    #print("vicitm left")
                    victim_damage_area = victim_damage_area + 'L'
                    break

            print('victim damage area '+ victim_damage_area)

            if actual_victim_damage in victim_damage_area:
                victim_damage_score = 1.0
                print(victim_damage_score)
            else:
                victim_damage_score = 0.5
                print(victim_damage_score)


    return striker_damage_score, victim_damage_score



# distance from collision point
def DistanceExtraction(striker_speed, striker_after_position, striker_collision_position, victim_speed, victim_after_position, victim_collision_position):
    print("Distance Extraction")

    distance_stiker = math.hypot(striker_collision_position[0] - striker_after_position[0], striker_collision_position[1] - striker_after_position[1])
    distance_victim = math.hypot(victim_collision_position[0] - victim_after_position[0], victim_collision_position[1] - victim_after_position[1])

    print('stiker distance '+ str(distance_stiker))
    print('victim distance '+ str(distance_victim))

    # the fitness fucntion of distance eztraction is limited to the speed of 20 ms
    # range of speed for calculation.

    striker_distance_score = 0
    victim_distance_score = 0

    # ---------------------------------------------------------------------------------------
    #  distance calculation without speed.

    # striker distance for 15 ms
    if striker_speed >= 0 and distance_stiker > 0 and distance_stiker < 8:
        striker_distance_score = 0.1

    if striker_speed >= 0 and distance_stiker > 7 and distance_stiker <= 15:
        striker_distance_score = 0.3

    if striker_speed >= 0 and distance_stiker > 15:
        striker_distance_score = 0.2


    # victim_distance for 20 ms
    if victim_speed >= 0 and distance_victim > 0 and distance_victim < 8 :
        victim_distance_score = 0.1

    # victim_distance for 20 ms
    if victim_speed >= 0 and distance_victim > 7 and distance_victim <= 15:
        victim_distance_score = 0.3

        # victim_distance for 20 ms
    if victim_speed >= 0 and distance_victim > 15:
        victim_distance_score = 0.2

    # --------------------------------------------------------------------------------------------

    return striker_distance_score, victim_distance_score



def AngleBtw2Points(pointA, pointB):
  changeInX = pointB[0] - pointA[0]
  changeInY = pointB[1] - pointA[1]
  return degrees(atan2(changeInY,changeInX)) #remove degrees if you want your answer in radians

# angle between 3 points
import math
def getAngle(a, b, c):
    ang = math.degrees(math.atan2(c[1] - b[1], c[0] - b[0]) - math.atan2(a[1] - b[1], a[0] - b[0]))
    return ang + 360 if ang < 0 else ang



# deviation of car due to collision.
def RotationExtraction(striker_precrash_position, striker_collision_point, striker_post_crash_position, victim_precrash_position, victim_collision_point, victim_post_crash_position):
    print("rotation extraction")

    striker_alpha = getAngle(striker_precrash_position, striker_collision_point, striker_post_crash_position)
    victim_alpha = getAngle(victim_precrash_position, victim_collision_point, victim_post_crash_position)

    striker_heading = AngleBtw2Points(striker_precrash_position, striker_collision_point)
    victim_heading = AngleBtw2Points(victim_precrash_position, victim_collision_point)

    striker_deviation = (striker_heading + striker_alpha) % 360
    victim_deviation = (victim_heading + victim_alpha) % 360

    striker_deviation_score = 0
    victim_deviation_score = 0

    if (striker_alpha < 180):
        print("striker clockwise rotation")
        striker_deviation_score = 0.3

    else:
        print("striker counter-clockwise rotation")
        striker_deviation_score = 0

    if (victim_alpha < 180):
        print("victim clockwise rotation")
        victim_deviation_score = 0.3
    else:
        print("victim counter-clockwise rotation")
        victim_deviation_score = 0

    # if striker_deviation > 0 and  striker_deviation <= 10:
    #     striker_deviaton_score = 0.1
    #
    # if striker_deviation > 10 and  striker_deviation <= 20:
    #     striker_deviaton_score = 0.2
    #
    # if striker_deviation > 20:
    #     striker_deviaton_score = 0.3
    #
    #
    # #---------------------------------------------------------
    #
    # if victim_deviation > 0 and victim_deviation <= 50:
    #     victim_deviation_score = 0.1
    #
    # if victim_deviation > 50 and victim_deviation <= 100:
    #     victim_deviation_score = 0.2
    #
    # if victim_deviation > 100:
    #     victim_deviation_score = 0.3


    print('striker rotation score '+ str(striker_deviation_score))
    print('victim rotation score '+ str(victim_deviation_score))

    return  striker_deviation_score,victim_deviation_score

