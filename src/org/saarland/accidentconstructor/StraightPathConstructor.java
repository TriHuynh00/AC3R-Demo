package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.*;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.OntologyHandler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class StraightPathConstructor {

    private DecimalFormat df;

    private int distanceBetweenBaseRoadNodes = 40;

    // Distance between a stopped car and an intersection
    private int stoppingDistance = 10;

    private int extraDistance = 15;

    private double acceleration = 4.47; // 4.4 m/s2 or 10 mph

    private double leaveTriggerDistance = -1;

    public double getLeaveTriggerDistance() {
        return leaveTriggerDistance;
    }

//    public void setleaveTriggerDistance(boolean requireLeaveDistanceTrigger) {
//        this.leaveTriggerDistance = leaveTriggerDistance;
//    }

    StraightPathConstructor()
    {
        df = AccidentParam.df6Digit;
    }

    /*
     *  Construct Straight Paths and Turn Into Paths cases. In general, there are two accident patterns:
     *
     *  1) One vehicle stops, then accelerates and hit another moving vehicle
     *
     *  AC3R computes the distance between the stopping car and traveling vehicle needed to make the stopping car
     *  accelerates and hit the travelling vehicle at the crash point.
     *
     *  For Straight Paths accidents, in each vehicle's trajectory, an extra waypoint is added after the crash point
     *  to make the vehicles crash naturally.
     *
     *  2) All vehicles travel from the start
     *
     *  AC3R computes the time and distance needed for vehicle1 to accelerate to its target speed, along with
     *  the time and distance of traveling at the target speed in 2 seconds. Next, for vehicle2, AC3R computes
     *  the time and distance needed to accelerate to the target speed, and subtracts the acceleration time of
     *  vehicle2 from the total traveling time of vehicle1 to get the time vehicle2 needed to reach the crash point
     *  and hits vehicle1.
     *
     *  The target speed is the travelling speed of the vehicle which AC3R extracts from the crash description,
     *  if no travelling speed is given, AC3R uses the default speed.
     *
     */
    public ArrayList<ArrayList<String>> constructAccidentScenario(ArrayList<VehicleAttr> vehicleList,
                                                                  OntologyHandler parser,
                                                                  TestCaseInfo testCase)
    {
        //AccidentConstructorUtil.findVehicleBasedOnId(2, vehicleList).setVelocity(50);
        ArrayList<ArrayList<String>> constructedCoordVeh = new ArrayList<ArrayList<String>>();
        ArrayList<Integer> impactAtSteps = new ArrayList<Integer>();
        ArrayList<ArrayList<VehicleAttr>> impactedVehiclesAtSteps = new ArrayList<ArrayList<VehicleAttr>>();

        VehicleAttr[] strikerAndVictim = new VehicleAttr[2];

        for (VehicleAttr vehicle : vehicleList)
        {
            constructedCoordVeh.add(new ArrayList<String>());
        }

        constructedCoordVeh = AccidentConstructorUtil.fillCoordOfVehicles(constructedCoordVeh, vehicleList.get(0).getActionList().size());

        strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictim(vehicleList.get(0), vehicleList.get(1));

        // Find the index of the impact action in the action lists of both vehicles
        AccidentConstructorUtil.findImpactedStepsAndVehicles(impactAtSteps, impactedVehiclesAtSteps, vehicleList);

        // If there are 2 vehicles, construct the only crash point at 0:0
        if (vehicleList.size() == 2) {

            if (impactAtSteps.size() > 0) {
                constructedCoordVeh.get(0).add(impactAtSteps.get(0), "0:0");
                constructedCoordVeh.get(1).add(impactAtSteps.get(0), "0:0");
            } else {
                constructedCoordVeh.get(0).add("0:0");
                constructedCoordVeh.get(1).add("0:0");
            }

            ConsoleLogger.print('d',"impactAtSteps size: " + impactAtSteps.size());

            // If an impact action is found, construct the crash coord based on the travelling direction of the vehicles
            if (impactAtSteps.size() >= 1) {
                String crashXCoord = "0";
                String crashYCoord = "0";

                for (int v = 0; v < vehicleList.size(); v++) {
                    VehicleAttr currentVehicle = vehicleList.get(v);

                    Street vehicleStandingStreet = currentVehicle.getStandingStreet();

                    ConsoleLogger.print('d',"Current vehicle ID is " + currentVehicle.getVehicleId());

                    ConsoleLogger.print('d',"Standing Street is " + vehicleStandingStreet.getStreetPropertyValue("road_ID"));

                    int laneNum = Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("lane_num"));

                    ConsoleLogger.print('d',"Vehicle " + currentVehicle.getVehicleId() + " travelling direction is " + currentVehicle.getTravellingDirection());

                    String travellingDirection = currentVehicle.getTravellingDirection();
                    String otherVehicleTravellingDirection = vehicleList.get(vehicleList.size() - 1 - v).getTravellingDirection();

                    // Determine crash coord
                    if ((travellingDirection.equals("NE") || travellingDirection.equals("NW")
                            || travellingDirection.equals("SE") || travellingDirection.equals("SW"))
                            && (otherVehicleTravellingDirection.equals("NE") || otherVehicleTravellingDirection.equals("NW")
                            || otherVehicleTravellingDirection.equals("SE") || otherVehicleTravellingDirection.equals("SW")))
                    {
                        crashXCoord = "0";
                        crashYCoord = "0";
                    }

                    else if (travellingDirection.equals("N") || travellingDirection.equals("NE") || travellingDirection.equals("NW"))
                    {
                        crashXCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth + AccidentParam.laneWidth / 2.0);
                        if (travellingDirection.equals("NE") || travellingDirection.equals("NW"))
                        {
                            if (crashYCoord.equals("0"))
                            {
                                crashYCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth - AccidentParam.laneWidth / 2.0);
                            }
                        }
                    }
                    else if (travellingDirection.equals("S") || travellingDirection.equals("SE") || travellingDirection.equals("SW"))
                    {
                        crashXCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth - AccidentParam.laneWidth / 2.0);
                        if (travellingDirection.equals("SE") || travellingDirection.equals("SW"))
                        {
                            if (crashYCoord.equals("0"))
                            {
                                crashYCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth + AccidentParam.laneWidth / 2.0);
                            }
                        }
                    }
                    else if (travellingDirection.equals("W"))
                    {
                        crashYCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth + AccidentParam.laneWidth / 2.0);
                    }
                    else if (travellingDirection.equals("E"))
                    {
                        crashYCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth - AccidentParam.laneWidth / 2.0);
                    }


                } // End finding crash coords by looping through involved vehicles

                String crashCoord = crashXCoord + ":" + crashYCoord;
                ConsoleLogger.print('d',"Crash coord " + crashCoord);


                // Find vehicle that stopped first (to trigger leave distance calculation)
                int stoppedCarID = -1;
                double otherCarInitXCoord = -1;
                double otherCarInitYCoord = -1;

                for (int v = 0; v < vehicleList.size(); v++) {
                    VehicleAttr currentVehicle = vehicleList.get(v);

                    constructedCoordVeh.get(currentVehicle.getVehicleId() - 1).set(impactAtSteps.get(0), crashCoord);

                    LinkedList<String> vehicleActionList = currentVehicle.getActionList();

                    int estimateVelocity = parser.findVelocityOfAction(vehicleActionList.get(0));

                    // If this is a stop, we construct the car to stop on the street, for parking, need to do st else
                    if (estimateVelocity == 0 && !vehicleActionList.get(0).equals("park")) {
                        Street vehicleStandingStreet = currentVehicle.getStandingStreet();

                        int laneNum = Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("lane_num"));

                        stoppedCarID = currentVehicle.getVehicleId();

                        String vehicleXCoord = "0";
                        String vehicleYCoord = "0";
                        Street otherStreet = null;

                        if (testCase.getStreetList().size() == 2) {
                            otherStreet = testCase.getStreetList().get(testCase.getStreetList().size() -
                                    Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_ID")));
                        }

                        if (currentVehicle.getTravellingDirection().equals("N")) {
                            // TODO: If lane_num > 2 (4 lanes above), find the right lane that the car sits in
//                            vehicleXCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth + AccidentParam.laneWidth / 2.0);

                            // Detect the yCoord by taking the other road and calculate offset based on the laneNumber
                            if (otherStreet != null) {
                                int otherStreetLaneNum = Integer.parseInt(
                                        otherStreet.getStreetPropertyValue("lane_num"));

                                vehicleYCoord = "" + -1 * ((otherStreetLaneNum / 2.0) * AccidentParam.laneWidth + stoppingDistance);
                            }
                        }

                        ConsoleLogger.print('d',"Coord of parking car " + vehicleXCoord + ":" + vehicleYCoord);
                        // Set the init coord of the car
                        otherCarInitXCoord = Double.parseDouble(vehicleXCoord);
                        otherCarInitYCoord = Double.parseDouble(vehicleYCoord);

                        constructedCoordVeh.get(currentVehicle.getVehicleId() - 1).set(0, vehicleXCoord + ":" + vehicleYCoord);

                    } // End checking first action is stopped
                } // End processing stopped vehicle

                // Begin to process the moving car
                ArrayList<ArrayList<Double>> distanceAndTime = new ArrayList<ArrayList<Double>>();

                for (VehicleAttr veh : vehicleList)
                {
                    distanceAndTime.add(new ArrayList<Double>());
                }

                for (int v = 0; v < vehicleList.size(); v++)
                {
                    VehicleAttr currentVehicle = vehicleList.get(v);

                    Street vehicleStandingStreet = currentVehicle.getStandingStreet();

                    String travelDirection = currentVehicle.getTravellingDirection();

                    ArrayList<String> vehicleCoordList = constructedCoordVeh.get(currentVehicle.getVehicleId() - 1);

                    boolean curvyRoad = false;
                    double radius = 0;

                    if (!vehicleStandingStreet.getStreetPropertyValue("road_shape").equals(RoadShape.STRAIGHT))
                    {
                        curvyRoad = true;
                        radius = Double.parseDouble(vehicleStandingStreet.getStreetPropertyValue("curve_radius").replace("m", ""));
                    }

                    // If we found a car that initially stopped, calculate the distance between moving and stopped cars
                    if (stoppedCarID != -1 && currentVehicle.getVehicleId() != stoppedCarID)
                    {
                        // Calculate distance between crash point and stopped cars
                        double stoppedCarAndCrashPointDistance = AccidentConstructorUtil.
                                computeDistanceBetweenTwo2DPoints(Double.parseDouble(crashXCoord)
                                        , Double.parseDouble(crashYCoord),
                                        otherCarInitXCoord, otherCarInitYCoord);
//                                Math.sqrt(
//                                Math.pow(Double.parseDouble(crashXCoord) - otherCarInitXCoord, 2)
//                                + Math.pow(Double.parseDouble(crashYCoord) - otherCarInitYCoord, 2)
//                        );

                        ConsoleLogger.print('d',"Stopped car distance to crash point: " + stoppedCarAndCrashPointDistance);

                        // Calculate the time needed for the stopped car to reach the crash point
//                        double approachingTimeInSec = stoppedCarAndCrashPointDistance /
//                                AccidentConstructorUtil.convertMPHToMS(vehicleList.get(stoppedCarID).getVelocity());

                        // Calculate accelaration distance here
//                        double approachingTimeInSec = calculateTravelTimeBasedOnAcceleration(
//                                vehicleList.get(stoppedCarID).getVelocity(), stoppedCarAndCrashPointDistance);
                        double approachingTimeInSec = AccidentConstructorUtil.convertMPHToMS(vehicleList.get(stoppedCarID).getVelocity())
                                / AccidentParam.accelerationTo20Mph + 0.6;

                        ConsoleLogger.print('d',"approaching time given velo " + AccidentConstructorUtil.convertMPHToMS(vehicleList.get(stoppedCarID).getVelocity()) + " m/s is " + approachingTimeInSec + " s");

                        // Calculate the distance of the moving vehicle that will ensure crash within the approachingTime
//                        double distanceCurrVehicleAndCrashPoint = approachingTimeInSec *
//                                AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity());

                        double distanceCurrVehicleAndCrashPoint = approachingTimeInSec * AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity());

                        // Add 5% more value to ensure crash due to uncertainty of the stopped car
//                        distanceCurrVehicleAndCrashPoint += distanceCurrVehicleAndCrashPoint * 0.05;

                        // If extra distance to make the crash non-critical is given, add the distance to the computation
                        distanceCurrVehicleAndCrashPoint += AccidentConstructorUtil.getNonCriticalDistance();

                        ConsoleLogger.print('d',"distance between moving car and crash point is " + distanceCurrVehicleAndCrashPoint);
                        ConsoleLogger.print('d',"Curvy Road of street " + vehicleStandingStreet.getStreetPropertyValue("road_ID") + " ? " + curvyRoad);
                        ConsoleLogger.print('d',"Road Shape " + testCase.getTestCaseProp().get("road_shape"));

                        String currentVehicleXCoord = crashXCoord;
                        String currentVehicleYCoord = crashYCoord;

                        // If travel to the west, add the distance to the x and keep the y intact
//                        if (travelDirection.equals("W"))
//                        {
//                            currentVehicleXCoord = df.format(Double.parseDouble(crashXCoord) + distanceCurrVehicleAndCrashPoint);
//
//                            if (radius != 0)
//                            {
//                                currentVehicleYCoord = df.format(AccidentConstructorUtil.computeYCircleFunc(radius,
//                                        Double.parseDouble(currentVehicleXCoord)));
//                            }
//                        }
                        int roadAngle = Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_angle"));
                        double segmentLength = distanceCurrVehicleAndCrashPoint;
                        if (travelDirection.equals("SE") || travelDirection.equals("SW")
                            || travelDirection.equals("NE") || travelDirection.equals("NW"))
                        {
                            // For diagonal direction, the near crash length is equal to crashYCoord + nearCrashDistance
                            double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(
                                    Double.parseDouble(crashYCoord) + segmentLength, roadAngle);

                            // if there is a curve radius, compute the new xCoord
                            if (radius != 0)
                            {
                                newCoord[0] = AccidentConstructorUtil.computeXCircleFunc(radius, newCoord[1]);
                            }

                            vehicleCoordList.add(0, df.format(newCoord[0])
                                    + ":" + df.format(newCoord[1]));

                            currentVehicleXCoord = df.format(newCoord[0]);
                            currentVehicleYCoord = df.format(newCoord[1]);

                        } // End setting init and near crash coord for diagonal directions
                        else// if (travelDirection.equals("S"))
                        {
                            // Compute near crash distance, for S, it is crashYcoord + nearCrashDistance
                            // For east and west direction, the segment length counts from the crashXCoord + nearCrashDistance
                            if (travelDirection.equals("E") || travelDirection.equals("W"))
                            {
                                segmentLength += Double.parseDouble(crashXCoord);
                            }
                            else // For north and south, it is nearCrashDistance + crashYCoord
                            {
                                segmentLength += Double.parseDouble(crashYCoord);
                            }
                            String nearCrashCoord = NavigationDictionary.createNESWCoordBasedOnNavigation(
                                    segmentLength, radius, "backward",
                                    NavigationDictionary.selectDictionaryFromTravelingDirection(travelDirection),
                                    AccidentParam.defaultCoordDelimiter
                            );
                            nearCrashCoord = nearCrashCoord.substring(0, nearCrashCoord.length() - 2);
                            vehicleCoordList.add(0, nearCrashCoord);
                            currentVehicleXCoord = nearCrashCoord.split(AccidentParam.defaultCoordDelimiter)[0];
                            currentVehicleYCoord = nearCrashCoord.split(AccidentParam.defaultCoordDelimiter)[1];
                        }

                        ConsoleLogger.print('d',"Moving vehicle first point before impact " + currentVehicleXCoord + ":" + currentVehicleYCoord);

                        // Check if the action before crash is a moving action, if yes, assign the prior crash point in that index
                        int indexActionBeforeCrash = impactAtSteps.get(0) - 1;

                        if (parser.findVelocityOfAction(currentVehicle.getActionList().get(indexActionBeforeCrash)) > 0)
                        {
                            vehicleCoordList.set(indexActionBeforeCrash, currentVehicleXCoord + ":" + currentVehicleYCoord);
                        }
                        else // Scan at other index to find the right place for inserting the prior crash point
                        {
                            for (int l = indexActionBeforeCrash - 1; l >= 0; l--)
                            {
                                if (parser.findVelocityOfAction(currentVehicle.getActionList().get(l)) > 0)
                                {
                                    vehicleCoordList.set(l, currentVehicleXCoord + ":" + currentVehicleYCoord);
                                    indexActionBeforeCrash = l;
                                    break;
                                }
                            }
                        }

                        // Compute Leave trigger distance
                        ConsoleLogger.print('d',String.format("Current Car: %s:%s ; Other car: %.6f:%.6f \n",
                                currentVehicleXCoord, currentVehicleYCoord, otherCarInitXCoord, otherCarInitYCoord));
                        leaveTriggerDistance = AccidentConstructorUtil.computeDistanceBetweenTwo2DPoints(
                            Double.parseDouble(currentVehicleXCoord), Double.parseDouble(currentVehicleYCoord),
                            otherCarInitXCoord, otherCarInitYCoord
                        );
                        testCase.putValToKey("need_trigger", "T");

                        // Set Leave trigger distance for the stopped car
                        AccidentConstructorUtil.findVehicleBasedOnId(stoppedCarID, vehicleList).setLeaveTriggerDistance(leaveTriggerDistance);

                        ConsoleLogger.print('d',"Leave Trigger Distance " + leaveTriggerDistance);

                        // Construct the coords before the "prior crash" point
                        for (int i = indexActionBeforeCrash - 1; i >= -1 * AccidentParam.SIMULATION_DURATION; i--)
                        {
//                            if (travelDirection.equals("W"))
//                            {
//                                double vehicleCoordX = Double.parseDouble(currentVehicleXCoord) + currentVehicle.getVelocity();
//                                double vehicleCoordY = Double.parseDouble(currentVehicleYCoord);
//                                if (radius != 0)
//                                {
//                                     vehicleCoordY = AccidentConstructorUtil.computeYCircleFunc(radius, vehicleCoordX);
//                                }
//
//                                currentVehicleXCoord = vehicleCoordX + "";
//                                currentVehicleYCoord = vehicleCoordY + "";
//
//                                if (i >= 0)
//                                    vehicleCoordList.set(i, vehicleCoordX + ":" + vehicleCoordY);
//                                else if (i < 0)
//                                    vehicleCoordList.add(0, vehicleCoordX + ":" + vehicleCoordY);
//                            } // End processing westbound direction
                            segmentLength = currentVehicle.getVelocity();

                            // For diagonal direction, the near crash length is equal to crashYCoord + nearCrashDistance
                            if (travelDirection.equals("SE") || travelDirection.equals("SW")
                                    || travelDirection.equals("NE") || travelDirection.equals("NW"))
                            {

                                double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(
                                        Double.parseDouble(currentVehicleYCoord) + segmentLength, roadAngle);

                                // if there is a curve radius, compute the new xCoord
                                if (radius != 0)
                                {
                                    newCoord[0] = AccidentConstructorUtil.computeXCircleFunc(radius, newCoord[1]);
                                }
                                currentVehicleXCoord = df.format(newCoord[0]);
                                currentVehicleYCoord = df.format(newCoord[1]);
                            } // End setting init and near crash coord for diagonal directions
                            else// if (travelDirection.equals("S"))
                            {
                                // Compute near crash distance, for S, it is crashYcoord + nearCrashDistance
                                // For east and west direction, the segment length counts from the crashXCoord + nearCrashDistance
                                if (travelDirection.equals("E") || travelDirection.equals("W"))
                                {
                                    segmentLength += Double.parseDouble(currentVehicleXCoord);
                                }
                                else // For north and south, it is nearCrashDistance + crashYCoord
                                {
                                    segmentLength += Double.parseDouble(currentVehicleYCoord);
                                }
                                String nearCrashCoord = NavigationDictionary.createNESWCoordBasedOnNavigation(
                                        segmentLength, radius, "backward",
                                        NavigationDictionary.selectDictionaryFromTravelingDirection(travelDirection),
                                        AccidentParam.defaultCoordDelimiter
                                );

                                nearCrashCoord = nearCrashCoord.substring(0, nearCrashCoord.length() - 2);
                                currentVehicleXCoord = nearCrashCoord.split(AccidentParam.defaultCoordDelimiter)[0];
                                currentVehicleYCoord = nearCrashCoord.split(AccidentParam.defaultCoordDelimiter)[1];
                            }
                            if (i >= 0)
                                vehicleCoordList.set(i, currentVehicleXCoord + ":" + currentVehicleYCoord);
                            else if (i < 0)
                                vehicleCoordList.add(0, currentVehicleXCoord + ":" + currentVehicleYCoord);
                        } // End setting coord for remaining actions

                        AccidentConstructorUtil.removeMeaninglessCoord(constructedCoordVeh);

                        constructedCoordVeh.set(currentVehicle.getVehicleId() - 1, vehicleCoordList);

                    } // End processing for stopped cars
                    else if (stoppedCarID == -1) // if there is no stopped car, then compute the travelling distance of 2 cars
                    {
                        // Compute the travelling distance and time of this car given its velocity

                        ArrayList<Double> currentVehDistanceAndTime = computeDistanceAndTimeWithAcceleration(currentVehicle.getVelocity());

                        // Compute total moving distance by appending distance of accelerated movement and simulation duration
                        // (in second) of stable movement

                        int roadAngle = Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_angle"));
                        double nearCrashDistance = 0;
                        double totalDistance = 0;

                        // Newly added part, compute the time based on another vehicle's speed
                        // if this is a striker car, computed as normal
                        if (currentVehicle.equals(strikerAndVictim[0]))
                        {
                            nearCrashDistance =  AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity())
                                * AccidentParam.SIMULATION_DURATION;

                        }
                        else if (currentVehicle.equals(strikerAndVictim[1]))
                        {
                            VehicleAttr striker = strikerAndVictim[0];
                            ArrayList<Double> strikerDistanceAndTime = computeDistanceAndTimeWithAcceleration(striker.getVelocity());
                            nearCrashDistance = AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity())
                                    * (strikerDistanceAndTime.get(1) + 2.0 - currentVehDistanceAndTime.get(1));
                        }
                        totalDistance = currentVehDistanceAndTime.get(0) + nearCrashDistance;
                        /* Old implementation
                        int roadAngle =  Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_angle"));

                        double nearCrashDistance =  AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity())
                                * AccidentParam.SIMULATION_DURATION;
                        double totalDistance = currentVehDistanceAndTime.get(0) + nearCrashDistance; */

                        ConsoleLogger.print('d',"nearCrashDistance = " + nearCrashDistance);
                        ConsoleLogger.print('d',"totalDistance = " + totalDistance);

                        // Compute near crash node for each road direction
                        if (//!travelDirection.equals("S") && !travelDirection.equals(""))
                            travelDirection.equals("SE") || travelDirection.equals("SW")
                            || travelDirection.equals("NE") || travelDirection.equals("NW"))
                        {
                            // For diagonal direction, the near crash length is equal to crashYCoord + nearCrashDistance
                            double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(
                                    Double.parseDouble(crashYCoord) + nearCrashDistance, roadAngle);

                            // if there is a curve radius, compute the new xCoord
                            if (radius != 0)
                            {
                                newCoord[0] = AccidentConstructorUtil.computeXCircleFunc(radius, newCoord[1]);
                            }

//                            vehicleCoordList.add(0, df.format(newCoord[0] - (AccidentParam.laneWidth + 2))
//                                    + ":" + df.format(newCoord[1]));
                            vehicleCoordList.add(0, df.format(newCoord[0])
                                    + ":" + df.format(newCoord[1]));
                            if (radius != 0)
                            {
                                newCoord[0] = AccidentConstructorUtil.computeXCircleFunc(radius, newCoord[1]);
                            }
                            // Compute and set the init position
                            newCoord = AccidentConstructorUtil.computeNewCoordOfRotatedLine(
                                Double.parseDouble(crashYCoord) + totalDistance, roadAngle);

                            ConsoleLogger.print('d', String.format("diagonal car rotated first coord %.2f %.2f ",
                                    newCoord[0], newCoord[1]));

                            newCoord[0] = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius, newCoord[0], newCoord[1]);

                            vehicleCoordList.add(0, df.format(newCoord[0])
                                    + ":" + df.format(newCoord[1]));
                            ConsoleLogger.print('d', String.format("diagonal car final first coord %.2f %.2f ",
                                    newCoord[0], newCoord[1]));

                        } // End setting init and near crash coord for diagonal directions
                        else// if (travelDirection.equals("S"))
                        {
                            // Compute near crash distance, for S, it is crashYcoord + nearCrashDistance
                            double segmentLength = nearCrashDistance;

                            String nearCrashCoord = NavigationDictionary.createNESWCoordBasedOnNavigation(
                                    segmentLength, radius, "backward",
                                    NavigationDictionary.selectDictionaryFromTravelingDirection(travelDirection),
                                    AccidentParam.defaultCoordDelimiter
                            );

                            String[] nearCrashCoordElem = nearCrashCoord.split(AccidentParam.defaultCoordDelimiter);
                            // For east and west direction, the segment length counts from the crashXCoord + nearCrashDistance
                            if (travelDirection.equals("E") || travelDirection.equals("W"))
                            {
                                AccidentConstructorUtil.updateCoordElementAtDimension(0, nearCrashCoord,
                                        Double.parseDouble(nearCrashCoordElem[0]) + crashXCoord,
                                        AccidentParam.defaultCoordDelimiter);
//                                segmentLength += Double.parseDouble(crashXCoord);
                            }
                            else // For north and south, it is nearCrashDistance + crashYCoord
                            {
                                AccidentConstructorUtil.updateCoordElementAtDimension(1, nearCrashCoord,
                                        Double.parseDouble(nearCrashCoordElem[1]) + crashYCoord,
                                        AccidentParam.defaultCoordDelimiter);
//                                segmentLength += Double.parseDouble(crashYCoord);
                            }

                            vehicleCoordList.add(0, nearCrashCoord.substring(0, nearCrashCoord.length() - 2));

                            segmentLength = totalDistance;


                            String totalDistanceCoord = NavigationDictionary.createNESWCoordBasedOnNavigation(
                                    segmentLength, radius, "backward",
                                    NavigationDictionary.selectDictionaryFromTravelingDirection(travelDirection),
                                    AccidentParam.defaultCoordDelimiter
                            );

                            String[] totalDistanceCoordElem = totalDistanceCoord.split(AccidentParam.defaultCoordDelimiter);
                            if (travelDirection.equals("E") || travelDirection.equals("W"))
                            {
                                AccidentConstructorUtil.updateCoordElementAtDimension(0, totalDistanceCoord,
                                        Double.parseDouble(totalDistanceCoordElem[0]) + crashXCoord,
                                        AccidentParam.defaultCoordDelimiter);
//                                segmentLength += Double.parseDouble(crashXCoord);
                            }
                            else // For north and south, it is nearCrashDistance + crashYCoord
                            {
                                AccidentConstructorUtil.updateCoordElementAtDimension(1, totalDistanceCoord,
                                        Double.parseDouble(totalDistanceCoordElem[1]) + crashYCoord,
                                        AccidentParam.defaultCoordDelimiter);
//                                segmentLength += Double.parseDouble(crashYCoord);
                            }

                            ConsoleLogger.print('d', "Total Distance Coord  " + totalDistanceCoord);
                            vehicleCoordList.add(0, totalDistanceCoord.substring(0, totalDistanceCoord.length() - 2));
                            ConsoleLogger.print('d', "VehicleCoordList of V #" + currentVehicle.getVehicleId()
                                    + " " + vehicleCoordList);
//                            double currentVehicleXCoord = Double.parseDouble(crashXCoord);
//                            double currentVehicleYCoord = Double.parseDouble(crashYCoord) + nearCrashDistance;
//
//                            currentVehicleXCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius,
//                                    currentVehicleXCoord, currentVehicleYCoord);
//
//                            vehicleCoordList.add(0, df.format(currentVehicleXCoord) + ":"
//                                    + df.format(currentVehicleYCoord));
//
//                            // Compute Init position
//
//                            currentVehicleYCoord = Double.parseDouble(crashYCoord) + totalDistance;
//
//                            currentVehicleXCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius,
//                                    currentVehicleXCoord, currentVehicleYCoord);
//
//                            vehicleCoordList.add(0, df.format(currentVehicleXCoord ) + ":"
//                                    + df.format(currentVehicleYCoord));

                        }

