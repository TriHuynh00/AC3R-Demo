package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.OntologyHandler;
import org.saarland.accidentelementmodel.VehicleAttr;

import java.util.ArrayList;
import java.util.Random;

public class RearEndConstructor {

    ArrayList<VehicleAttr> vehicleList = null;
    OntologyHandler ontologyHandler = null;
    TestCaseInfo testCase = null;

    public RearEndConstructor(ArrayList<VehicleAttr> vehicleList, OntologyHandler ontologyHandler,
                              TestCaseInfo testCaseInfo)
    {
        this.vehicleList = vehicleList;
        this.ontologyHandler = ontologyHandler;
        this.testCase = testCaseInfo;
    }

    AccidentConstructorUtil accidentConstructorUtil = new AccidentConstructorUtil();

    public ArrayList<ArrayList<String>> constructAccidentScenario(ArrayList<VehicleAttr> vehicleList, OntologyHandler parser) {
        int actionListLength = vehicleList.get(0).getActionList().size();
        ArrayList<Integer> impactAtSteps = new ArrayList<Integer>();
        ArrayList<ArrayList<VehicleAttr>> impactedVehiclesAtSteps = new ArrayList<ArrayList<VehicleAttr>>();
        ArrayList<Integer> impactLocation = new ArrayList<Integer>();
        // Assume collision point is 0, at x = 0
        ArrayList<ArrayList<String>> constructedCoordVeh = new ArrayList<ArrayList<String>>();

        for (int i = 0; i < vehicleList.size(); i++)
        {
            constructedCoordVeh.add(new ArrayList<String>());
        }

        // Find impact points
        int j = 0;
        impactLocation.add(0);
        for (int i = 0; i < actionListLength; i++) {
            ArrayList<VehicleAttr> impactedVehicleInThisStep = new ArrayList<VehicleAttr>();
            boolean foundImpact = false;
            for (VehicleAttr vehicle : vehicleList) {
                if (vehicle.getActionList().get(i).startsWith("hit")) {
                    foundImpact = true;
                    ConsoleLogger.print('d',"Find impact at step " + i);
                    if (impactAtSteps.indexOf(i) == -1) {
                        impactAtSteps.add(i);
                    }

                    impactedVehicleInThisStep.add(vehicle);
                }
            }
            if (impactedVehicleInThisStep.size() > 0) {
                impactedVehiclesAtSteps.add(impactedVehicleInThisStep);
            }
        }

        // Start from the first impact point, construct the movement of the actor based on the action
        for (int l = 0; l < impactedVehiclesAtSteps.size(); l++)
        {
            ArrayList<VehicleAttr> impactedVehicles = impactedVehiclesAtSteps.get(l);
            for (VehicleAttr veh : impactedVehicles) {
                ConsoleLogger.print('d',"Veh ID in impacted at l=" + l + " VID: " + veh.getVehicleId());
            }
            // Check if 2 cars can hit GIVEN REAR END COLLISION

            boolean stableMovement = false;


            if (impactedVehicles.size() == 2) {
                VehicleAttr vehicle0 = impactedVehicles.get(0);
                VehicleAttr vehicle1 = impactedVehicles.get(1);

                VehicleAttr strikerVehicle = null;
                VehicleAttr victimVehicle = null;

                Random randomGen = new Random();

                int pushAwayDistance = 0;
                // Check if this vehicle is a victim or striker

                VehicleAttr[] strikerAndVictim = AccidentConstructorUtil.findStrikerAndVictim(l, vehicle0, vehicle1, impactAtSteps);
                strikerVehicle = strikerAndVictim[0];
                victimVehicle = strikerAndVictim[1];

                int estimateSpeedOfStriker = -1000;
                int estimateSpeedOfVictim = -1000;

                estimateSpeedOfStriker = Integer.parseInt(parser.findExactConcept(vehicle0.getActionList()
                        .get(impactAtSteps.get(0) - 1))
                        .getDataProperties()
                        .get("velocity"));

                estimateSpeedOfVictim = Integer.parseInt(parser.findExactConcept(vehicle1.getActionList()
                        .get(impactAtSteps.get(0) - 1))
                        .getDataProperties()
                        .get("velocity"));

                ConsoleLogger.print('d',"EV0 : " + estimateSpeedOfStriker + "; EV1 : " + estimateSpeedOfVictim);

                // If 2 vehicle has the same speed, increase the speed of the striker
                if (estimateSpeedOfStriker == estimateSpeedOfVictim)
                {
                    stableMovement = true;
                    // TODO: Randomize the pushAwayForce
                    pushAwayDistance = estimateSpeedOfStriker / 2 + estimateSpeedOfVictim;
                    // TODO: NEED A RANDOM GENERATOR to determine velocity
                    strikerVehicle.setVelocity(strikerVehicle.getVelocity() + 10);
                    // Construct corrdination
                    for (int i = 0; i < impactedVehicles.size(); i++) {
                        int vehicleVelocity = impactedVehicles.get(i).getVelocity();
                        ArrayList<String> vehicleCoord = new ArrayList<String>();
                        if (vehicleVelocity >= 0 && vehicleVelocity != 1000) {
                            // Construct the path from the first step to the current impact point
                            for (int d = 0; d < impactAtSteps.get(l); d++) {
//                                if (vehicleVelocity >= 0)
//                                {
                                    vehicleCoord.add("" + vehicleVelocity * (d + 1));
//                                }
//                                else if (vehicleVelocity != 1000)
//                                {
//                                    vehicleCoord.add("" + 0);
//                                }
                            }

                        }
                        constructedCoordVeh.set(impactedVehicles.get(i).getVehicleId() - 1, vehicleCoord);
                    }

                }
                else // 2 vehicle do not travel at the same speed before impact point
                {
                    ArrayList<String> vehicleCoordStriker; // Striker movement path
                    ArrayList<String> vehicleCoordVictim; // victim movement path

                    // Check if the vehicle coordination for striker exists, if not, create a new one
                    if (constructedCoordVeh.get(strikerVehicle.getVehicleId() - 1).size() > 0)
                    {
                        vehicleCoordStriker = constructedCoordVeh.get(strikerVehicle.getVehicleId() - 1);
                    }
                    else
                    {
                        vehicleCoordStriker = new ArrayList<String>();
                        vehicleCoordStriker.add("0");
                    }

                    // Check if the vehicle coordination for victim exists, if not, create a new one
                    if (constructedCoordVeh.get(victimVehicle.getVehicleId() - 1).size() > 0)
                    {
                        vehicleCoordVictim = constructedCoordVeh.get(victimVehicle.getVehicleId()  - 1);
                    }
                    else
                    {
                        vehicleCoordVictim = new ArrayList<String>();
                        vehicleCoordVictim.add("0");
                    }
                    ConsoleLogger.print('d',"2 cars have diff speed");

                    // Construct coordinate before 1st crash point for 2 vehicles
                    for (int i = impactAtSteps.get(0) - 1; i >= 0; i--) {
                        int estimateSpeedVictim = Integer.parseInt(parser.findExactConcept(victimVehicle.getActionList().get(i))
                                .getDataProperties()
                                .get("velocity"));

                        // TODO : Need to detect mph and set speed for a specific travel action here

                        int estimateSpeedStriker = Integer.parseInt(parser.findExactConcept(strikerVehicle.getActionList().get(i))
                                .getDataProperties()
                                .get("velocity"));

                        // TODO: Randomize the pushAwayForce
                        // pushAwayDistance = Math.abs(estimateSpeedStriker / 2 + estimateSpeedVictim);

                        ConsoleLogger.print('d',"i:" + i + " ESV: " + estimateSpeedVictim + "; ESS: " + estimateSpeedStriker);

                        // Detect DECELERATION
                        if (estimateSpeedVictim != -1 && estimateSpeedVictim <= 0 && i >= 1)
                        {
                            int beforeDecelerSpeed = 0;
                            beforeDecelerSpeed = Integer.parseInt(parser.findExactConcept(victimVehicle.getActionList().get(i - 1))
                                    .getDataProperties()
                                    .get("velocity"));
                            ConsoleLogger.print('d',"BDS: " + beforeDecelerSpeed);
                            if (beforeDecelerSpeed > 0) {
                                vehicleCoordStriker.add(0, "" + (-1 * (estimateSpeedStriker)));
                                vehicleCoordVictim.add(0, "" + (-1 * (beforeDecelerSpeed - 10)));
//                                pushAwayDistance = Math.abs(estimateSpeedStriker / 2 + beforeDecelerSpeed - 10);
                            }
                            else if (beforeDecelerSpeed == 0)
                            {
                                vehicleCoordStriker.add(0, "" + (-1 * (estimateSpeedStriker)));
                                vehicleCoordVictim.add(0, "" + (-1 * (beforeDecelerSpeed - 10)));
//                                pushAwayDistance = Math.abs(estimateSpeedStriker / 2);
                            }

                        }
                        else
                        {
                            // Set distance before impact for striker
//                            if (estimateSpeedStriker == estimateSpeedVictim) {
//                                for (int k = 1; k < simulationDuration + 1; k++) {

                            if (vehicleCoordVictim.size() < impactAtSteps.get(0) + 1)
                            {
                                vehicleCoordVictim.add(0, ""
                                        + (-1 * (estimateSpeedVictim) + Integer.parseInt(vehicleCoordVictim.get(0))) );
                            }

                            // Only add before 1st impact distance if they are not already set
                            if (vehicleCoordStriker.size() < impactAtSteps.get(0) + 1)
                            {
                                vehicleCoordStriker.add(0, ""
                                        + (-1 * (estimateSpeedStriker) + Integer.parseInt(vehicleCoordStriker.get(0))) );
                            }

//                                }
//                            }

                        }
                    } // End construct crash points before 1st impact

                    int actionListSize = victimVehicle.getActionList().size();
                    // TODO: Construct after crash movements
                    for (VehicleAttr processedVehicle : strikerAndVictim) {

                        if (impactAtSteps.get(0) + 1 < actionListSize) {
                            for (int actionListIndex = impactAtSteps.get(0) + 1; actionListIndex < actionListSize; actionListIndex++) {
                                // If this is not a hit, get the velocity of the next word * (len - i)
                                String actionAtI = processedVehicle.getActionList().get(actionListIndex);

                                if (!actionAtI.startsWith("hit") && !actionAtI.equalsIgnoreCase("endHit")) {

                                    int estimateActionVelocity = Integer.parseInt(parser.findExactConcept(actionAtI)
                                            .getDataProperties().get("velocity"));

                                    if (estimateActionVelocity > 0) {
                                        if (processedVehicle.equals(strikerVehicle))
                                        {
                                            ConsoleLogger.print('d',"Set after impact coord for Striker " + estimateActionVelocity);
                                            vehicleCoordStriker.add( (estimateActionVelocity * (actionListSize - actionListIndex)) + "");
                                        }
                                        else if (processedVehicle.equals(victimVehicle))
                                        {
                                            ConsoleLogger.print('d',"Set after impact coord for Victim " + estimateActionVelocity);
                                            vehicleCoordVictim.add( (estimateActionVelocity * (actionListSize - actionListIndex)) + "");
                                        }
                                        else
                                        {
                                            ConsoleLogger.print('d',"CANNOT Set after impact coord for Victim or Striker at i = " + actionListIndex + " vehID " + (processedVehicle.getVehicleId()  - 1));
                                        }
                                    }
                                    else if (estimateActionVelocity == 0)
                                    {
                                        if (processedVehicle.equals(strikerVehicle))
                                        {
                                            ConsoleLogger.print('d',"Set after impact coord for Striker stop " + estimateActionVelocity);
                                            vehicleCoordStriker.add( vehicleCoordStriker.get(vehicleCoordStriker.size() - 1) + "");
                                        }
//                                        else if (processedVehicle.equals(victimVehicle))
//                                        {
//                                            ConsoleLogger.print('d',"Set after impact coord for Victim stop " + estimateActionVelocity);
//                                            vehicleCoordVictim.add( pushAwayDistance + "");
//                                        }
                                    }
                                } // End process non-impact aftermath action
                                else if (actionAtI.equals("hit"))
                                {
                                    // Add the estimate distance after impact
                                    ConsoleLogger.print('d',"PushAwayDistance "  + pushAwayDistance);

                                    int nextStepEstimateSpeed = -1000;
                                    if (actionListIndex + 1 < actionListSize)
                                    {
                                        nextStepEstimateSpeed = Integer.parseInt(parser.findExactConcept(
                                                strikerVehicle.getActionList()
                                                        .get(actionListIndex + 1)).getDataProperties().get("velocity"));
                                    }

//                                    if (pushAwayDistance > 0
//                                            && (nextStepEstimateSpeed == 1000 || nextStepEstimateSpeed == -1000)
//                                            && vehicleCoordStriker.size() < actionListSize)
//                                    {
//                                        vehicleCoordStriker.add((pushAwayDistance * (actionListSize - actionListIndex)) + "");
//                                    }
                                }
                                else if (actionAtI.equals("hit*"))
                                {
                                    ConsoleLogger.print('d',"VehCoordStriker size " + vehicleCoordStriker.size());
                                    ConsoleLogger.print('d',"VehCoordVictim size " + vehicleCoordVictim.size());
                                    if (pushAwayDistance > 0)
                                    {
//                                        if (vehicleCoordVictim.size() == 2) // Fresh coordination
//                                        {

                                            if (vehicleCoordStriker.size() - vehicleCoordVictim.size() > 0)
                                            {
                                                int diffSizeStrikerVictim = Math.abs(vehicleCoordStriker.size() - vehicleCoordVictim.size());
                                                for (int c = 0; c < diffSizeStrikerVictim; c++)
                                                {
                                                    ConsoleLogger.print('d',"Add 0 to victim coord with c = " + c);
                                                    vehicleCoordVictim.add("0");
                                                }
                                            }
                                            else if (vehicleCoordStriker.size() - vehicleCoordVictim.size() < 0)
                                            {
                                                int diffSizeStrikerVictim = Math.abs(vehicleCoordStriker.size() - vehicleCoordVictim.size());
                                                for (int c = 0; c < diffSizeStrikerVictim ; c++)
                                                {
                                                    vehicleCoordStriker.add("0");
                                                }
                                            }

                                            // Justify push away distance
                                            //int modifiedPushAwayDistance = (int)(pushAwayDistance * (actionListSize - actionListIndex) * 0.75);
                                            //vehicleCoordVictim.set(actionListIndex, modifiedPushAwayDistance + "");
                                            int estimateSpeedVictim = Integer.parseInt(parser.findExactConcept(victimVehicle.getActionList().get(actionListIndex - 1))
                                                    .getDataProperties()
                                                    .get("velocity"));

                                            // Detect Deceleration
                                            if (estimateSpeedVictim != -1 && estimateSpeedVictim < 0) {
                                                estimateSpeedVictim = Integer.parseInt(parser.findExactConcept(victimVehicle.getActionList().get(actionListIndex - 2))
                                                        .getDataProperties()
                                                        .get("velocity")) - estimateSpeedVictim;
                                            }

                                            // Construct coord at crash point
//                                            if (Integer.parseInt(vehicleCoordStriker.get(actionListIndex)) > modifiedPushAwayDistance)
//                                            {
//                                                vehicleCoordStriker.set(actionListIndex, modifiedPushAwayDistance + "");
//                                            }

                                            // Construct coord for victim at from current to first crash points
                                            for (int c = actionListIndex - 1; c >= impactAtSteps.get(0); c--)
                                            {
                                                int strikerCoord = Integer.parseInt(vehicleCoordStriker.get(c));
                                                int victimCoord = Integer.parseInt(vehicleCoordVictim.get(c));
                                                ConsoleLogger.print('d',"Before construct coord: strikerCoord = " + strikerCoord
                                                        + " victimCoord = " + victimCoord );
                                                if (strikerCoord >= victimCoord)
                                                {
                                                    victimCoord = strikerCoord + AccidentParam.distanceBetweenCars;
                                                    vehicleCoordVictim.set(c, victimCoord + "");
                                                }
                                                ConsoleLogger.print('d',String.format("Construct coord for crash steps " +
                                                                "at c = %d with victimCoord = %s; strikerCoord = %d\n",
                                                        c, victimCoord, strikerCoord));
                                            }

                                            // Construct ccord for victim before the first crash
                                            for (int c = impactAtSteps.get(0) - 1; c >= 0; c--)
                                            {
                                                int strikerCoord = Integer.parseInt(vehicleCoordStriker.get(c));
                                                int victimCoordInThisStep = Integer.parseInt(vehicleCoordVictim.get(c)) - estimateSpeedVictim * (impactAtSteps.get(0) - c);
                                                if (victimCoordInThisStep <= strikerCoord)
                                                {
                                                    victimCoordInThisStep = strikerCoord + AccidentParam.distanceBetweenCars;
                                                }
                                                ConsoleLogger.print('d',String.format("Construct coord before 1st crash at " +
                                                        "c = %d with victimCoord = %s\n", c, victimCoordInThisStep));
                                                vehicleCoordVictim.set(c, victimCoordInThisStep + "");
                                            }
//                                        }
                                    }
                                }
                                // If this is a rest action, set the vehicle to the previous positiont
                                else if (actionAtI.equals("endHit"))
                                {
                                    if (processedVehicle.equals(strikerVehicle))
                                    {
                                        vehicleCoordStriker.add( vehicleCoordStriker.get(vehicleCoordStriker.size() - 1));
                                    }
                                    else if (processedVehicle.equals(victimVehicle))
                                    {
                                        vehicleCoordVictim.add( vehicleCoordVictim.get(vehicleCoordVictim.size() - 1));
                                    }
                                }
                                ConsoleLogger.print('d',"Vehicle Coord Victim at i " + actionListIndex + " " + vehicleCoordVictim);
                                ConsoleLogger.print('d',"Vehicle Coord Striker at i " + actionListIndex + " " + vehicleCoordStriker);
                            } // End processing an impact step
                        } // End checking if there is valid impact step to process

                    } // End looping through striker and victim

                    constructedCoordVeh.set(victimVehicle.getVehicleId() - 1, vehicleCoordVictim);
                    constructedCoordVeh.set(strikerVehicle.getVehicleId() - 1, vehicleCoordStriker);
                }
                // Construct movement points if the vehicle velocity is more than 0
                ConsoleLogger.print('d',"Stable Movement? " + stableMovement);
//            if (stableMovement) {
//                for (int i = 0; i < impactedVehicles.size(); i++) {
//                    int vehicleVelocity = impactedVehicles.get(i).getVelocity();
//                    ArrayList<String> vehicleCoord = new ArrayList<String>();
//                    if (vehicleVelocity >= 0 && vehicleVelocity != 1000) {
//                        // Construct the path based on the simulation time
//                        for (int d = simulationDuration; d >= 0; d--) {
//                            if (vehicleVelocity > 0) {
//                                vehicleCoord.add("" + vehicleVelocity * d * 1.0);
//                            } else if (vehicleVelocity != 1000) {
//                                vehicleCoord.add("" + 0);
//                            }
//                        }
//
//                    }
//                    constructedCoordVeh.add(vehicleCoord);
//                }
//            }

                // End construct movement points
            }
        }

        // Add more coord prior to the start point
        constructedCoordVeh = appendPrecrashMovementsForTravelingVictim(constructedCoordVeh, vehicleList, parser);

        // TODO: Apply moving direction to Coord

        ConsoleLogger.print('d',"Vehicle Coords in the end");
//        for (ArrayList<String> vehicleCoord : constructedCoordVeh) {
        for (int v = 0; v < constructedCoordVeh.size(); v++) {
            ArrayList<String> vehicleCoord = constructedCoordVeh.get(v);
            ConsoleLogger.print('d',"Vehicle Coord of " + constructedCoordVeh.indexOf(vehicleCoord) + " : ");

            // Both vehicles should move in the same direction, so we can adjust the same lane position for both
            VehicleAttr currentVehicle = vehicleList.get(v);
            String movingDirection = currentVehicle.getTravellingDirection();
            double laneAdjustment = (AccidentParam.laneWidth / 2.0) *
                    (Double.parseDouble(currentVehicle.getStandingStreet().getStreetPropertyValue("lane_num")) / 2);
            for (int i = 0; i < vehicleCoord.size(); i++) {
                switch (movingDirection) {
                    case "E":
                        vehicleCoord.set(i, vehicleCoord.get(i) + ":" + (-laneAdjustment));
                        break;
                    default:
                        break;
                }
//                    // NORTH: 0 : coord
//                    if (movingDirection.equalsIgnoreCase("north"))
//                    {
//                        vehicleCoord.set(i, "0:-" + vehicleCoord.get(i));
//                    }
//                    // EAST: -coord : 0
//                    else if (movingDirection.equalsIgnoreCase("east"))
//                    {
//                        vehicleCoord.set(i, "-" + vehicleCoord.get(i) + ":0");
//                    }
//                    // SOUTH: 0 : coord
//                    else if (movingDirection.equalsIgnoreCase("south"))
//                    {
//                        vehicleCoord.set(i, "0:" + vehicleCoord.get(i));
//                    }
//                    // WEST: coord : 0
//                    else if (movingDirection.equalsIgnoreCase("west"))
//                    {
//                        vehicleCoord.set(i, vehicleCoord.get(i) + ":0");
//                    }
                // If grading does not matter, append 0 in the end
                if (!AccidentParam.isGradingConcerned)
                    vehicleCoord.set(i, vehicleCoord.get(i) + ":0");
                ConsoleLogger.print('n',vehicleCoord.get(i) + " ");
            }
            for (VehicleAttr suspectVehicle : vehicleList)
            {
                if (suspectVehicle.getVehicleId() - 1 == constructedCoordVeh.indexOf(vehicleCoord))
                {
                    suspectVehicle.setMovementPath(vehicleCoord);
                    break;
                }
            }

            ConsoleLogger.print('d', "");
        }

        for (VehicleAttr vehicleAttr1 : vehicleList) {
            ConsoleLogger.print('d',"Final Vehicle Coord of " + vehicleAttr1.getVehicleId() + " : ");

            for (String coord : vehicleAttr1.getMovementPath()) {
                ConsoleLogger.print('n',coord + " ");
            }
            ConsoleLogger.print('d', "");
        }

        return constructedCoordVeh;
    }

