package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.RoadShape;
import org.saarland.accidentelementmodel.Street;
import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.OntologyHandler;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;

public class SideswipeConstructor {
    DecimalFormat df;

    public SideswipeConstructor()
    {
        df = AccidentParam.df6Digit;
    }


    public ArrayList<ArrayList<String>> constructAccidentScenario(ArrayList<VehicleAttr> vehicleList,
                                                                  OntologyHandler parser,
                                                                  TestCaseInfo testCase)
    {
        ArrayList<ArrayList<String>> constructedCoordVeh = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<Integer>> impactAtSteps = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<ArrayList<VehicleAttr>>> impactedVehiclesAtSteps
            = new ArrayList<ArrayList<ArrayList<VehicleAttr>>>();


        for (VehicleAttr vehicle : vehicleList)
        {
            constructedCoordVeh.add(vehicle.getVehicleId() - 1, new ArrayList<String>());
            impactAtSteps.add(new ArrayList<Integer>());
            impactedVehiclesAtSteps.add(new ArrayList<ArrayList<VehicleAttr>>());
        }

        constructedCoordVeh = AccidentConstructorUtil.fillCoordOfVehicles(constructedCoordVeh, vehicleList.get(0).getActionList().size());

        VehicleAttr[] strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictim(vehicleList.get(0), vehicleList.get(1));

        boolean impactFound = false;
        // If there are 2 vehicles, find the impact point of victim and striker to construct coordinate.
        if (vehicleList.size() == 2) {
//            constructedCoordVeh.get(0).add("0:0");
//            constructedCoordVeh.get(1).add("0:0");

            for (VehicleAttr vehicle : vehicleList) {
                // Find the index of the impact action in the action lists of both vehicles

                ArrayList<Integer> vehicleImpactsAtStep = impactAtSteps.get(vehicle.getVehicleId() - 1);

                AccidentConstructorUtil.findImpactedStepsAndVehicles(
                    vehicle,
                    impactAtSteps.get(vehicle.getVehicleId() - 1),
                    impactedVehiclesAtSteps.get(vehicle.getVehicleId() - 1),
                    vehicleList);

                if (impactAtSteps.get(vehicle.getVehicleId() - 1).size() > 0) {
                    impactFound = true;
                    constructedCoordVeh.get(vehicle.getVehicleId() - 1).add(
                        vehicleImpactsAtStep.get(0), "0:0");
                } else {
                    vehicle.getActionList().add("hit");
                    impactAtSteps.get(vehicle.getVehicleId() - 1).add(vehicle.getActionList().size() - 1);
                    constructedCoordVeh.get(vehicle.getVehicleId() - 1).add("0:0");
                }
            }
        }




//        AccidentConstructorUtil.findImpactedStepsAndVehicles(impactAtSteps, impactedVehiclesAtSteps, vehicleList);

        // Set the crash coord
//        ArrayList<String> vehicleCoordStriker = constructedCoordVeh.get(strikerVehicle.getVehicleId() - 1);
//        vehicleCoordStriker.set(impactAtSteps.get(0), "0:0");

        // Construct the coords before crash
        ConsoleLogger.print('d',"impactAtSteps size: " + impactAtSteps.size());
//        if (impactAtSteps.size() >= 1) {
        if (impactFound) {
            Street currentStreet = null;

            if (testCase.getStreetList().size() == 1)
            {
                currentStreet = testCase.getStreetList().get(0);
            }
            boolean needLeaveTriggerDistance = false;
            boolean curvyRoad = false;
            double radius = 0;

            if (!currentStreet.getStreetPropertyValue("road_shape").equals(RoadShape.STRAIGHT))
            {
                curvyRoad = true;
                radius = Double.parseDouble(currentStreet.getStreetPropertyValue("curve_radius").replace("m", ""));
            }

            ConsoleLogger.print('d',"Curvy Road? " + curvyRoad);
            ConsoleLogger.print('d',"Road Shape " + currentStreet.getStreetPropertyValue("road_shape"));
            // Construct coord before crash

            for (int v = 0; v < vehicleList.size(); v++)
            {
                VehicleAttr currentVehicle = vehicleList.get(v);
                ConsoleLogger.print('d', String.format("vehicle %s",currentVehicle.getVehicleId()));

                ArrayList<String> vehicleCoordList = constructedCoordVeh.get(currentVehicle.getVehicleId() - 1);

                LinkedList<String> vehicleActionList = currentVehicle.getActionList();

                // Find if the vehicle is doing a consistent action before crash
                int estimateActionAtIVelocity = 0;

                boolean isConsistentAction = true;

                for (int i = 0;
                     i < impactAtSteps.get(currentVehicle.getVehicleId() - 1).get(0); i++) {
                    String actionAtI = vehicleActionList.get(i);

                    ConsoleLogger.print('d', String.format("ActionAtI is %s for vehicle %s",
                        actionAtI, currentVehicle.getVehicleId()));

                    String nextAction = vehicleActionList.get(i + 1);

//                    if (!actionAtI.startsWith("hit") && !actionAtI.equalsIgnoreCase("endHit")) {
                        estimateActionAtIVelocity = Integer.parseInt(parser.findExactConcept(actionAtI)
                                .getDataProperties().get("velocity"));

                        int estimateNextActionVelocity = Integer.parseInt(parser.findExactConcept(vehicleActionList.get(i + 1))
                                .getDataProperties().get("velocity"));

                        // if we have only 2 actions, then the action is consistent, as the vehicle
                        // performs only 1 action before crashing
                        if (vehicleActionList.size() == 2 && estimateNextActionVelocity == 1000) {
                            isConsistentAction = true;
                            break;
                        }

                        if (estimateActionAtIVelocity != estimateNextActionVelocity) {
                            ConsoleLogger.print('d',"inconsistent action at " + i);
                            isConsistentAction = false;
                            break;
                        }
//                    }
                }

                if (vehicleActionList.get(0).startsWith("park"))
                {
                    int onStreetStatus = currentVehicle.getOnStreet();
                    ConsoleLogger.print('d',"Found park at 0, status is " + onStreetStatus);
                    if (onStreetStatus < 1)
                    {
                        //Street currentStreet = null;
                        // TODO: Find the street containing a car if there are multiple streets

                        double xCoord = -3;

                        if (currentStreet != null)
                        {
                            // If the car parked on the pavement, increase the yCoord by half street size + half pavement size + 1
                            if (onStreetStatus == 0)
                            {
                                ConsoleLogger.print('d',"Set coord for parking on the pavement");
                                double yCoord = (Double.parseDouble(currentStreet.getStreetPropertyValue("lane_num"))
                                        * AccidentParam.laneWidth) / 2
                                        + AccidentParam.laneWidth / 2;
//                                if (currentVehicle.getStandingRoadSide().equals("left"))
//                                {
//                                    yCoord = ;
//                                }
                                if (currentVehicle.getStandingRoadSide().equals("right"))
                                {
                                    yCoord = -1.0 * yCoord;
                                }
                                vehicleCoordList.set(0, xCoord + ":" + df.format(yCoord));
                            }
                            else if (onStreetStatus == -1) // Set yCoord by half street size + half parking line size
                            {
                                ConsoleLogger.print('d',"Set coord for parking on the parking line");
                                double yCoord = (Double.parseDouble(currentStreet.getStreetPropertyValue("lane_num"))
                                        * AccidentParam.laneWidth) / 2
                                        + AccidentParam.parkingLineWidth / 2.0;
                                if (currentVehicle.getStandingRoadSide().equals("left")) {
                                    //yCoord += 1;
                                    yCoord += 0;
                                } else if (currentVehicle.getStandingRoadSide().equals("right")) {
                                    yCoord = -1.0 * yCoord + 1;
                                }
                                vehicleCoordList.set(0, xCoord + ":" + df.format(yCoord));
                            } else {
                                ConsoleLogger.print('e',"Invalid parking position " + onStreetStatus);
                            }
                        } // End checking if currentStreet is null
                        // Check if other car is parked initially, then leave trigger distance is needed. Only do this
                        // when the current vehicle is the last item in the vehicle list
                        if (v == strikerAndVictim[1].getVehicleId() - 1)
                        {
                            VehicleAttr strikerVehicle = strikerAndVictim[0];

                            testCase.putValToKey("need_trigger", "T");
                            currentVehicle.setLeaveTriggerDistance(computeLeaveTriggerDistance(
                                    constructedCoordVeh.get(currentVehicle.getVehicleId() - 1).get(0),
                                    "0:0", strikerVehicle, currentVehicle));
                            ConsoleLogger.print('d',"Compute Leave Trigger distance = " + currentVehicle.getLeaveTriggerDistance());
                            ConsoleLogger.print('d', "victim travel direction " + currentVehicle.getTravellingDirection());

                        }
                    } // End checking if the car is parked
                }

                if (isConsistentAction)
                {
                    // Construct the x and y coords before crash point
                    ConsoleLogger.print('d',"consistent action with velocity of " + estimateActionAtIVelocity);
                    if (estimateActionAtIVelocity > 0) {
                        ConsoleLogger.print('d',"Vehicle " + v + " has consistent moving action");

                        // Set all the
                        ArrayList<Integer> vehicleImpactAtSteps = impactAtSteps.get(currentVehicle.getVehicleId() - 1);
                        for (int i = 0 ; i < vehicleImpactAtSteps.get(0); i++)
                        {
                            double xCoord = estimateActionAtIVelocity * -1.0 * (vehicleImpactAtSteps.get(0) - i);
                            double yCoord = 0;
                            if (curvyRoad) {
//                            yCoord = 1000 + -1.0 * Math.sqrt(1000000 - Math.pow(xCoord, 2));
                                yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);

                            }
                            vehicleCoordList.set(i, xCoord + ":" + df.format(yCoord));
                        }

                        // Add Accelerated Distance to ensure that the vehicle reaches its speed before traveling with constant speed for 2s
                        ArrayList<Double> distanceAndTimeWithAcceleration = AccidentConstructorUtil.computeDistanceAndTimeWithAcceleration(currentVehicle.getVelocity());

                        double xCoord = -1.0 * distanceAndTimeWithAcceleration.get(0) + Double.parseDouble(vehicleCoordList.get(0).split(":")[0]);
                        double yCoord = 0;
                        if (curvyRoad)
                        {
                            yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
                        }

                        vehicleCoordList.add(0,  xCoord + ":" + df.format(yCoord));

                        // Check if other car is parked initially, then leave trigger distance is needed. Only do this
                        // when the current vehicle is the last item in the vehicle list
                        if (v == strikerAndVictim[1].getVehicleId() - 1)
                        {
//                            VehicleAttr victimVehicle = strikerAndVictim[1];
//                            if (victimVehicle.getActionList().get(0).startsWith("park")) {
//                                ConsoleLogger.print('d',"Found other vehicle parks initially");
//                                victimVehicle.setLeaveTriggerDistance(computeLeaveTriggerDistance(
//                                        constructedCoordVeh.get(victimVehicle.getVehicleId() - 1).get(0), "0:0",
//                                        currentVehicle, victimVehicle));
//                                testCase.putValToKey("need_trigger", "T");
//                            }
                            if (currentVehicle.getActionList().get(0).startsWith("park")) {
                                ConsoleLogger.print('d',"Found other vehicle parks initially");
                                currentVehicle.setLeaveTriggerDistance(computeLeaveTriggerDistance(
                                        constructedCoordVeh.get(currentVehicle.getVehicleId() - 1).get(0), "0:0",
                                        strikerAndVictim[0], strikerAndVictim[1]));
                                testCase.putValToKey("need_trigger", "T");
                            }
                        }
                    }

                    // Construct the static car position, remove the collision
                    else if (estimateActionAtIVelocity == 0)
                    {

                        vehicleActionList.remove("0");
                        ConsoleLogger.print('d',"Vehicle " + v + " has consistent stop action");

                        // Check if this is a parking action

                    } // End checking stop actions

                } // End checking if action is consistent
                else // Action is not consistent
                {
                    // Construct movement style if this is a non-parking vehicle
                    if (!vehicleActionList.get(0).startsWith("park"))
                    {
                        int indexAtFirstImpact = impactAtSteps.get(currentVehicle.getVehicleId() - 1).get(0);
                        // Set the x and y Coord for each action
                        for (int s = 0; s < indexAtFirstImpact; s++)
                        {
                            int estimateCurrentActionVelocity = Integer.parseInt(parser.findExactConcept(vehicleActionList.get(s))
                                    .getDataProperties().get("velocity"));

                            double xCoord = -1.0 * estimateCurrentActionVelocity * (indexAtFirstImpact - s);
                            double yCoord = 0;
                            if (curvyRoad)
                            {
                                yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
                            }
                            vehicleCoordList.set(s, xCoord + ":" + yCoord);
                        }
                    }
                }
                constructedCoordVeh.set(currentVehicle.getVehicleId() - 1, vehicleCoordList);
            } // End looping through each vehicle




            ConsoleLogger.print('d',"Before Remove");
            for (int i = 0; i < vehicleList.size(); i++)
            {

                ConsoleLogger.print('d',"Vehicle #" + (i + 1) + " coord list:");
                for (String coord : constructedCoordVeh.get(i))
                {
                    ConsoleLogger.print('n',coord + " ");

                }
                ConsoleLogger.print('d', "");
            }

            // For NOW, remove all coord that contain only 0 to see how things happen naturally
            for (int i = 0; i < constructedCoordVeh.size(); i++)
            {
                ArrayList<String> coordList = constructedCoordVeh.get(i);
                if (coordList.size() > 1)
                {
                    for (int j = coordList.size() - 1; j >= 0; j--)
                    {
                        if (coordList.get(j).equals("0"))
                        {
                            ConsoleLogger.print('d',"FOund only 0 at " + j);
                            coordList.remove(j);

                            ConsoleLogger.print('d',"Coord list after remove " + coordList);
                        }
                    }

                }
                else
                {
                    if (coordList.get(0).equals("0"))
                    {
                        coordList.set(0, "0:0");
                    }
                }
            }


        } // End checking if there is only 1 impact location

//        appendPrecrashMovementsForVehicle(constructedCoordVeh, vehicleList, parser, curvyRoad, radius);

