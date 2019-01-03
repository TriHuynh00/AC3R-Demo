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

public class FrontCollisionConstructor {

    DecimalFormat df;

    public FrontCollisionConstructor()
    {
        df = new DecimalFormat("####.##");
    }

    public ArrayList<ArrayList<String>> constructAccidentScenario(ArrayList<VehicleAttr> vehicleList,
                                                                  OntologyHandler parser,
                                                                  TestCaseInfo testCase) {
        ArrayList<ArrayList<String>> constructedCoordVeh = new ArrayList<ArrayList<String>>();
        ArrayList<Integer> impactAtSteps = new ArrayList<Integer>();
        ArrayList<ArrayList<VehicleAttr>> impactedVehiclesAtSteps = new ArrayList<ArrayList<VehicleAttr>>();

        boolean curvyRoad = false;
        double radius = 0;

        VehicleAttr[] strikerAndVictim = new VehicleAttr[2];

        for (VehicleAttr vehicle : vehicleList)
        {
            constructedCoordVeh.add(vehicle.getVehicleId() - 1, new ArrayList<String>());
        }

        constructedCoordVeh = AccidentConstructorUtil.fillCoordOfVehicles(constructedCoordVeh, vehicleList.get(0).getActionList().size());

        // If there are 2 vehicles, find the victim and striker to construct coordinate.
        if (vehicleList.size() == 2) {
            // Remove all the victim coord and append only 1 value
            strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictim(vehicleList.get(0), vehicleList.get(1));
            constructedCoordVeh.get(strikerAndVictim[1].getVehicleId() - 1).clear();
            constructedCoordVeh.get(strikerAndVictim[1].getVehicleId() - 1).add("0");
        } else if (vehicleList.size() == 1) {
            strikerAndVictim = new VehicleAttr[]{vehicleList.get(0), null};
        }

        VehicleAttr strikerVehicle = strikerAndVictim[0];
        LinkedList<String> strikerVehicleActionList = strikerVehicle.getActionList();

        // Find the impact point
        AccidentConstructorUtil.findImpactedStepsAndVehicles(impactAtSteps, impactedVehiclesAtSteps, vehicleList);

        // Set the crash coord
        ArrayList<String> vehicleCoordStriker = constructedCoordVeh.get(strikerVehicle.getVehicleId() - 1);
        vehicleCoordStriker.set(impactAtSteps.get(0), "0:0");

        // Construct the coords before crash
        ConsoleLogger.print('d',"impactAtSteps size: " + impactAtSteps.size());
        if (impactAtSteps.size() == 1) {

            Street currentStreet = null;

            // If there is only 1 road, take that road as standard road
            if (testCase.getStreetList().size() == 1)
            {
                currentStreet = testCase.getStreetList().get(0);
            }

            if (!currentStreet.getStreetPropertyValue("road_shape").equals(RoadShape.STRAIGHT))
            {
                curvyRoad = true;
                radius = Double.parseDouble(currentStreet.getStreetPropertyValue("curve_radius").replace("m", ""));
            }

            ConsoleLogger.print('d',"Curvy Road? " + curvyRoad);
            ConsoleLogger.print('d',"Road Shape " + currentStreet.getStreetPropertyValue("road_shape"));

            // Construct coord before crash
            for (int i = 0; i < impactAtSteps.get(0); i++) {
                String actionAtI = strikerVehicleActionList.get(i);
                if (!actionAtI.startsWith("hit") && !actionAtI.equalsIgnoreCase("endHit")) {

                    int estimateActionVelocity = Integer.parseInt(parser.findExactConcept(actionAtI)
                            .getDataProperties().get("velocity"));

                    if (estimateActionVelocity > 0) {
                        ConsoleLogger.print('d',"FrontColl Set after impact coord for Striker " + estimateActionVelocity);
                        // Calculate Xcoord value
                        double xCoord = estimateActionVelocity * (impactAtSteps.get(0) - i) * -1.0;

                        double yCoord = 0;
                        if (curvyRoad) {
//                            yCoord = 1000 + -1.0 * Math.sqrt(1000000 - Math.pow(xCoord, 2));
                            yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
                            ConsoleLogger.print('d',"ycoord is " + yCoord);
                        }

                        vehicleCoordStriker.set(i, xCoord + ":" + df.format(yCoord));
                    }
                    else if (estimateActionVelocity == 0)
                    {
                        ConsoleLogger.print('d',"Set after impact coord for Striker STOP " + estimateActionVelocity);

                        // Set the stop coord the same as previous coord
                        if (i > 0)
                        {
                            vehicleCoordStriker.set(i, vehicleCoordStriker.get(i - 1) + "");
                        }
                        else
                        {
                            double xCoord = -100;
                            double yCoord = 0;
                            if (curvyRoad) {
                                yCoord = AccidentConstructorUtil.computeYCircleFunc(radius, xCoord);
                                ConsoleLogger.print('d',"ycoord is " + yCoord);
                            }
                            vehicleCoordStriker.set(i, xCoord + ":" + df.format(yCoord));
                        }
                    }
                } // End assign coord to each action before crash
            } // End looping through all action prior to crash



            constructedCoordVeh.set(strikerVehicle.getVehicleId() - 1, vehicleCoordStriker);
        } // End checking if there is only 1 impact location

        appendPrecrashMovementsForVehicle(constructedCoordVeh, vehicleList, parser, curvyRoad, radius);

        // remove all coord that contain only 0 to see how things happen naturally
        for (ArrayList<String> coordList : constructedCoordVeh)
        {
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
                if (coordList.get(0).trim().equals("0"))
                {
                    coordList.set(0, "0:0");
                }
            }
        }