//                        distanceAndTime.set(currentVehicle.getVehicleId() - 1, computeDistanceAndTimeWithAcceleration(currentVehicle.getVelocity()));
//                        // Reach the final vehicle, compute the distance now
//                        if (v == vehicleList.size() - 1)
//                        {
//                            if (distanceAndTime.get(0).get(1) > di)
//                        }

                        AccidentConstructorUtil.removeMeaninglessCoord(constructedCoordVeh);

                        constructedCoordVeh.set(currentVehicle.getVehicleId() - 1, vehicleCoordList);
                    }
                } // End processing vehicles

                // Append an extra distance to ensure crash happens. extra distance shows the intended moving direction
                // of the car
                for (int v = 0; v < vehicleList.size(); v++) {
                    VehicleAttr currentVehicle = vehicleList.get(v);

                    String travelDirection = currentVehicle.getTravellingDirection();

                    Street vehicleStandingStreet = currentVehicle.getStandingStreet();

                    Street turnStreet = null;

                    boolean isCurrentVehicleTurn = false;

                    String turnDirection = "";

                    String actionBeforeImpact = AccidentConstructorUtil.getActionBeforeImpact(currentVehicle);


                    ConsoleLogger.print('d', "Action before impact is " + actionBeforeImpact);


                    // Find the other street that a car intends to turn into
                    // NOTE: This only works when there is only one turn action in the action list
                    // because the construction of path is not based on multiple turns
                    if (actionBeforeImpact.contains("turn")) {
                        isCurrentVehicleTurn = true;
                        // Find the direction of the turn
                        if (actionBeforeImpact.contains("left")) {
                            turnDirection = "left";
                        } else if (actionBeforeImpact.contains("right")) {
                            turnDirection = "right";
                        }

                        ConsoleLogger.print('d', String.format("Current vehicle %s move towards %s turn direction is %s on street %s direction %s",
                            currentVehicle.getVehicleId(),
                            travelDirection,
                            turnDirection,
                            vehicleStandingStreet.getStreetPropertyValue("road_ID"),
                            vehicleStandingStreet.getStreetPropertyValue("road_navigation")));

                        String currVehiclePossibleTurnDirections = NavigationDictionary.getTurnDirectionOfMovingDirection(
                            currentVehicle.getTravellingDirection(), turnDirection);

                        ConsoleLogger.print('d', "Current vehicle possible turn direction is " + currVehiclePossibleTurnDirections);

                        // Find the turning street, if there is more than one street in street list
                        if (testCase.getStreetList().size() >= 2
                            && !currVehiclePossibleTurnDirections.startsWith("No turn")) {
                            for (Street street : testCase.getStreetList()) {
                                if (!street.getStreetPropertyValue("road_ID").equals(vehicleStandingStreet.getStreetPropertyValue("road_ID"))) {
                                    for (String possibleTurnDirection : currVehiclePossibleTurnDirections.
                                        split(NavigationDictionary.POSSIBLE_TURN_DIRECTION_DELIMITER)) {
                                                                                // If the candidate street's direction is a continuous street, its direction
                                        // should be in the same axis as possible turn directions. If the street ends
                                        // at the intersection, its moving direction
                                        // choose it as the turning street
                                        if ((NavigationDictionary.isDirectionsInSameAxis(
                                                street.getStreetPropertyValue("road_navigation"), possibleTurnDirection)
                                                && street.getStreetPropertyValue("is_single_road_piece").equals("F"))
                                            || (NavigationDictionary.isDirectionsOpposite(
                                                    street.getStreetPropertyValue("road_navigation"), possibleTurnDirection)
                                                && street.getStreetPropertyValue("is_single_road_piece").equals("T"))
                                            ) {
                                            turnStreet = street;
                                            ConsoleLogger.print('d', String.format("Turn street is %s direction %s coord %s",
                                                turnStreet.getStreetPropertyValue("road_ID"),
                                                turnStreet.getStreetPropertyValue("road_navigation"),
                                                turnStreet.getStreetPropertyValue("road_node_list")));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } // End processing turn direction and find street which the current vehicle turns into

                    String isSingleRoad = vehicleStandingStreet.getStreetPropertyValue("is_single_road_piece");

                    int streetRotateAngle = Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_angle"));

                    ArrayList<String> vehicleCoordAtI = constructedCoordVeh.get(currentVehicle.getVehicleId() - 1);
                    System.out.println("Vehicle Coord before add extra point: " + vehicleCoordAtI.toString());
//                    if (!testCase.getCrashType().contains("turn into")) {
                        if (vehicleCoordAtI.get(vehicleCoordAtI.size() - 1).equals(crashCoord)) {
                            // IF non critical param is specified, change the crash coord of the victim to a point before crash point

                            double extraXCoord = 0;
                            double extraYCoord = 0;
//                            if (travelDirection.equals("W")) {
//
//                                extraXCoord = Double.parseDouble(crashXCoord) - extraDistance;
//                                extraYCoord = Double.parseDouble(crashYCoord);
//
////                                vehicleCoordAtI.add(vehicleCoordAtI.size(), extraXCoord + ":" + extraYCoord);
//                            } // End processing westbound direction
//                            else if (travelDirection.equals("E")) {
//                                extraXCoord = Double.parseDouble(crashXCoord) + extraDistance;
//                                extraYCoord = Double.parseDouble(crashYCoord);
//
////                                vehicleCoordAtI.add(vehicleCoordAtI.size(), extraXCoord + ":" + extraYCoord);
//                            } // End processing eastbound direction
//                            else if (travelDirection.equals("N")) {
//                                extraXCoord = Double.parseDouble(crashXCoord);
//                                extraYCoord = Double.parseDouble(crashYCoord) + extraDistance;
//
////                                vehicleCoordAtI.add(vehicleCoordAtI.size(), extraXCoord + ":" + extraYCoord);
//                            } // End processing northbound direction
                            //else
                            // Test environment
                            isCurrentVehicleTurn = false;
                            isSingleRoad = "T";
                            if (travelDirection.equals("N") || travelDirection.equals("S")
                                || travelDirection.equals("E") || travelDirection.equals("W"))
                            {
                                double radius = Double.parseDouble(
                                    vehicleStandingStreet.getStreetPropertyValue("curve_radius")
                                    .replace("m", ""));

                                ConsoleLogger.print('d', String.format("Extra distance info:\nisCurrentVehicleTurn = %s \n" +
                                    "isSingleRoad = %s \ntravelDirection = %s \ncurrentStreetId = %s",
                                    isCurrentVehicleTurn, isSingleRoad, travelDirection,
                                    vehicleStandingStreet.getStreetPropertyValue("road_ID")));

                                // If this is a continuous road, let the car move to the other end of the intersection
                                if (!isCurrentVehicleTurn && isSingleRoad.equals("F")) {
                                    ConsoleLogger.print('d', "go Straight on non continuous road");
                                    String afterCrashCoord = NavigationDictionary.createNESWCoordBasedOnNavigation(
                                        extraDistance, radius, "forward",
                                        NavigationDictionary.selectDictionaryFromTravelingDirection(travelDirection),
                                        AccidentParam.defaultCoordDelimiter
                                    );
                                    String[] afterCrashCoordXYZ = afterCrashCoord.split(AccidentParam.defaultCoordDelimiter);
                                    extraXCoord = Double.parseDouble(crashXCoord) + Double.parseDouble(afterCrashCoordXYZ[0]);
                                    extraYCoord = Double.parseDouble(crashYCoord) + Double.parseDouble(afterCrashCoordXYZ[1]);
                                    ConsoleLogger.print('d', String.format("afterCrashCoord = (%.2f, %.2f)",
                                        extraXCoord, extraYCoord));
                                    // If the current vehicle runs on a non-continuous road,
                                    // make a turn to the other road
                                } else if (!isCurrentVehicleTurn && isSingleRoad.equals("T")) {

                                    for (Street otherStreet : testCase.getStreetList()) {
                                        // skip if see the same road
                                        if (otherStreet.getStreetPropertyValue("road_ID")
                                                .equals(vehicleStandingStreet.getStreetPropertyValue("road_ID"))) {
                                            continue;
                                        }
                                        int otherStreetRoadAngle = Integer.parseInt(otherStreet.getStreetPropertyValue("road_angle"));
                                        int oppositeAngle = AccidentConstructorUtil.computeOppositeAngle(otherStreetRoadAngle);
                                        ConsoleLogger.print('d', "opposite angle of other street " + oppositeAngle);

                                        // Make the new crash coord further to avoid waypoint conflict
                                        double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(Math.abs(extraDistance),
                                                oppositeAngle);

//                                        String afterCrashCoord = NavigationDictionary.createNESWCoordBasedOnNavigation(
//                                            extraDistance, radius, "backward",
//                                            NavigationDictionary.selectDictionaryFromTravelingDirection(
//                                                otherStreet.getStreetPropertyValue("road_navigation")),
//                                            AccidentParam.defaultCoordDelimiter
//                                        );

//                                        String[] newCoord = afterCrashCoord.split(AccidentParam.defaultCoordDelimiter);

                                        extraXCoord = newCoord[0];
                                        extraYCoord = newCoord[1];


                                        ConsoleLogger.print('d', String.format("No turning & non-continuous road, " +
                                            "extraCoord is (%.2f, %.2f)", extraXCoord, extraYCoord));
                                        ConsoleLogger.print('d', String.format("Other road_ID is %s \n isSingleRoad %s\n" +
                                            "road_node_list = %s",
                                            otherStreet.getStreetPropertyValue("road_ID"),
                                            otherStreet.getStreetPropertyValue("is_single_road_piece"),
                                            otherStreet.getStreetPropertyValue("road_node_list")));
                                        break;
                                    }
                                } else if (isCurrentVehicleTurn) {
                                    System.out.println("Turn Street nodes = " + turnStreet.getStreetPropertyValue("road_node_list") );
                                    System.out.println("Turn Street direction = " + turnStreet.getStreetPropertyValue("road_navigation") );
                                    double[] turningCoord = AccidentConstructorUtil.findTurningPointInTurningStreet(turnStreet, turnDirection, travelDirection);
                                    System.out.println("Turn Coord is " + turningCoord);
                                    if (turningCoord[0] != -1000) {
                                        extraXCoord = turningCoord[0];
                                        extraYCoord = turningCoord[1];
                                    }
                                }


//                                vehicleCoordAtI.add(vehicleCoordAtI.size(), df.format(extraXCoord) + ":" + df.format(extraYCoord));
                            } // End processing southbound direction
                            else // Processing diagonal cardinal direction (NE, NW, ...)
                            {
                                extraXCoord = Double.parseDouble(crashXCoord);
                                extraYCoord = Double.parseDouble(crashYCoord) + extraDistance;
                                // TODO: Compute turning behavior of the car
                                // If this road is a continuous road, then keep going on the opposite direction
                                if (vehicleStandingStreet.getStreetPropertyValue("is_single_road_piece").equals("F")) {

                                    int oppositeAngle = AccidentConstructorUtil.computeOppositeAngle(streetRotateAngle + extraDistance);
                                    double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(extraYCoord,
                                            oppositeAngle);
                                    extraXCoord = newCoord[0];
                                    extraYCoord = newCoord[1];
                                }
                                else
                                {
                                    for (Street otherStreet : testCase.getStreetList()) {
                                        // skip if see the same road
                                        if (otherStreet.getStreetPropertyValue("road_ID")
                                            .equals(vehicleStandingStreet.getStreetPropertyValue("road_ID"))) {
                                            continue;
                                        }
                                        int otherStreetRoadAngle = Integer.parseInt(otherStreet.getStreetPropertyValue("road_angle"));
                                        int oppositeAngle = AccidentConstructorUtil.computeOppositeAngle(otherStreetRoadAngle);
                                        ConsoleLogger.print('d', "opposite angle of other street " + oppositeAngle);

                                        // Make the new crash coord further to avoid waypoint conflict
                                        double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(Math.abs(extraDistance),
                                            oppositeAngle);

                                        extraXCoord = newCoord[0];
                                        extraYCoord = newCoord[1];
                                        ConsoleLogger.print('d', String.format("No turning & non-continuous road, " +
                                            "extraCoord is (%.2f, %.2f)", extraXCoord, extraYCoord));
                                        ConsoleLogger.print('d', String.format("Other road_ID is %s \n" +
                                                "road_node_list = %s",
                                            otherStreet.getStreetPropertyValue("road_ID"),
                                            otherStreet.getStreetPropertyValue("road_node_list")));
                                        break;
                                    }
                                }
                            } // End appending extra crash point to ensure crash happens

                            if (vehicleCoordAtI.size() > 0
                                    && !vehicleCoordAtI.get(vehicleCoordAtI.size() - 1).equals(extraXCoord + ":" + extraYCoord))
                                vehicleCoordAtI.add(vehicleCoordAtI.size(),
                                    AccidentConstructorUtil.displayAs6DecimalNum(extraXCoord) + ":" +
                                    AccidentConstructorUtil.displayAs6DecimalNum(extraYCoord));
                        } // End checking last coord is crash coord

//                    } // End add extra point for turn into path case.

                    System.out.println("Vehicle Coord after add extra point: " + vehicleCoordAtI.toString());
                    // Append grade (zCoord) value into the coords
                    for (int z = 0; z < vehicleCoordAtI.size(); z++)
                    {
                        String currentCoord = vehicleCoordAtI.get(z);
                        // If grading is not concerned, zCoord = 0.
                        if (!AccidentParam.isGradingConcerned)
                            vehicleCoordAtI.set(z, currentCoord + AccidentParam.defaultCoordDelimiter + "0");
                    }

                    // If non-critical is specified, and there is no stopped car, modify the crash wp to create a
                    // near-crash wp
                    if (AccidentConstructorUtil.getNonCriticalDistance() > 0 && stoppedCarID == -1)
                    {
                        String currVehicleCrashCoord = "";

                        currVehicleCrashCoord = vehicleCoordAtI.get(vehicleCoordAtI.size() - 1);

                        String[] currVehicleCrashCoordElems = currVehicleCrashCoord.split(AccidentParam.defaultCoordDelimiter);

                        HashMap<String, String> currVehicleNavDict = NavigationDictionary.selectDictionaryFromTravelingDirection(
                                currentVehicle.getTravellingDirection());

                        if (currentVehicle.getVehicleId() == strikerAndVictim[1].getVehicleId())
                        {
                            ConsoleLogger.print('d', "curr vehicle lane number " + currentVehicle.getTravelOnLaneNumber());

                            // If the current coord contains the crash coord, modify this crash coord
                            if (testCase.getCrashType().contains("straight path")) {
                                vehicleCoordAtI.remove(vehicleCoordAtI.size() - 1);
                                currentVehicle.setVelocity((int)(currentVehicle.getVelocity() * 0.8));
                            }

                            ConsoleLogger.print('d', "victim travel direction = " + currentVehicle.getTravellingDirection());
                            ConsoleLogger.print('d', "victim vehicle path = " + vehicleCoordAtI.toString());

                            String[] xyCoordBackwardNavigation = currVehicleNavDict.get("backward").split(";");

                            double xCoordNonCritical = Double.parseDouble(currVehicleCrashCoordElems[0]) +
                                    NavigationDictionary.setCoordValue(AccidentConstructorUtil.getNonCriticalDistance(),
                                            xyCoordBackwardNavigation[0]);

                            double yCoordNonCritical = Double.parseDouble(currVehicleCrashCoordElems[1]) +
                                    NavigationDictionary.setCoordValue(AccidentConstructorUtil.getNonCriticalDistance(),
                                            xyCoordBackwardNavigation[1]);

                            // If the near crash coord is ahead of the non-critical distance coord, fix it
                            if (Double.parseDouble(currVehicleCrashCoordElems[0]) > xCoordNonCritical) {
                                xCoordNonCritical += 3;
                            } else if (Double.parseDouble(currVehicleCrashCoordElems[1]) > yCoordNonCritical) {
                                yCoordNonCritical += 3;
                            }

                            currVehicleCrashCoord = AccidentConstructorUtil.updateCoordElementAtDimension(
                                    0,
                                    currVehicleCrashCoord,
                                    xCoordNonCritical + "",
                                    AccidentParam.defaultCoordDelimiter);

                            currVehicleCrashCoord = AccidentConstructorUtil.updateCoordElementAtDimension(
                                    1,
                                    currVehicleCrashCoord,
                                    yCoordNonCritical + "",
                                    AccidentParam.defaultCoordDelimiter);


                            vehicleCoordAtI.set(vehicleCoordAtI.size() - 1, currVehicleCrashCoord);
                        }
                        else if (testCase.getCrashType().contains("turn into") &&
                                currentVehicle.getVehicleId() == strikerAndVictim[0].getVehicleId() )
                        {
                            ConsoleLogger.print('d', "victim travel direction = " + currentVehicle.getTravellingDirection());
                            ConsoleLogger.print('d', "victim vehicle path = " + vehicleCoordAtI.toString());

                            String[] xyCoordForwardNavigation = currVehicleNavDict.get("forward").split(";");

                            double xCoordNonCriticalForward = Double.parseDouble(currVehicleCrashCoordElems[0])
                                    + NavigationDictionary.setCoordValue(AccidentConstructorUtil.getNonCriticalDistance(),
                                            xyCoordForwardNavigation[0]);

                            double yCoordNonCriticalForward = Double.parseDouble(currVehicleCrashCoordElems[1])
                                    + NavigationDictionary.setCoordValue(AccidentConstructorUtil.getNonCriticalDistance(),
                                            xyCoordForwardNavigation[1]);

                            String extraForwardCoord = xCoordNonCriticalForward + AccidentParam.defaultCoordDelimiter +
                                                        yCoordNonCriticalForward + AccidentParam.defaultCoordDelimiter;
                            if (!AccidentParam.isGradingConcerned)
                            {
                                extraForwardCoord += "0";
                            }
                            vehicleCoordAtI.set(vehicleCoordAtI.size() - 1, extraForwardCoord);
                        }
                    }

                    currentVehicle.setMovementPath(vehicleCoordAtI);
                } // End looping through vehicles

            } // End checking if impactAtSteps >= 1
        } // End checking if there are only 2 vehicles

        ConsoleLogger.print('d',"All recorded track");
        for (ArrayList<String> movementPath : constructedCoordVeh) {
            ConsoleLogger.print('d',movementPath);
        }

        return constructedCoordVeh;
    }

    public ArrayList<ArrayList<String>> appendPrecrashMovementsForVehicle(ArrayList<ArrayList<String>> vehicleCoordList,
                                                                          ArrayList<VehicleAttr> vehicleList,
                                                                          OntologyHandler parser,
                                                                          boolean curvyRoad,
                                                                          double radius)
    {

        int defaultSpeed = 20;
        // Append the coord based on the first action of the vehicle
        for (VehicleAttr vehicleAttr : vehicleList)
        {
            int vehicleIndexInCoordArr = vehicleAttr.getVehicleId() - 1;
            ArrayList<String> coordOfSelectedVehicle = vehicleCoordList.get(vehicleIndexInCoordArr);

            if (coordOfSelectedVehicle.size() <= 1)
            {
                continue;
            }

            double yCoord = 0;

            for (int i = 1; i <= AccidentParam.SIMULATION_DURATION; i++)
            {

                int estimateVehicleSpeed = AccidentConstructorUtil.getVelocityOfAction(vehicleAttr.getActionList().get(0), parser);
                double xCoord = -100;
                if (estimateVehicleSpeed > 0 && estimateVehicleSpeed != 1000)
                {
//                    ConsoleLogger.print('d',"Vehicle " + vehicleIndexInCoordArr + " Travel 1st act : computedCoord:" + (-1 * estimateVehicleSpeed * i) + " first coord:" + Integer.parseInt(coordOfSelectedVehicle.get(0)));
                    xCoord = -1.0 * estimateVehicleSpeed * i + Double.parseDouble(coordOfSelectedVehicle.get(i - 1).split(":")[0]);

                }
                else if (estimateVehicleSpeed < 0)
                {
//                    ConsoleLogger.print('d',"Vehicle " + vehicleIndexInCoordArr + " Stop 1st act : computedCoord:" + (-1 * estimateVehicleSpeed * i) + " first coord:" + Integer.parseInt(coordOfSelectedVehicle.get(0)));
                    xCoord = -1.0 * defaultSpeed * i + Double.parseDouble(coordOfSelectedVehicle.get(i - 1).split(":")[0]);

                }

                if (curvyRoad)
                {
                    yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
                }
                coordOfSelectedVehicle.add(0,  xCoord + ":" + df.format(yCoord));
            } // End appending precrash coord

            vehicleCoordList.set(vehicleIndexInCoordArr, coordOfSelectedVehicle);

        }
        return vehicleCoordList;
    }

    private void constructRoadNodes(ArrayList<Street> streetList)
    {
        for (int s = 0; s < streetList.size(); s++) {
            Street road = streetList.get(s);
            double gradeDegree = Double.parseDouble(road.getStreetPropertyValue("road_grade_deg"));
            double radius = Double.parseDouble(road.getStreetPropertyValue("curve_radius"));

            double gradeIncrement = 0;
            ArrayList<String> roadCoordList = new ArrayList<String>();
            roadCoordList.add("0 0");
            // If this is a north or southbound road, construct road nodes from point 0,0 along the Y axis
            if (road.getStreetPropertyValue("road_navigation").equals("N")
                    || road.getStreetPropertyValue("road_navigation").equals("S")) {

                // Make 8 nodes along the Y axis, 4 above, 4 below point 0 0
                for (int i = 1; i <= 4; i++) {
                    double currXCoord = 0;
                    double currYCoord = i * distanceBetweenBaseRoadNodes;

                    if (radius != 0) {
                        currXCoord = AccidentConstructorUtil.computeXCircleFunc(radius, currYCoord);
                    }

                    roadCoordList.add(0, currXCoord + " " + currYCoord);

                    if (radius != 0) {
                        currXCoord = AccidentConstructorUtil.computeYCircleFunc(radius, -currYCoord);
                    }
                    roadCoordList.add(roadCoordList.size(), currXCoord + " " + -currYCoord);

                } // End adding 4 nodes


            } // End processing north or southbound road

            else if (road.getStreetPropertyValue("road_navigation").equals("W")
                    || road.getStreetPropertyValue("road_navigation").equals("E")) {

                // Make 8 nodes along the Y axis, 4 above, 4 below point 0 0
                for (int i = 1; i <= 4; i++) {
                    double currXCoord = i * distanceBetweenBaseRoadNodes;
                    double currYCoord = 0;

                    if (radius != 0) {
                        currYCoord = AccidentConstructorUtil.computeYCircleFunc(radius, currXCoord);
                    }

                    roadCoordList.add(0, currXCoord + " " + currYCoord);

                    if (radius != 0) {
                        currYCoord = AccidentConstructorUtil.computeYCircleFunc(radius, -currXCoord);
                    }
                    roadCoordList.add(roadCoordList.size(), -currXCoord + " " + currYCoord);

                } // End adding 8 nodes
            } // End processing west or eastbound road

            // Append the coord list to the road_node_list attr

            road.putValToKey("road_node_list", roadCoordList.toString().replace(",", ";")
                    .replace("[", "").replace("]", ""));
            ConsoleLogger.print('d',"Road " + road.getStreetPropertyValue("road_ID") + " has node list " +
                    road.getStreetPropertyValue("road_node_list"));

            // Append uphill grade value
            String gradeDegreeAtO = "0";
            if (gradeDegree > 0) {
                // increase road grade from 0 to last node
                for (int i = 0; i < roadCoordList.size(); i++) {
                    gradeIncrement = AccidentConstructorUtil.computeGradeIncrement(0, 40, gradeDegree);//gradeDegree / 100 / distanceBetweenBaseRoadNodes;
                    String currentCoord = roadCoordList.get(i);
                    String currentHeight = AccidentParam.df6Digit.format(gradeIncrement * i);

                    // Find the level road and set all of its coord to the current height
                    if (currentCoord.equals("0 0")) {
                        gradeDegreeAtO = currentHeight;

                        // Append the gradeDegreeAtO to the lvl road
                        String levelRoadNodeListWithGrade = appendGradeAtOToLevelRoad(streetList, gradeDegreeAtO);
                        if (levelRoadNodeListWithGrade.equals("")) {
                            ConsoleLogger.print('d',"There is no level road");
                        }
                    }
                    roadCoordList.set(i, currentCoord + " " + currentHeight);


                }
            } else if (gradeDegree < 0) {
                int lastIndex = roadCoordList.size() - 1;
                for (int i = 0; i < roadCoordList.size(); i++) {
                    String currentCoord = roadCoordList.get(i);
                    String currentHeight = AccidentParam.df6Digit.format(gradeIncrement * i);

                    // Find the level road and set all of its coord to the current height
                    if (currentCoord.equals("0 0")) {
                        gradeDegreeAtO = currentHeight;

                        // Append the gradeDegreeAtO to the lvl road
                        String levelRoadNodeListWithGrade = appendGradeAtOToLevelRoad(streetList, gradeDegreeAtO);
                        if (levelRoadNodeListWithGrade.equals("")) {
                            ConsoleLogger.print('d',"There is no level road");
                        }
                    }
                }
                // increase road grade from 0 to last node
                for (int i = roadCoordList.size() - 1; i > 0; i--) {
                    gradeIncrement = AccidentConstructorUtil.computeGradeIncrement(0, 40, gradeDegree); //gradeDegree / 100 / distanceBetweenBaseRoadNodes;
                    roadCoordList.set(i, roadCoordList.get(i) + " " + AccidentParam.df6Digit.format(gradeIncrement * (lastIndex - i)));
                }
            }

            road.putValToKey("road_node_list", roadCoordList.toString().replace(",", ";")
                    .replace("[", "").replace("]", ""));

            ConsoleLogger.print('d',"Road Coord List is " + roadCoordList);
        }
    }

    private String appendGradeAtOToLevelRoad(ArrayList<Street> streetList, String gradeDegreeAtO)
    {
        for (Street assumedLvlStreet : streetList)
        {
            String lvlNodeListStr = "";
            if (assumedLvlStreet.getStreetPropertyValue("road_grade_deg").equals("0"))
            {
                String[] lvlNodeListArr = assumedLvlStreet
                        .getStreetPropertyValue("road_node_list").split(";");
                for (int z = 0; z < lvlNodeListArr.length; z++)
                {
                    lvlNodeListStr += lvlNodeListArr[z] + " " + gradeDegreeAtO + ";";
                }
                String roadNodeListStr = lvlNodeListStr.substring(0, lvlNodeListStr.length() - 1);
                assumedLvlStreet.putValToKey("road_node_list", roadNodeListStr);
                ConsoleLogger.print('d',"Add GradeAtO to Level Street ID " + assumedLvlStreet.getStreetPropertyValue("road_ID") + " " + roadNodeListStr);
                return roadNodeListStr;
            }

        }
        return "";
    }

    private double calculateTravelTimeBasedOnAcceleration(double velocity, double distance)
    {

        double accumulateDistance = 0;
        double chosenAcceleration = AccidentParam.accelerationTo20Mph;

        if (velocity <= 40)
        {
            chosenAcceleration = AccidentParam.accelerationTo20Mph;
        }

        // Calculate accumulated distance using formula s = v0 + a(n - 0.5) with v0 = 0 and n = i
        for (int i = 1; ;i++)
        {
            accumulateDistance += chosenAcceleration * i - chosenAcceleration / 2;
            if (accumulateDistance > distance)
            {
                return i - 1;
            }
        }
    }


    /*
     *  Compute the travelling time and distance needed to accelerate up to the target velocity
     *
     *  @param velocity  the target velocity used to compute the accelerated distance and time
     */

    private ArrayList<Double> computeDistanceAndTimeWithAcceleration(double velocity)
    {
        ArrayList<Double> distanceAndTime = new ArrayList<Double>();

        double chosenAcceleration = AccidentParam.accelerationTo20Mph;

        if (velocity <= 40)
        {
            chosenAcceleration = AccidentParam.accelerationTo20Mph;
        }

        double reachVelocityTime = velocity / chosenAcceleration;
        double acceleratedTravellingDistance = 0.5 * chosenAcceleration * reachVelocityTime * reachVelocityTime;

        distanceAndTime.add(acceleratedTravellingDistance);
        distanceAndTime.add(reachVelocityTime);

        return distanceAndTime;
    }

}

// Append the vehicle coord depending on the direction, we may not need to take acceleration into account now

//                        for (int i = 1; i <= AccidentParam.SIMULATION_DURATION; i++)
//                                {
//                                double vehicleCoordX = Double.parseDouble(crashXCoord);
//                                double vehicleCoordY = Double.parseDouble(crashYCoord);
//                                if (travelDirection.equals("W"))
//                                {
//                                vehicleCoordX = Double.parseDouble(crashXCoord) + currentVehicle.getVelocity() * i;
//                                vehicleCoordY = Double.parseDouble(crashYCoord);
//
//                                if (radius != 0)
//                                {
//                                vehicleCoordY = AccidentConstructorUtil.computeYCircleFunc(radius, vehicleCoordX);
//                                }
//                                } // End processing westbound direction
//
//                                else if (travelDirection.equals("SW"))
//                                {
//                                vehicleCoordX = Double.parseDouble(crashXCoord) + currentVehicle.getVelocity() * i;
//                                vehicleCoordY = Double.parseDouble(crashYCoord);
//
//                                if (radius != 0)
//                                {
//                                vehicleCoordY = AccidentConstructorUtil.computeYCircleFunc(radius, vehicleCoordX);
//                                }
//                                } // End processing southwestbound direction
//
//                                else if (travelDirection.equals("S"))
//                                {
//                                vehicleCoordX = Double.parseDouble(crashXCoord);
//                                vehicleCoordY = Double.parseDouble(crashYCoord) + currentVehicle.getVelocity() * i;
//
//                                if (radius != 0)
//                                {
//                                vehicleCoordX = AccidentConstructorUtil.computeYCircleFunc(radius, vehicleCoordY);
//                                }
//
//                                } // End processing westbound direction
//
//                                vehicleCoordList.add(0, vehicleCoordX + ":" + vehicleCoordY);
//                                } // End setting coord for remaining actions