        ConsoleLogger.print('d', "add far away wp");
        double extraDistanceX = AccidentConstructorUtil.getNonCriticalDistance() == 0 ?
                5 : AccidentConstructorUtil.getNonCriticalDistance();
        double extraDistanceY = AccidentConstructorUtil.getNonCriticalDistance() == 0 ?
                2 : 0;
//        double extraDistanceStriker = AccidentConstructorUtil.getNonCriticalDistance() == 0 ?
//                6 : 0;

        // Victim enter the road and go forward, but make a little wider curve
        constructedCoordVeh.get(strikerAndVictim[1].getVehicleId() - 1).add
            (extraDistanceX + AccidentParam.defaultCoordDelimiter + extraDistanceY);

        // Striker go straight a bit
        constructedCoordVeh.get(strikerAndVictim[0].getVehicleId() - 1).add
            (extraDistanceX + AccidentParam.defaultCoordDelimiter + "0");




        for (int i = 0; i < vehicleList.size(); i++)
        {
            // Append grading
            for (int j = 0; j < constructedCoordVeh.get(i).size(); j++)
            {
                ArrayList<String> currentCoordList = constructedCoordVeh.get(i);
                if (!AccidentParam.isGradingConcerned)
                {
                    currentCoordList.set(j, currentCoordList.get(j) + AccidentParam.defaultCoordDelimiter + "0");
                }
            }

            vehicleList.get(i).setMovementPath(constructedCoordVeh.get(i));
            ConsoleLogger.print('d',"Vehicle #" + (i + 1) + " coord list:");
            for (String coord : constructedCoordVeh.get(i))
            {
                ConsoleLogger.print('n',coord + " ");

            }
            ConsoleLogger.print('d', "");
        }
        return constructedCoordVeh;
    }

    // Compute Leave Trigger Distance
    private double computeLeaveTriggerDistance(String initCoordVictim, String crashPoint,
                                               VehicleAttr striker, VehicleAttr victim) {
        String delimiter = ":";
        double xCoordInitVictim = Double.parseDouble(initCoordVictim.split(delimiter)[0]);
        double yCoordInitVictim = Double.parseDouble(initCoordVictim.split(delimiter)[1]);
        double xCoordCrash = Double.parseDouble(crashPoint.split(delimiter)[0]);
        double yCoordCrash = Double.parseDouble(crashPoint.split(delimiter)[1]);


        double victimTravelDistance = AccidentConstructorUtil.computeDistanceBetweenTwo2DPoints(
                xCoordInitVictim, yCoordInitVictim, xCoordCrash, yCoordCrash);

        double approachingTimeInSec = Math.sqrt(victimTravelDistance / (0.5 * AccidentParam.accelerationTo20Mph)) + 0.4;

        // Travel distance with constant speed of striker
        double strikerTravelDistance = approachingTimeInSec * AccidentConstructorUtil.convertMPHToMS(striker.getVelocity());

        ConsoleLogger.print('d',"Vic Travel Dis: " +victimTravelDistance);
        ConsoleLogger.print('d',"Vic Approaching Time: " + approachingTimeInSec);
        ConsoleLogger.print('d',"Stri Travel Dis: " +strikerTravelDistance);

        // Compute the trigger distance between victim and striker
        double triggerDistance = AccidentConstructorUtil.computeDistanceBetweenTwo2DPoints(
                xCoordCrash - strikerTravelDistance, yCoordCrash, xCoordInitVictim, yCoordInitVictim);

        // If extra distance to make the crash non-critical is given, add the distance to the computation
        ConsoleLogger.print('d',"Trigger Distance before: " + triggerDistance);
        triggerDistance += AccidentConstructorUtil.getNonCriticalDistance();
        ConsoleLogger.print('d',"Trigger Distance after: " + triggerDistance);
        return triggerDistance;
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
            int estimateVehicleSpeed = AccidentConstructorUtil.getVelocityOfAction(vehicleAttr.getActionList().get(0), parser);
            double desireSpeed = AccidentConstructorUtil.convertMPHToMS(defaultSpeed);
            if (estimateVehicleSpeed > 0 && estimateVehicleSpeed != 1000)
            {
                desireSpeed = AccidentConstructorUtil.convertMPHToMS(estimateVehicleSpeed);
            }

            for (int i = 1; i <= AccidentParam.SIMULATION_DURATION; i++)
            {
                double xCoord = -100;
                xCoord = -1.0 * desireSpeed * i + Double.parseDouble(coordOfSelectedVehicle.get(i - 1).split(":")[0]);

//                if (estimateVehicleSpeed > 0 && estimateVehicleSpeed != 1000)
//                {
////                    ConsoleLogger.print('d',"Vehicle " + vehicleIndexInCoordArr + " Travel 1st act : computedCoord:" + (-1 * estimateVehicleSpeed * i) + " first coord:" + Integer.parseInt(coordOfSelectedVehicle.get(0)));
//                    desireSpeed = AccidentConstructorUtil.convertMPHToMS(estimateVehicleSpeed);
//                }
//                else if (estimateVehicleSpeed <= 0)
//                {
////                    ConsoleLogger.print('d',"Vehicle " + vehicleIndexInCoordArr + " Stop 1st act : computedCoord:" + (-1 * estimateVehicleSpeed * i) + " first coord:" + Integer.parseInt(coordOfSelectedVehicle.get(0)));
//                    xCoord = -1.0 * defaultSpeed * i + Double.parseDouble(coordOfSelectedVehicle.get(i - 1).split(":")[0]);
//                    desireSpeed = AccidentConstructorUtil.convertMPHToMS(defaultSpeed);
//                }

                if (curvyRoad)
                {
                    yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
                }
                coordOfSelectedVehicle.add(0,  xCoord + ":" + df.format(yCoord));
            } // End appending precrash constant speed coord

            // Add Accelerated Distance to ensure that the vehicle reaches its speed before traveling with constant speed for 2s
            ArrayList<Double> distanceAndTimeWithAcceleration = AccidentConstructorUtil.computeDistanceAndTimeWithAcceleration(vehicleAttr.getVelocity());

            double xCoord = -1.0 * distanceAndTimeWithAcceleration.get(0) + Double.parseDouble(coordOfSelectedVehicle.get(0).split(":")[0]);

            if (curvyRoad)
            {
                yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
            }

            coordOfSelectedVehicle.add(0,  xCoord + ":" + df.format(yCoord));

            vehicleCoordList.set(vehicleIndexInCoordArr, coordOfSelectedVehicle);

        }
        return vehicleCoordList;
    }
}