    public ArrayList<ArrayList<String>> appendPrecrashMovementsForTravelingVictim(ArrayList<ArrayList<String>> vehicleCoordList,
                                                                 ArrayList<VehicleAttr> vehicleList,
                                                                 OntologyHandler parser)
    {
        int defaultSpeed = 20;
        // Append the coord based on the first action of the vehicle
        for (VehicleAttr vehicleAttr : vehicleList)
        {
            int vehicleIndexInCoordArr = vehicleAttr.getVehicleId() - 1;
            ArrayList<String> coordOfSelectedVehicle = vehicleCoordList.get(vehicleIndexInCoordArr);

            for (int i = 1; i <= AccidentParam.simulationDuration; i++)
            {
                int estimateVehicleSpeed = Integer.parseInt(parser.findExactConcept(vehicleAttr.getActionList().get(0))
                        .getDataProperties()
                        .get("velocity"));

                if (estimateVehicleSpeed > 0 && estimateVehicleSpeed != 1000)
                {
//                    ConsoleLogger.print('d',"Vehicle " + vehicleIndexInCoordArr + " Travel 1st act : computedCoord:" + (-1 * estimateVehicleSpeed * i) + " first coord:" + Integer.parseInt(coordOfSelectedVehicle.get(0)));
                    coordOfSelectedVehicle.add(0, "" +
                            ( (-1 * estimateVehicleSpeed * i) + Integer.parseInt(coordOfSelectedVehicle.get(i - 1)) ) );
                }
                else if (estimateVehicleSpeed <= 0)
                {
//                    ConsoleLogger.print('d',"Vehicle " + vehicleIndexInCoordArr + " Stop 1st act : computedCoord:" + (-1 * estimateVehicleSpeed * i) + " first coord:" + Integer.parseInt(coordOfSelectedVehicle.get(0)));
                    coordOfSelectedVehicle.add(0, "" +
                            ( (-1 * defaultSpeed * i) + Integer.parseInt(coordOfSelectedVehicle.get(i - 1)) ) );
                }
            } // End appending precrash coord

            vehicleCoordList.set(vehicleIndexInCoordArr, coordOfSelectedVehicle);

        }
        return vehicleCoordList;
    }

//    private VehicleAttr[] findStrikerAndVictim(int actionWordIndex, VehicleAttr vehicle0, VehicleAttr vehicle1,
//                                               ArrayList<Integer> impactAtSteps)
//    {
//        VehicleAttr[] strikerAndVictim = new VehicleAttr[2];
//        if (vehicle0.getActionList().get(impactAtSteps.get(actionWordIndex)).endsWith("hit*")) {
//
//            strikerAndVictim[0] = vehicle1;
//            strikerAndVictim[1] = vehicle0;
//        }
//        else
//        {
//            strikerAndVictim[0] = vehicle0;
//            strikerAndVictim[1] = vehicle1;
//        }
//        return strikerAndVictim;
//    }

}