        // Append 0 grade if grading is not concerned
        for (int i = 0; i < vehicleList.size(); i++)
        {

            ConsoleLogger.print('d',"Vehicle #" + (i + 1) + " coord list:");
            ArrayList<String> currentConstructedCoord = constructedCoordVeh.get(i);
            for (int k = 0; k < currentConstructedCoord.size(); k++)
            {
                if (!AccidentParam.isGradingConcerned)
                    currentConstructedCoord.set(k, currentConstructedCoord.get(k) + ":0");
                else
                    currentConstructedCoord.set(k, currentConstructedCoord.get(k));
                ConsoleLogger.print('n',currentConstructedCoord.get(k) + " ");

            }
            constructedCoordVeh.set(i, currentConstructedCoord);
            ConsoleLogger.print('d', "");
            vehicleList.get(i).setMovementPath(constructedCoordVeh.get(i));
        }

        // Final crash point adjustment depending on the scenario's story
        ArrayList<String> strikerVehiclePath = strikerVehicle.getMovementPath();

        for (VehicleAttr vehicleAttr : vehicleList) {
            ArrayList<String> vehicleMovementPath = vehicleAttr.getMovementPath();

            // If this is not the striker car, convert the coords in this waypointFilePath to BeamNG format
            if (!vehicleMovementPath.get(0).equals(strikerVehiclePath.get(0))) {
                // ConsoleLogger.print('d',"Not the processed coord list, vehicle is " + vehicleAttr.getVehicleId() +
                //  " on street? " + vehicleAttr.getOnStreet() + " vehMovPath size " + vehicleMovementPath.size());

                // If this is a static car, then check whether it is on the pavement or parking line, and set coord accordingly
                if (vehicleMovementPath.size() == 1
                        && (vehicleAttr.getOnStreet() == 0 || vehicleAttr.getOnStreet() == -1)) {

                    String convertedCoord = vehicleMovementPath.get(0);
                    Street currentStreet = vehicleAttr.getStandingStreet();
                    int laneNumber = Integer.parseInt(currentStreet.getStreetPropertyValue("lane_num"));

                    // Append zCoord (grading) to a non-zCoord coordinate
                    if (convertedCoord.split(AccidentParam.defaultCoordDelimiter).length == 2) {
//                        if (!AccidentParam.isGradingConcerned) {
//                            convertedCoord += AccidentParam.defaultCoordDelimiter + "0";
//                        }
                    }

                    String newYCoord = "";

                    // For straight road, need to place the waypoint far away from the road a bit compared to curvy
                    // road
                    ConsoleLogger.print('d', "Standing Road side " + vehicleAttr.getStandingRoadSide());
                    if (radius == 0.0) {

                        double parkingLineExtraDistance = 0;

                        if (!currentStreet.getStreetPropertyValue("road_park_line").equals("0")
                                && !currentStreet.getStreetPropertyValue("road_park_line").equals("")) {
                            ConsoleLogger.print('d', "Parking Line extra distance set to 2");
                            parkingLineExtraDistance = AccidentParam.parkingLineWidth / 2;
                        }

                        if (vehicleAttr.getStandingRoadSide().equals("left")) {
                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(AccidentParam.defaultCoordDelimiter)[1]) +
                                    (int) (laneNumber / 2.0 * AccidentParam.laneWidth + parkingLineExtraDistance) + 0.5);
                            //(int) (laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth / 2 + parkingLineExtraDistance) - 0.5 );
                        } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(AccidentParam.defaultCoordDelimiter)[1]) -
                                    (int) (laneNumber / 2.0 * AccidentParam.laneWidth - parkingLineExtraDistance) - 0.5);

                        }

                    } else {
                        if (vehicleAttr.getStandingRoadSide().equals("left")) {
                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(AccidentParam.defaultCoordDelimiter)[1]) +
                                    (laneNumber / 2.0 * AccidentParam.laneWidth) + 2);
                            //(laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth + 1) + 1);
                        } else if (vehicleAttr.getStandingRoadSide().equals("right")) {

                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(AccidentParam.defaultCoordDelimiter)[1]) -
                                    (laneNumber / 2.0 * AccidentParam.laneWidth) - 2);
                            ConsoleLogger.print('d', "Right Curve Park " + Double.parseDouble(convertedCoord.split(AccidentParam.defaultCoordDelimiter)[1])
                                    + " " + (laneNumber / 2.0 * AccidentParam.laneWidth) + " ");
                        }
