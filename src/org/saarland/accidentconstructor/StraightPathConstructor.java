package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.RoadShape;
import org.saarland.accidentelementmodel.Street;
import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.OntologyHandler;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
        AccidentConstructorUtil.findImpactedStepsAndVehicles(impactAtSteps, impactedVehiclesAtSteps, vehicleList);

        // If there are 2 vehicles, construct the only crash point at 0:0
        if (vehicleList.size() == 2) {
            constructedCoordVeh.get(0).add(impactAtSteps.get(0), "0:0");
            constructedCoordVeh.get(1).add(impactAtSteps.get(0), "0:0");

            ConsoleLogger.print('d',"impactAtSteps size: " + impactAtSteps.size());
            if (impactAtSteps.size() >= 1) {
                // TODO: Locate the impacted coord first by looking at the travelling direction
                String crashXCoord = "0";
                String crashYCoord = "0";

                for (int v = 0; v < vehicleList.size(); v++) {
                    VehicleAttr currentVehicle = vehicleList.get(v);

                    Street vehicleStandingStreet = currentVehicle.getStandingStreet();

                    ConsoleLogger.print('d',"Current vehicle ID is " + currentVehicle.getVehicleId());

                    ConsoleLogger.print('d',"Standing Street is " + vehicleStandingStreet);

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
                            vehicleXCoord = "" + ((laneNum / 2 - 1) * AccidentParam.laneWidth + AccidentParam.laneWidth / 2.0);

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

                        ConsoleLogger.print('d',"distance between moving car and crash point is " + distanceCurrVehicleAndCrashPoint);


                        ConsoleLogger.print('d',"Curvy Road of street " + vehicleStandingStreet.getStreetPropertyValue("road_ID") + " ? " + curvyRoad);
                        ConsoleLogger.print('d',"Road Shape " + testCase.getTestCaseProp().get("road_shape"));

                        String currentVehicleXCoord = crashXCoord;
                        String currentVehicleYCoord = crashYCoord;

                        // If travel to the west, add the distance to the x and keep the y intact
                        if (travelDirection.equals("W"))
                        {
                            currentVehicleXCoord = df.format(Double.parseDouble(crashXCoord) + distanceCurrVehicleAndCrashPoint);
                            if (radius != 0)
                            {
                                currentVehicleYCoord = df.format(AccidentConstructorUtil.computeYCircleFunc(radius,
                                        Double.parseDouble(currentVehicleXCoord)));
                            }
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

                        // Set Leave trigger distance for the stopped car
                        AccidentConstructorUtil.findVehicleBasedOnId(stoppedCarID, vehicleList).setLeaveTriggerDistance(leaveTriggerDistance);

                        ConsoleLogger.print('d',"Leave Trigger Distance " + leaveTriggerDistance);

                        // Construct the coords before the "prior crash" point
                        for (int i = indexActionBeforeCrash - 1; i >= -1 * AccidentParam.simulationDuration; i--)
                        {
                            if (travelDirection.equals("W"))
                            {
                                double vehicleCoordX = Double.parseDouble(currentVehicleXCoord) + currentVehicle.getVelocity();
                                double vehicleCoordY = Double.parseDouble(currentVehicleYCoord);
                                if (radius != 0)
                                {
                                     vehicleCoordY = AccidentConstructorUtil.computeYCircleFunc(radius, vehicleCoordX);
                                }

                                currentVehicleXCoord = vehicleCoordX + "";
                                currentVehicleYCoord = vehicleCoordY + "";

                                if (i >= 0)
                                    vehicleCoordList.set(i, vehicleCoordX + ":" + vehicleCoordY);
                                else if (i < 0)
                                    vehicleCoordList.add(0, vehicleCoordX + ":" + vehicleCoordY);
                            } // End processing westbound direction
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
                                * AccidentParam.simulationDuration;

                        }
                        else if (currentVehicle.equals(strikerAndVictim[1]))
                        {
                            VehicleAttr striker = strikerAndVictim[0];
                            ArrayList<Double> strikerDistanceAndTime = computeDistanceAndTimeWithAcceleration(striker.getVelocity());
                            nearCrashDistance = AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity())
                                    * (strikerDistanceAndTime.get(1) + 2 - currentVehDistanceAndTime.get(1));
                        }
                        totalDistance = currentVehDistanceAndTime.get(0) + nearCrashDistance;
                        /* Old implementation
                        int roadAngle =  Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_angle"));

                        double nearCrashDistance =  AccidentConstructorUtil.convertMPHToMS(currentVehicle.getVelocity())
                                * AccidentParam.simulationDuration;
                        double totalDistance = currentVehDistanceAndTime.get(0) + nearCrashDistance; */



                        ConsoleLogger.print('d',"nearCrashDistance = " + nearCrashDistance);
                        ConsoleLogger.print('d',"totalDistance = " + totalDistance);

                        // Compute near crash node for each road direction
                        if (!travelDirection.equals("S") && !travelDirection.equals(""))
                            /*travelDirection.equals("SE") || travelDirection.equals("SW")
                            || travelDirection.equals("NE") || travelDirection.equals("NW"))*/
                        {
                            // For diagonal direction, the near crash length is equal to crashYCoord + nearCrashDistance
                            double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(
                                    Double.parseDouble(crashYCoord) + nearCrashDistance, roadAngle);

                            // if there is a curve radius, compute the new xCoord
                            if (radius != 0)
                            {
                                newCoord[0] = AccidentConstructorUtil.computeXCircleFunc(radius, newCoord[1]);
                            }

                            vehicleCoordList.add(0, df.format(newCoord[0] - AccidentParam.laneWidth + 2)
                                    + ":" + df.format(newCoord[1]));

                            if (radius != 0)
                            {
                                newCoord[0] = AccidentConstructorUtil.computeXCircleFunc(radius, newCoord[1]);
                            }
                            // Compute and set the init position
                            newCoord = AccidentConstructorUtil.computeNewCoordOfRotatedLine(
                                Double.parseDouble(crashYCoord) + totalDistance, roadAngle);

                            newCoord[0] = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius, newCoord[0], newCoord[1]);

                            vehicleCoordList.add(0, df.format(newCoord[0])
                                    + ":" + df.format(newCoord[1]));
                        } // End setting init and near crash coord for diagonal directions
                        else if (travelDirection.equals("S"))
                        {
                            // Compute near crash distance, for S, it is crashYcoord + nearCrashDistance
                            double currentVehicleXCoord = Double.parseDouble(crashXCoord);
                            double currentVehicleYCoord = Double.parseDouble(crashYCoord) + nearCrashDistance;

                            currentVehicleXCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius,
                                    currentVehicleXCoord, currentVehicleYCoord);

                            vehicleCoordList.add(0, df.format(currentVehicleXCoord) + ":"
                                    + df.format(currentVehicleYCoord));

                            // Compute Init position

                            currentVehicleYCoord = Double.parseDouble(crashYCoord) + totalDistance;

                            currentVehicleXCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius,
                                    currentVehicleXCoord, currentVehicleYCoord);

                            vehicleCoordList.add(0, df.format(currentVehicleXCoord ) + ":"
                                    + df.format(currentVehicleYCoord));
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

                // Append an extra distance to ensure crash happens. extra distance shows the intention moving direction
                // of the car
                for (int v = 0; v < vehicleList.size(); v++)
                {
                    VehicleAttr currentVehicle = vehicleList.get(v);

                    Street vehicleStandingStreet = currentVehicle.getStandingStreet();

                    String isSingleRoad = vehicleStandingStreet.getStreetPropertyValue("is_single_road_piece");

                    int streetRotateAngle = Integer.parseInt(vehicleStandingStreet.getStreetPropertyValue("road_angle"));

                    ArrayList<String> vehicleCoordAtI = constructedCoordVeh.get(currentVehicle.getVehicleId() - 1);

                    if (vehicleCoordAtI.get(vehicleCoordAtI.size() - 1).equals(crashCoord))
                    {
                        String travelDirection = currentVehicle.getTravellingDirection();

                        if (travelDirection.equals("W"))
                        {
                            double extraXCoord = Double.parseDouble(crashXCoord) - extraDistance;
                            double extraYCoord = Double.parseDouble(crashYCoord);

                            vehicleCoordAtI.add(vehicleCoordAtI.size(), extraXCoord + ":" + extraYCoord);
                        } // End processing westbound direction

                        else if (travelDirection.equals("N"))
                        {
                            double extraXCoord = Double.parseDouble(crashXCoord);
                            double extraYCoord = Double.parseDouble(crashYCoord) +  extraDistance;

                            vehicleCoordAtI.add(vehicleCoordAtI.size(), extraXCoord + ":" + extraYCoord);
                        } // End processing N direction
                        else if (travelDirection.equals("S"))
                        {
                            double extraXCoord = Double.parseDouble(crashXCoord);
                            double extraYCoord = Double.parseDouble(crashYCoord) - extraDistance;

                            // If this is a single road piece, append the road based on other road direction
                            if (isSingleRoad.equals("T"))
                            {
                                for (Street otherStreet : testCase.getStreetList())
                                {
                                    // skip if see the same road
                                    if (otherStreet.getStreetPropertyValue("road_ID")
                                            .equals(vehicleStandingStreet.getStreetPropertyValue("road_ID")))
                                    {
                                        continue;
                                    }
                                    int otherStreetRoadAngle = Integer.parseInt(otherStreet.getStreetPropertyValue("road_angle"));
                                    int oppositeAngle = AccidentConstructorUtil.computeOppositeAngle(otherStreetRoadAngle);
                                    ConsoleLogger.print('d',"opposite angle of other street " + oppositeAngle);

                                    // Make the new crash coord further to avoid waypoint conflict
                                    double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(Math.abs(extraYCoord),
                                            oppositeAngle);

                                    extraXCoord = newCoord[0];
                                    extraYCoord = newCoord[1];
                                }
                            }

                            vehicleCoordAtI.add(vehicleCoordAtI.size(), df.format(extraXCoord) + ":" + df.format(extraYCoord));
                        } // End processing N direction
                        else
                        {
                            // check direction of other road to determine the

                            double extraXCoord = Double.parseDouble(crashXCoord);
                            double extraYCoord = Double.parseDouble(crashYCoord) +  extraDistance;

                            // If this road is a continuous road, then keep going on the opposite direction
                            if (vehicleStandingStreet.getStreetPropertyValue("is_single_road_piece").equals("F"))
                            {

                                int oppositeAngle = AccidentConstructorUtil.computeOppositeAngle(streetRotateAngle + extraDistance);
                                double newCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(extraYCoord,
                                        oppositeAngle);
                                extraXCoord = newCoord[0];
                                extraYCoord = newCoord[1];
                            }
                            else // TODO: Compute based on other road if this is a single_road_piece
                            {

                            }


                            vehicleCoordAtI.add(vehicleCoordAtI.size(), extraXCoord + ":" + extraYCoord);
                        }
                    } // End checking last coord is crash coord

                    currentVehicle.setMovementPath(vehicleCoordAtI);
                } // End appending extra crash point to ensure crash happens

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

            for (int i = 1; i <= AccidentParam.simulationDuration; i++)
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

    private ArrayList<Double> computeDistanceAndTimeWithAcceleration(double velocity)
    {
        ArrayList<Double> distanceAndTime = new ArrayList<Double>();

        double accumulateDistance = 0;
        double accumulateVelocity = 0;
        double chosenAcceleration = AccidentParam.accelerationTo20Mph;

        if (velocity <= 40)
        {
            chosenAcceleration = AccidentParam.accelerationTo20Mph;
        }

        double reachVelocityTime = velocity / chosenAcceleration;
        double acceleratedTravellingDistance = 0.5 * chosenAcceleration * reachVelocityTime * reachVelocityTime;

        distanceAndTime.add(acceleratedTravellingDistance);
        distanceAndTime.add(reachVelocityTime);
//        // Calculate accumulated distance using formula s = v0 + a(n - 0.5) with v0 = 0 and n = i
//        for (int i = 1; ;i++)
//        {
//            accumulateDistance += chosenAcceleration * i - chosenAcceleration / 2;
//            accumulateVelocity += chosenAcceleration * i;
//            if (accumulateDistance > distance)
//            {
//                return i - 1;
//            }
//        }

        // Compute the distance

        return distanceAndTime;
    }

}

// Append the vehicle coord depending on the direction, we may not need to take acceleration into account now

//                        for (int i = 1; i <= AccidentParam.simulationDuration; i++)
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