//                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(AccidentParam.defaultCoordDelimiter)[1]) -
//                                    (laneNumber / 2 * AccidentParam.laneWidth - 1))  ;
                    }

                    ConsoleLogger.print('d', "convert coord " + convertedCoord);
                    ConsoleLogger.print('d', "newYCoord " + newYCoord);
                    String newCoord = AccidentConstructorUtil.updateCoordElementAtDimension(
                            1, convertedCoord, newYCoord, AccidentParam.defaultCoordDelimiter);
                    ConsoleLogger.print('d', "newCoord  " + newCoord);

                    // Set the updated impact point to the other car
                    for (VehicleAttr otherVehicle : vehicleList) {
                        ConsoleLogger.print('d', "other veh ID " + otherVehicle.getVehicleId());
                        if (otherVehicle.getVehicleId() != vehicleAttr.getVehicleId()) {
                            ArrayList<String> otherVehicleMovementPath = otherVehicle.getMovementPath();
                            for (int j = 0; j < otherVehicleMovementPath.size(); j++) {
                                String[] pathNodeElements = otherVehicleMovementPath.get(j).split(AccidentParam.defaultCoordDelimiter);
                                String[] convertedCoordElements = convertedCoord.split(AccidentParam.defaultCoordDelimiter);

                                // Check whether this is the crash points between the 2 cars
                                if (pathNodeElements[0].equals(convertedCoordElements[0])
                                        && pathNodeElements[1].equals(convertedCoordElements[1])) {
                                    // Update the right grade value (zCoord)

                                    if (!AccidentParam.isGradingConcerned)
                                        newCoord = AccidentConstructorUtil.updateCoordElementAtDimension(
                                            2, newCoord, pathNodeElements[2], AccidentParam.defaultCoordDelimiter);

                                    // Adjust the car position far away a bit to ensure crash
                                    String adjustedCoord = AccidentConstructorUtil.updateCoordElementAtDimension(0, newCoord,
                                            (Double.parseDouble(convertedCoordElements[0]) + 3) + "",
                                            AccidentParam.defaultCoordDelimiter);

//                                    otherVehicleMovementPath.set(j, adjustedCoord);
//                                    otherVehicle.setMovementPath(otherVehicleMovementPath);
//
//                                    vehicleMovementPath.set(0, newCoord);
//                                    vehicleAttr.setMovementPath(vehicleMovementPath);

                                    // For straight road, put the wp away the street a bit
//                                    if (radius == 0.0) {
//                                        if (vehicleAttr.getStandingRoadSide().equals("left")) {
//                                            newYCoord = AccidentParam.df6Digit.format((Double.parseDouble(newYCoord)) - 1);
//                                        } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
//                                            newYCoord = AccidentParam.df6Digit.format((Double.parseDouble(newYCoord)) + 1);
//                                        }
//                                        newCoord = AccidentConstructorUtil.updateCoordElementAtDimension(1,
//                                                newCoord, newYCoord, AccidentParam.defaultCoordDelimiter);
//
//                                        ConsoleLogger.print('d', "new coord straight road update " + newCoord);
//                                    }

                                    adjustedCoord = AccidentConstructorUtil.updateCoordElementAtDimension(1, adjustedCoord, newYCoord,
                                            AccidentParam.defaultCoordDelimiter);


                                    ConsoleLogger.print('d', "Found same impact coord at " + j + " value " + adjustedCoord);
                                    otherVehicleMovementPath.set(j, adjustedCoord);
                                    otherVehicle.setMovementPath(otherVehicleMovementPath);

                                    vehicleMovementPath.set(0, newCoord);
                                    vehicleAttr.setMovementPath(vehicleMovementPath);

                                    ConsoleLogger.print('d', "otherVehicle before beamNG path " + otherVehicleMovementPath.toString());
                                    ConsoleLogger.print('d', "currVehicle before beamNG path " + vehicleMovementPath.toString());

                                }
                            }
                        }
                    } // End looping through other vehicles and set impact points
                } // End checking vehicle is on parking line or pavement
//                else {
//                    for (int i = 0; i < vehicleMovementPath.size(); i++) {
//                        String newCoord = AccconvertCoordToBeamNGFormat(vehicleMovementPath.get(i), radius);
//                        vehicleMovementPath.set(i, newCoord);
//                    }
//                }
            }
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
                else if (estimateVehicleSpeed <= 0)
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


}

