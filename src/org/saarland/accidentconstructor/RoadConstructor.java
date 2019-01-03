package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.NavigationDictionary;
import org.saarland.accidentelementmodel.Street;
import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.accidentelementmodel.VehicleAttr;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.AccidentConcept;
import org.saarland.ontologyparser.OntologyHandler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RoadConstructor {

    private ArrayList<VehicleAttr> vehicleList;
    private TestCaseInfo testCaseInfo;
    private int vehicleIdOfBasePath;
    private int distanceBetweenBaseRoadNodes = 60;
    private int processedAiConfigVehicleCount = 0;
    private OntologyHandler ontologyHandler;
    // Construct the config for each vehicle
    StringBuilder allAIConfigStr = new StringBuilder();
    String allLuaAIConfigContent;
//    private Street processingRoad; // the Street being processed currently

    public RoadConstructor(ArrayList<VehicleAttr> vehicleList, TestCaseInfo testCase, OntologyHandler ontologyHandler) throws IOException
    {
        testCaseInfo = testCase;
        this.vehicleList = vehicleList;
        Path luaPathFollowerFilePath = Paths.get(AccidentParam.luaAIFilePath);
        this.ontologyHandler = ontologyHandler;
        allLuaAIConfigContent = loadTemplateFileContent(luaPathFollowerFilePath) + "\n";
    }

    private int chooseTheBaseVehicle()
    {
        // Find the vehicle with the most highest Xcoord value
        double maxXCoord = 0;
        int maxXCoordVehicleID = -1;
        for (VehicleAttr vehicle : vehicleList)
        {
            double firstXCoord = Double.parseDouble(vehicle.getMovementPath().get(0).split(":")[0]);

            if (Math.abs(firstXCoord) > maxXCoord)
            {
                maxXCoord = Math.abs(firstXCoord);
                maxXCoordVehicleID = vehicle.getVehicleId() - 1;
            }
        }

        vehicleIdOfBasePath = maxXCoordVehicleID + 1;

        return maxXCoordVehicleID;
    }

//    public void constructRoadNodes(String roadGradeDirection, String roadGradeDeg, int laneNumber, String radiusStr, String scenarioName) {
    public String constructRoadNodes(String scenarioName) {

        ArrayList<Street> streetList = testCaseInfo.getStreetList();
        StringBuilder environmentFileStrBuilder = new StringBuilder();
        //        StringBuilder environmentFileTemplate = new StringBuilder();

        Path headerFilePath = Paths.get(AccidentParam.headerFilePath);

        // Construct the header of BeamNG prefab file
        try
        {
            environmentFileStrBuilder.append(AccidentConstructorUtil.loadTemplateContent(AccidentParam.headerFilePath));
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('e',"Error at constructing header of BeamNG file " + ex);
        }

        for (int s = 0; s < streetList.size(); s++) {
            Street road = streetList.get(s);

            String roadGradeDirection = road.getStreetProp().get("road_grade");
            String roadGradeDeg = road.getStreetProp().get("road_grade_deg");
            int laneNumber = Integer.parseInt(road.getStreetProp().get("lane_num"));
            String radiusStr = road.getStreetProp().get("curve_radius");

            boolean isPavementNeeded = false;
            // Detect if pavement is needed
            for (VehicleAttr vehicle : vehicleList)
            {
                ConsoleLogger.print('d',"Vehicle " + vehicle.getVehicleId() + " onStreet " + vehicle.getOnStreet());
                if (vehicle.getOnStreet() == 0)
                {
                    isPavementNeeded = true;
                    break;
                }
            }
            double radius = Double.parseDouble(radiusStr.replace("m", ""));

            double gradeDegree = 2.0;
            double gradeIncrement = 0;

            // Check if the grade contains the degree or not
            if (roadGradeDeg.endsWith("%"))
            {
                try
                {
                    gradeDegree = Double.parseDouble(roadGradeDeg.replace("%", ""));
                }
                catch (Exception e)
                {
                    ConsoleLogger.print('d',"Exception at convert grade " + e);
                }
            }

            // Road Construction

            NavigationDictionary navDict = new NavigationDictionary();
            HashMap<String, String> navigationDict;

            String roadCardinalDirection = road.getStreetPropertyValue("road_navigation");

            boolean isSingleRoadPiece = road.getStreetPropertyValue("is_single_road_piece").equalsIgnoreCase("T");

            if (streetList.size() == 1)
            {
                navigationDict = navDict.EastNavDict;
            }
            else {
                switch (roadCardinalDirection) {
                    case "N":
                        navigationDict = navDict.NorthNavDict;
                        break;
                    case "S":
                        navigationDict = navDict.SouthNavDict;
                        break;
                    case "E":
                        navigationDict = navDict.EastNavDict;
                        break;
                    case "W":
                        navigationDict = navDict.WestNavDict;
                        break;
                    case "NE":
                        navigationDict = navDict.NorthEastNavDict;
                        break;
                    case "NW":
                        navigationDict = navDict.NorthWestNavDict;
                        break;
                    case "SE":
                        navigationDict = navDict.SouthEastNavDict;
                        break;
                    case "SW":
                        navigationDict = navDict.SouthWestNavDict;
                        break;
                    default:
                        navigationDict = navDict.EastNavDict;
                        break;
                }
            }

            // Construct coords of the road
            ArrayList<String> roadCoordList = new ArrayList<String>();

            if (!AccidentParam.isGradingConcerned)
                roadCoordList.add("0 0 0");

            // Add nodes to construct the road, if the road is a single road piece i.e. the road ends at an intersection,
            // then construct a long road. Otherwise construct until the intersection
            for (int i = 1; i <= 4; i++)
            {
                double segmentLength = i * distanceBetweenBaseRoadNodes;

                String finalCoord = navDict.createCoordBasedOnNavigation(segmentLength, radius,
                        "forward", navigationDict);

                // TODO: Compute grading coord (zCoord) when grading is concerned.

                ConsoleLogger.print('d', "finalCoord is " + finalCoord);

                roadCoordList.add(0, finalCoord);

                // If this road extends beyond the intersection, add more nodes to it
                if (!isSingleRoadPiece)
                {
                    String extendedCoord = navDict.createCoordBasedOnNavigation(segmentLength, radius,
                            "backward", navigationDict);

                    roadCoordList.add(roadCoordList.size(), extendedCoord);
                }

            } // End adding road nodes

            ConsoleLogger.print('d', "New Road Coord list is " + roadCoordList.toString());

            road.putValToKey("road_node_list", roadCoordList.toString().replace(",", ";")
                    .replace("[", "").replace("]", ""));

            ConsoleLogger.print('d',"Road Coord List is " + roadCoordList);

            StringBuilder nodeListStr = new StringBuilder();

            for (String roadNode : roadCoordList) {
                nodeListStr.append("\t\tNode = \"" + roadNode + "\";\n");
            }

            nodeListStr.append("\n");

            try
            {
                // Update to environment file
                environmentFileStrBuilder.append(constructRoadObjs(roadCoordList.get(0), nodeListStr.toString(),
                        roadCoordList, laneNumber, isPavementNeeded, road));

                environmentFileStrBuilder.append("\n ");
            }
            catch (Exception ex)
            {
                environmentFileStrBuilder.append("Error in constructing Environment " + ex);
            }


        }

            // If this is a straight path, construct the road coords differently

//            if (testCaseInfo.getCrashType().contains("straight paths") ||
//                    testCaseInfo.getCrashType().contains("turn into") ||
//                    (testCaseInfo.getCrashType().contains("rear-end")
//                            || testCaseInfo.getCrashType().toLowerCase().contains("rearend")
//                            || testCaseInfo.getCrashType().toLowerCase().contains("rear end")))
//            {
//
//                ArrayList<String> roadCoordList = new ArrayList<String>();
//                roadCoordList.add("0 0");
//
//                String isSingleRoadPiece = road.getStreetPropertyValue("is_single_road_piece");
//
//                // If this is a north or southbound road, construct road nodes from point 0,0 along the Y axis
//                if (road.getStreetPropertyValue("road_navigation").equals("N")
//                    || road.getStreetPropertyValue("road_navigation").equals("S"))
//                {
//                    ConsoleLogger.print('d',"road direction " + road.getStreetPropertyValue("road_navigation") +  " is single road ? " + road.getStreetPropertyValue("is_single_road_piece"));
//
//                    // append the nodes continuously if this is not a cut road
//                    if (isSingleRoadPiece.equals("F") || isSingleRoadPiece.equals(""))
//                    {
//                        ConsoleLogger.print('d',"Add 8 continuous node for road with direction " + road.getStreetPropertyValue("road_navigation"));
//                        // Make 8 nodes along the Y axis, 4 above, 4 below point 0 0
//                        for (int i = 1; i <= 4; i++)
//                        {
//                            double currXCoord = 0;
//                            double currYCoord = i * distanceBetweenBaseRoadNodes;
//
//                            if (radius != 0)
//                            {
//                                currXCoord = AccidentConstructorUtil.computeXCircleFunc(radius, currYCoord);
//                            }
//
//                            roadCoordList.add(0, currXCoord + " " + currYCoord);
//
//                            if (radius != 0)
//                            {
//                                currXCoord = AccidentConstructorUtil.computeYCircleFunc(radius, -currYCoord);
//                            }
//                            roadCoordList.add(roadCoordList.size(), currXCoord + " " + -currYCoord);
//
//                        } // End adding 8 nodes
//                    } // End adding nodes for continuous road
//                    else // Construct roads for single road
//                    {
//                        // append the nodes on 1 side if this is a cut road
//                        for (int i = 1; i <= 4; i++)
//                        {
//                            double currXCoord = 0;
//                            double currYCoord = i * distanceBetweenBaseRoadNodes;
//
//                            currXCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius, currXCoord, currYCoord);
//
//                            // N -> append road along the negative y axis
//                            if (road.getStreetPropertyValue("road_navigation").equals("N"))
//                            {
//                                roadCoordList.add(roadCoordList.size(), currXCoord + " " + -currYCoord);
//                            }
//                            else if (road.getStreetPropertyValue("road_navigation").equals("S"))
//                            {
//                                roadCoordList.add(0, currXCoord + " " + currYCoord);
//                            }
//                        }
//
//                    } // End constructing nodes for single N or S road
//
//                } // End processing north or southbound road
//
//                else if (road.getStreetPropertyValue("road_navigation").equals("W")
//                        || road.getStreetPropertyValue("road_navigation").equals("E"))
//                {
//
//                    if (isSingleRoadPiece.equals("F") || isSingleRoadPiece.equals(""))
//                    {
//                        // Make 8 nodes along the Y axis, 4 above, 4 below point 0 0
//                        for (int i = 1; i <= 4; i++)
//                        {
//                            double currXCoord = i * distanceBetweenBaseRoadNodes;
//                            double currYCoord = 0;
//
//                            if (radius != 0)
//                            {
//                                currYCoord = AccidentConstructorUtil.computeYCircleFunc(radius, currXCoord);
//                            }
//
//                            roadCoordList.add(0, currXCoord + " " + currYCoord);
//
//                            if (radius != 0)
//                            {
//                                currYCoord = AccidentConstructorUtil.computeYCircleFunc(radius, -currXCoord);
//                            }
//                            roadCoordList.add(roadCoordList.size(), -currXCoord + " " + currYCoord);
//
//                        } // End adding 8 nodes
//                    }
//                    else
//                    {
//                        // append the nodes on 1 side if this is a cut road
//                        for (int i = 1; i <= 4; i++)
//                        {
//                            double currXCoord = i * distanceBetweenBaseRoadNodes;
//                            double currYCoord = 0;
//
//                            currYCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius, currYCoord, currXCoord);
//
//                            // N -> append road along the negative y axis
//                            if (road.getStreetPropertyValue("road_navigation").equals("E"))
//                            {
//                                roadCoordList.add(roadCoordList.size(), -currXCoord + " " + currYCoord);
//                            }
//                            else if (road.getStreetPropertyValue("road_navigation").equals("W"))
//                            {
//                                roadCoordList.add(0, currXCoord + " " + currYCoord);
//                            }
//                        }
//                    }
//                } // End processing west or eastbound road
//                else // Construct road nodes for SE SW NE NW
//                {
//                    ConsoleLogger.print('d',"Construct road for direction " + road.getStreetPropertyValue("road_navigation"));
//                    // Construct a straight road span heading to the north (positive x plan) from point 0 0
//                    for (int i = 1; i <= 4; i++)
//                    {
//                        double currXCoord = 0;
//                        double currYCoord = i * distanceBetweenBaseRoadNodes;
//
//                        currXCoord = AccidentConstructorUtil.computeCurveCoordIfRadiusGiven(radius, currXCoord, currYCoord);
//
//                        int roadAngle = Integer.parseInt(road.getStreetPropertyValue("road_angle"));
//
//                        double rotatedCoord[] = AccidentConstructorUtil.computeNewCoordOfRotatedLine(currYCoord, roadAngle);
//
//                        roadCoordList.add(0, AccidentParam.df6Digit.format(rotatedCoord[0]) + " "
//                                + AccidentParam.df6Digit.format(rotatedCoord[1]));
//
//                        // Append the nodes on the opposite direction
//                        if (isSingleRoadPiece.equals("F"))
//                        {
//                            roadCoordList.add(roadCoordList.size(), AccidentParam.df6Digit.format(-rotatedCoord[0]) + " "
//                                    + AccidentParam.df6Digit.format(-rotatedCoord[1]));
//                        }
//
//                    }
//                }
//
//                // Append the coord list to the road_node_list attr
//
//                road.putValToKey("road_node_list", roadCoordList.toString().replace(",", ";")
//                        .replace("[", "").replace("]", ""));
//                ConsoleLogger.print('d',"Road " + road.getStreetPropertyValue("road_ID") + " has node list " +
//                        road.getStreetPropertyValue("road_node_list"));
//
//                // Append uphill grade value
//                String gradeDegreeAtO = "0";
//                if (gradeDegree > 0)
//                {
//                    // increase road grade from 0 to last node
//                    for (int i = 0; i < roadCoordList.size(); i++)
//                    {
//                        gradeIncrement = AccidentConstructorUtil.computeGradeIncrement(0, 40, gradeDegree);//gradeDegree / 100 / distanceBetweenBaseRoadNodes;
//                        String currentCoord = roadCoordList.get(i);
//                        String currentHeight = AccidentParam.df6Digit.format(gradeIncrement * i);
//
//                        // Find the level road and set all of its coord to the current height
//                        if (currentCoord.equals("0 0"))
//                        {
//                            gradeDegreeAtO = currentHeight;
//
//                            // Append the gradeDegreeAtO to the lvl road
//                            String levelRoadNodeListWithGrade = appendGradeAtOToLevelRoad(streetList, gradeDegreeAtO);
//                            if (levelRoadNodeListWithGrade.equals(""))
//                            {
//                                ConsoleLogger.print('d',"There is no level road");
//                            }
//                        }
//                        roadCoordList.set(i, currentCoord + " " + currentHeight);
//
//
//                    }
//                }
//                else if (gradeDegree < 0)
//                {
//                    int lastIndex = roadCoordList.size() - 1;
//                    for (int i = 0; i < roadCoordList.size(); i++)
//                    {
//                        String currentCoord = roadCoordList.get(i);
//                        String currentHeight = AccidentParam.df6Digit.format(gradeIncrement * i);
//
//                        // Find the level road and set all of its coord to the current height
//                        if (currentCoord.equals("0 0"))
//                        {
//                            gradeDegreeAtO = currentHeight;
//
//                            // Append the gradeDegreeAtO to the lvl road
//                            String levelRoadNodeListWithGrade = appendGradeAtOToLevelRoad(streetList, gradeDegreeAtO);
//                            if (levelRoadNodeListWithGrade.equals("")) {
//                                ConsoleLogger.print('d',"There is no level road");
//                            }
//                        }
//                    }
//                    // increase road grade from 0 to last node
//                    for (int i = roadCoordList.size() - 1; i > 0; i--)
//                    {
//                        gradeIncrement = AccidentConstructorUtil.computeGradeIncrement(0, 40, gradeDegree); //gradeDegree / 100 / distanceBetweenBaseRoadNodes;
//                        roadCoordList.set(i, roadCoordList.get(i) + " " + AccidentParam.df6Digit.format(gradeIncrement * (lastIndex - i)));
//                    }
//                }
//
//                road.putValToKey("road_node_list", roadCoordList.toString().replace(",", ";")
//                        .replace("[", "").replace("]", ""));
//
//                ConsoleLogger.print('d',"Road Coord List is " + roadCoordList);
//
//                StringBuilder nodeListStr = new StringBuilder();
//
//                for (String roadNode : roadCoordList) {
//                    nodeListStr.append("\t\tNode = \"" + roadNode + "\";\n");
//                }
//
//                nodeListStr.append("\n");
//
//                try
//                {
//                    // Update to environment file
//                    environmentFileStrBuilder.append(constructRoadObjs(roadCoordList.get(0), nodeListStr.toString(),
//                            roadCoordList, laneNumber, isPavementNeeded, road));
//
//                    environmentFileStrBuilder.append("\n ");
//                }
//                catch (Exception ex)
//                {
//                    environmentFileStrBuilder.append("Error in constructing Environment " + ex);
//                }
//            } // End constructing road nodes for straight path case
//            else // Constructing road nodes for other cases
//            {
//                int baseVehicleID = chooseTheBaseVehicle();
//                ArrayList<String> vehiclePath = vehicleList.get(baseVehicleID).getMovementPath();
//                ArrayList<String> roadPath = new ArrayList<String>();
//
//                // Something very wrong is happening, the path of the chosen vehicle only has 2 coord
//                if (vehiclePath.size() <= 2)
//                {
//                    ConsoleLogger.print('d',"Dead Wrong");
//                    return "Dead Wrong";
//                }
//
//                for (int i = 0; i < vehiclePath.size() - 1; i++)
//                {
//                    if (vehiclePath.get(i).split(":")[0].equals(vehiclePath.get(i + 1).split(":")[0]))
//                    {
//                        vehiclePath.remove(i + 1);
//                    }
//                }
//
//                ConsoleLogger.print('d',"Chosen Vehicle " + vehicleList.get(baseVehicleID).getVehicleId());
//                ConsoleLogger.print('d',"Chosen Vehicle Path " + vehicleList.get(baseVehicleID).getMovementPath());
//
//
//                StringBuilder nodeListStr = new StringBuilder();
//                String initPosition = "";
//
//                lengthenTheRoad(vehiclePath, radius);
//                roadPath.addAll(vehiclePath);
//
//                if (!AccidentParam.isGradingConcerned)
//                {
//                    gradeIncrement = 0;
//                }
//                else
//                {
//                    if (roadGradeDirection.equalsIgnoreCase("uphill")) {
//                        // Compute distance between 2 nodes
//                        double xCoord1 = Double.parseDouble(vehiclePath.get(0).split(":")[0]);
//                        double xCoord2 = Double.parseDouble(vehiclePath.get(1).split(":")[0]);
//
//                        // percent is calculated by : 100 * rise / run => rise = percent/100/run
//                        gradeIncrement = gradeDegree / 100 / Math.abs(xCoord1 - xCoord2);
//                        ConsoleLogger.print('d',"Grade Increment " + gradeIncrement + " xCoord1 " + xCoord1 + " xCoord2 " + xCoord2);
//                    }
//                }
//
//                // Adjust the yCoord of the vehicle path
//                for (int i = 0; i < vehiclePath.size(); i++)
//                {
//                    String otherData = " " + laneNumber * AccidentParam.laneWidth;
//
//                    //ConsoleLogger.print('d',"coord before " + vehiclePath.get(i));
//                    String yCoordStr = vehiclePath.get(i).split(":")[1];
//
//
//                    // Increase yCoord by 1 and zCoord
//                    double newYCoord = Double.parseDouble(yCoordStr) + 1;
//
//                    String[] coordElements = vehiclePath.get(i).split(":");
//
//                    String baseCoord = coordElements[0] + " " + yCoordStr
//                            + " " + AccidentParam.df6Digit.format(gradeIncrement * i);
//
//                    String newCoord = coordElements[0] + " " + AccidentParam.df6Digit.format(newYCoord)
//                            + " " + AccidentParam.df6Digit.format(gradeIncrement * i);
//
//                    vehiclePath.set(i, baseCoord);
//                    roadPath.set(i, newCoord);
//
//
//                    // Set initial Position for the road
//                    if (i == 0) {
//                        initPosition = newCoord;//.replace(otherData, "");
//                    }
//                    // TODO: Construct Node List for road object
//                    // Make a JSON element format
//                    nodeListStr.append("\t\tNode = \"" + newCoord + "\";\n");
//                }
//
//                try
//                {
////                    List<String> fileContent = Files.readAllLines(headerFilePath, Charset.defaultCharset());
////
////                    for (int i = 0; i < fileContent.size(); i++) {
////                        environmentFileStrBuilder.append(fileContent.get(i) + "\n");
////                    }
//                    ConsoleLogger.print('d',"Modified Vehicle Path " + vehiclePath);
//                    ConsoleLogger.print('d',"Modified Road Path " + roadPath);
//                    // Update to environment file
//                    environmentFileStrBuilder.append(constructRoadObjs(initPosition, nodeListStr.toString(),
//                            roadPath, laneNumber, isPavementNeeded, road));
//
//                    environmentFileStrBuilder.append(constructWaypointsAndVehicles(vehiclePath, scenarioName));
//                    environmentFileStrBuilder.append("\n");
//                }
//                catch (Exception ex)
//                {
//                    environmentFileStrBuilder.append("Error in constructing Environment " + ex);
//                }
//            } // End constructing road nodes for other accident types
//        } // End processing each street
//
//        // Construct waypoints and vehicles objects
//        if (testCaseInfo.getCrashType().startsWith("straight paths")
//                || testCaseInfo.getCrashType().contains("turn into") ||
//                (testCaseInfo.getCrashType().contains("rear-end")
//                    || testCaseInfo.getCrashType().toLowerCase().contains("rearend")
//                    || testCaseInfo.getCrashType().toLowerCase().contains("rear end")))
//        {
//            for (int v = 0; v < vehicleList.size(); v++)
//            {
//                environmentFileStrBuilder.append(constructWaypointsAndVehiclesFor2Roads(vehicleList.get(v), scenarioName));
//
//            }

        environmentFileStrBuilder.append(constructWaypointsAndVehicles(scenarioName));
        ConsoleLogger.print('d', "Final Road Str Builder Obj " + environmentFileStrBuilder.toString());
        return environmentFileStrBuilder.toString();

//        environmentFileStrBuilder.append("Error in constructing Waypoints and Vehiles " + ex);
//        return environmentFileStrBuilder.toString();

    }

    private String constructRoadObjs(String initPosition, String nodeListStr, ArrayList<String> vehiclePath,
                                   int laneNumber, boolean isPavementNeeded, Street processingRoad) {
        ConsoleLogger.print('d',"init Position: " + initPosition);
        ConsoleLogger.print('d',"Node List " + nodeListStr);

        String roadID = processingRoad.getStreetPropertyValue("road_ID");
        String roadNavigation = processingRoad.getStreetPropertyValue("road_navigation");
        int basePriority = 10;

        try
        {
            // Construct Road Template
            Path path = Paths.get(AccidentParam.roadFilePath);
            List<String> fileContent = Files.readAllLines(path, Charset.defaultCharset());
//            ConsoleLogger.print('d',"Accident File Content");

//            StringBuilder allRoadStrBuilder = new StringBuilder();
            StringBuilder roadStrBuilder = new StringBuilder();
            // Find the line with MeshRoad
            for (int i = 0; i < fileContent.size(); i++) {
                roadStrBuilder.append(fileContent.get(i) + "\n");
            }

//            ConsoleLogger.print('d',"roadStrBuilder \n" + roadStrBuilder.toString());

            // Construct the main lane
            String lane1RoadStr = roadStrBuilder.toString();
//            lane1RoadStr = lane1RoadStr.replace("$laneName", "lane" + roadID);
//            lane1RoadStr = lane1RoadStr.replace("$drivable", "1");
//            lane1RoadStr = lane1RoadStr.replace("$priority", "10");
//            lane1RoadStr = lane1RoadStr.replace("$material", AccidentConstructorUtil.getRoadMaterial(processingRoad));
//            lane1RoadStr = lane1RoadStr.replace("$initCoord", initPosition);


            ConsoleLogger.print('d',"isPavementNeeded? " + isPavementNeeded);
            boolean hasParkingLine = false;
            if (isPavementNeeded)
            {
                String lane1RoadCoord = appendWidthToNodeList(laneNumber * AccidentParam.laneWidth, vehiclePath);

                //lane1RoadStr = lane1RoadStr.replace("$nodeList", lane1RoadCoord);

                lane1RoadStr = constructRoadObject("lane" + roadID, "10", processingRoad,
                        initPosition, "-1", lane1RoadCoord);

                processingRoad.putValToKey("road_node_list", lane1RoadCoord.replace("\n", "")
                        .replace("\t", "")
                        .replace("Node = \"", "")
                        .replace("\"", ""));

                ConsoleLogger.print('d',"lane1RoadStr: \n" + lane1RoadStr);

//                String pavementRoadStr = roadStrBuilder.toString();
//                pavementRoadStr = pavementRoadStr.replace("$laneName", "pavement1");
//                pavementRoadStr = pavementRoadStr.replace("$priority", "9");
//                pavementRoadStr = pavementRoadStr.replace("$material", AccidentParam.pavementMaterial);

                // Extract the yCoord and replace the new yCoord to the base initPos
                double pavementInitYPos = Double.parseDouble(
                        initPosition.split(" ")[1]) - (laneNumber / 2 * AccidentParam.laneWidth);
                String pavementInitPos = initPosition.replace(initPosition.split(" ")[1], AccidentParam.df6Digit.format(pavementInitYPos));

//                pavementRoadStr = pavementRoadStr.replace("$initCoord", pavementInitPos);

                ArrayList<String> pavementPath = new ArrayList<String>();
                pavementPath.addAll(vehiclePath);

                // Edit the coord of pavement by taking the base coord - 5
                for (int i = 0; i < pavementPath.size(); i++) {
                    String originalCoord = pavementPath.get(i);
                    String[] originalCoordElements = originalCoord.split(" ");
                    double newYPos = Double.parseDouble(
                            originalCoord.split(" ")[1]) - (laneNumber / 2 * AccidentParam.laneWidth
                            + AccidentParam.laneWidth / 2);
                    String newYPosStr = originalCoordElements[0] + " " +
                            AccidentParam.df6Digit.format(newYPos) + " " + originalCoordElements[2] + " ";
                    pavementPath.set(i, newYPosStr);
                }

                // Construct pavement on the left side
//                String pavement2RoadStr = roadStrBuilder.toString();
//                pavement2RoadStr = pavement2RoadStr.replace("$laneName", "pavement2");
//                pavement2RoadStr = pavement2RoadStr.replace("$priority", "9");
//                pavement2RoadStr = pavement2RoadStr.replace("$material", AccidentParam.pavementMaterial);

                // Extract the yCoord and replace the new yCoord to the base initPos
                double pavement2InitYPos = Double.parseDouble(
                        initPosition.split(" ")[1]) + ( laneNumber / 2 * AccidentParam.laneWidth);
                String pavement2InitPos = initPosition.replace(initPosition.split(" ")[1], AccidentParam.df6Digit.format(pavement2InitYPos));

//                pavement2RoadStr = pavement2RoadStr.replace("$initCoord", pavement2InitPos);

                ArrayList<String> pavement2Path = new ArrayList<String>();
                pavement2Path.addAll(vehiclePath);

                // Edit the coord of pavement2 by taking the base coord + #lane * laneWidth
                for (int i = 0; i < pavement2Path.size(); i++) {
                    String originalCoord = pavement2Path.get(i);
                    String[] originalCoordElements = originalCoord.split(" ");
                    double newYPos = Double.parseDouble(
                            originalCoord.split(" ")[1]) + ( laneNumber / 2 * AccidentParam.laneWidth
                            + AccidentParam.laneWidth / 2);
                    String newYPosStr = originalCoordElements[0] + " " +
                            AccidentParam.df6Digit.format(newYPos) + " " + originalCoordElements[2] + " ";
                    pavement2Path.set(i, newYPosStr);
                }

                String newPavementCoordPos = appendWidthToNodeList(AccidentParam.laneWidth, pavementPath);

                String newPavement2CoordPos = appendWidthToNodeList(AccidentParam.laneWidth, pavement2Path);

//                pavementRoadStr = pavementRoadStr.replace("$nodeList", newPavementCoordPos);

                processingRoad.putValToKey("right_pavement_node_list", newPavementCoordPos.replace("\n", "")
                        .replace("\t", "")
                        .replace("Node = \"", "")
                        .replace("\"", ""));

//                pavement2RoadStr = pavement2RoadStr.replace("$nodeList", newPavement2CoordPos);


                String pavementRoadStr = constructRoadObject("pavement1", "9", processingRoad,
                        pavementInitPos, "-1", newPavementCoordPos);

                String pavement2RoadStr = constructRoadObject("pavement2", "9", processingRoad,
                        pavement2InitPos, "-1", newPavement2CoordPos);

                ConsoleLogger.print('d',"pavementRoadStr \n" + pavementRoadStr);

                processingRoad.putValToKey("left_pavement_node_list", newPavement2CoordPos.replace("\n", "")
                        .replace("\t", "")
                        .replace("Node = \"", "")
                        .replace("\"", ""));

                ConsoleLogger.print('d',"pavement2RoadStr \n" + pavement2RoadStr);

                lane1RoadStr += "\n\n" + pavementRoadStr;

                lane1RoadStr += "\n\n" + pavement2RoadStr;
                
                
            }
            else // Construct road with the pavements as decoration
            {
                String lane1RoadCoord = "";

                ConsoleLogger.print('d',"Processing road park line " + processingRoad.getStreetPropertyValue("road_park_line"));

                if (!processingRoad.getStreetPropertyValue("road_park_line").equals("0")
                    && !processingRoad.getStreetPropertyValue("road_park_line").equals(""))
                {
                    lane1RoadCoord = appendWidthToNodeList(AccidentParam.laneWidth  * laneNumber// * (laneNumber + 1) +1 for 2 pavements
                            + AccidentParam.parkingLineWidth * 2, vehiclePath);
                }
                else
                {
                    lane1RoadCoord = appendWidthToNodeList(AccidentParam.laneWidth * laneNumber, vehiclePath);
                }

                // Record the road nodes' coord
                processingRoad.putValToKey("road_node_list", lane1RoadCoord.replace("\n", "")
                                                                .replace("\t", "")
                                                                .replace("Node = \"", "")
                                                                .replace("\"", ""));

                lane1RoadStr = constructRoadObject("lane" + roadID, "10",
                        processingRoad, vehiclePath.get(0), "-1", lane1RoadCoord);

                // Construct Parking Line if applicable
                if (processingRoad.getStreetPropertyValue("road_park_line").equals("0")
                    || processingRoad.getStreetPropertyValue("road_park_line").equals(""))
                {
                    // Skip constructing parking lane right away
                }
                else
                {
                    hasParkingLine = true;
                    int startingSide = -1 ; // -1 = left side; 1 = right side
                    int limitLane = 2; // 1 = left side only; 2 = right or both sides

                    // If there are parking lines on both sides, construct them
                    if (processingRoad.getStreetPropertyValue("road_park_line").equals("3"))
                    {
                        startingSide = -1;
                    }
                    else if (processingRoad.getStreetPropertyValue("road_park_line").equals("1"))
                    {
                        limitLane = 1;
                    }
                    else if (processingRoad.getStreetPropertyValue("road_park_line").equals("2"))
                    {
                        startingSide = 1;
                    }

                    // Adjust the y coord of the parking line
                    for (int r = startingSide; r < limitLane; r += 2) {
                        ArrayList<String> parkingLinePath = new ArrayList<String>();
                        parkingLinePath.addAll(vehiclePath);

                        ConsoleLogger.print('d',"Vehicle Path size " + vehiclePath.size());

                        // Add the parking line to the side of the main road
                        int[] parkingLineXYZOffset = new int[]{0,
                                r * (laneNumber * AccidentParam.laneWidth / 2 + AccidentParam.parkingLineWidth / 2), 0};

                        // Update the parking line coords and construct the node list string
                        updateCoordElementForNodeList(parkingLinePath, parkingLineXYZOffset);
                        String parkingLineNodeListStr = appendWidthToNodeList(AccidentParam.parkingLineWidth, parkingLinePath);
                        
                        ConsoleLogger.print('d',"Vehicle path " + vehiclePath);
                        ConsoleLogger.print('d',"parking Line path " + parkingLineNodeListStr);

                        String sideIndicator = r == -1 ? "right" : "left";

                        // Record the nodes' coord of this parking line
                        processingRoad.putValToKey(sideIndicator + "_parkline_node_list", parkingLineNodeListStr.replace("\n", "")
                                .replace("\t", "")
                                .replace("Node = \"", "")
                                .replace("\"", ""));

                        String parkingLineRoadStr = constructRoadObject("parkingLine" + sideIndicator, "8",
                                processingRoad, parkingLinePath.get(0), "-1", parkingLineNodeListStr);


                        lane1RoadStr += "\n\n" + parkingLineRoadStr;

                    }

                } // End processing road parking line



            // Construct Deco pavement for both sides of the road
            for (int r = -1; r < 2; r += 2)
            {
                ArrayList<String> pavementPath = new ArrayList<String>();
                pavementPath.addAll(vehiclePath);

                int[] pavementXYZOffset = new int[] {0, 0, 0};

                // If we have parking line, take the parking line width into account
                if (hasParkingLine)
                {
                    // If this is a E/W road, adjust the offset on the Y direction
                    if (roadNavigation.equals("E") || roadNavigation.equals("W"))
                    {
                        pavementXYZOffset = new int[] {0,
                                r * (laneNumber * AccidentParam.laneWidth / 2 + AccidentParam.parkingLineWidth + 2), 0};
                    }
                    // If this is a N/S road, adjust the offset on the X direction
                    else if (roadNavigation.equals("S") || roadNavigation.equals("N"))
                    {
                        pavementXYZOffset = new int[] {
                                r * (laneNumber * AccidentParam.laneWidth / 2 + AccidentParam.parkingLineWidth + 2),
                                0, 0};
                    }
                }
                else // Exclude the parking line width in the pavement construction
                {
                    // If this is a E/W road, adjust the offset on the Y direction
                    if (roadNavigation.equals("E") || roadNavigation.equals("W"))
                    {
                        pavementXYZOffset = new int[] {0, r * (laneNumber * AccidentParam.laneWidth / 2 + 2), 0};
                    }
                    // If this is a N/S road, adjust the offset on the X direction
                    else if (roadNavigation.equals("S") || roadNavigation.equals("N"))
                    {
                        pavementXYZOffset = new int[]{r * (laneNumber * AccidentParam.laneWidth / 2 + 2), 0, 0};
                    }
                    else
                    {
                        pavementXYZOffset = new int[]{r * (laneNumber * AccidentParam.laneWidth - 4), 0, 0};
                    }
                }


                updateCoordElementForNodeList(pavementPath, pavementXYZOffset);
                String pavementRoadNodeListStr = appendWidthToNodeList(AccidentParam.laneWidth -1, pavementPath);

                String sideIndicator = r == -1 ? "right" : "left";

                // Construct BeamNG pavement road
//                String pavementRoadStr = roadStrBuilder.toString();
//                pavementRoadStr = pavementRoadStr.replace("$laneName", "pavement" + sideIndicator);
//                pavementRoadStr = pavementRoadStr.replace("$priority", "9");
//                pavementRoadStr = pavementRoadStr.replace("$material", AccidentParam.pavementMaterial);
//                pavementRoadStr = pavementRoadStr.replace("$initCoord", pavementPath.get(0));
//                pavementRoadStr = pavementRoadStr.replace("$nodeList", pavementRoadNodeListStr);
//                pavementRoadStr = pavementRoadStr.replace("drivability = \"1\"", "drivability = \"-1\"");

                String pavementRoadStr = constructRoadObject("pavement" + sideIndicator + "_" + roadID, basePriority + 1 + "",
                        processingRoad, pavementPath.get(0), "-1", pavementRoadNodeListStr);

                // Update the node coords into the corresponding pavement side of the road
                processingRoad.putValToKey(sideIndicator + "_pavement_node_list", pavementRoadNodeListStr.replace("\n", "")
                        .replace("\t", "")
                        .replace("Node = \"", "")
                        .replace("\"", ""));

                ConsoleLogger.print('d',"lane1RoadStr Deco Pavement" + r + ": \n" + lane1RoadStr);
                lane1RoadStr = lane1RoadStr + "\n\n" + pavementRoadStr;
            }

            // TODO: CONSIDER THIS BRACKET when running multiple streets cases
            } // End construct pavement as deco

            double pavementPadding = isPavementNeeded ? AccidentParam.laneWidth / 2 : 0;

            // Construct the road division(s) based on the number of lanes
            for (int i = 1; i < laneNumber; i++) {
                ArrayList<String> currentlaneDivisionPath = new ArrayList<String>();
                currentlaneDivisionPath.addAll(vehiclePath);

                for (int j = 0; j < currentlaneDivisionPath.size(); j++)
                {
                    // Modify the y coord of the lane node by taking the current coord + totalLaneWidth/2 - i * laneWidth
                    String originalCoord = currentlaneDivisionPath.get(j);
                    String[] originalCoordElements = originalCoord.split(" ");

                    double modifiedPos = 0;
                    String newPosStr = "";

                    if (roadNavigation.equals("N") || roadNavigation.equals("S"))
                    {
                        ConsoleLogger.print('d',"Draw split line NS road");
                        modifiedPos = Double.parseDouble(originalCoord.split(" ")[1]) // original Y
                                + laneNumber * AccidentParam.laneWidth / 2 // (laneNumber + 1 pavement) * width / 2
                                - i * AccidentParam.laneWidth + pavementPadding; // j * laneWidth

                        newPosStr = originalCoordElements[0] + " " +
                                AccidentParam.df6Digit.format(modifiedPos) + " " + originalCoordElements[2] + " ";
                    }
                    else if (roadNavigation.equals("E") || roadNavigation.equals("W"))
                    {
                        ConsoleLogger.print('d',"Draw split line EW road");
                        modifiedPos = Double.parseDouble(originalCoord.split(" ")[1]) // original Y
                                + laneNumber * AccidentParam.laneWidth / 2 // (laneNumber + 1 pavement) * width / 2
                                - i * AccidentParam.laneWidth ; // j * laneWidth

//                        newPosStr = AccidentParam.df6Digit.format(modifiedPos) + " "
//                                + (Double.parseDouble(originalCoordElements[1]) + pavementPadding) + " "
//                                + originalCoordElements[2] + " ";

                        newPosStr = (Double.parseDouble(originalCoordElements[0]) + " "
                                + AccidentParam.df6Digit.format(modifiedPos)) + " "
                                + originalCoordElements[2] + " ";
                    }
                    else // NS NW SE SW directions
                    {

                        modifiedPos = (Double.parseDouble(originalCoord.split(" ")[1]) - 3 - laneNumber * AccidentParam.laneWidth / 2)  // Get start point from original X
                                + (AccidentParam.laneWidth + 2) * i // Get total width by far
                                 // Divide into equal pieces
                                ; // get the coord at the end length of that piece

                        newPosStr = originalCoordElements[0] + " "
                                + AccidentParam.df6Digit.format(modifiedPos) + " " + originalCoordElements[2] + " ";
                    }
                    currentlaneDivisionPath.set(j, newPosStr);
                }

//                String roadDivisionStr = roadStrBuilder.toString();
//                roadDivisionStr = roadDivisionStr.replace("$laneName", "roadDivision" + i);
//                roadDivisionStr = roadDivisionStr.replace("$priority", "9");
//                roadDivisionStr = roadDivisionStr.replace("$material", AccidentParam.laneDivisionMaterial);
//                roadDivisionStr = roadDivisionStr.replace("$initCoord", initPosition);
//                roadDivisionStr = roadDivisionStr.replace("drivability = \"1\"", "drivability = \"-1\"");
                String roadDivisionCoord = appendWidthToNodeList(0.1, currentlaneDivisionPath);
//                roadDivisionStr = roadDivisionStr.replace("$nodeList", roadDivisionCoord);
                String roadDivisionStr = constructRoadObject("roadDivision" + i + "_" + roadID, "9",
                        processingRoad, initPosition, "-1", roadDivisionCoord);
                lane1RoadStr += "\n\n" + roadDivisionStr;
            }

            ConsoleLogger.print('d',"Final Road String \n" + lane1RoadStr);
            return lane1RoadStr;
//            byte[] buffer = updateEnvironmentFile.toString().getBytes();
            // Write to File
//            Files.write(path, buffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "Error in constructing road " + e;
        }
    }

    private String appendWidthToNodeList(double roadWidth, ArrayList<String> coordPath)
    {
        StringBuilder nodeListStr = new StringBuilder();
        for (int i = 0; i < coordPath.size(); i++)
        {
            String newCoord = coordPath.get(i) + " " + roadWidth;
//            newElement = newElement.substring(0, newElement.length() - 2);
            nodeListStr.append("\t\tNode = \"" + newCoord + "\";\n");
        }
        return nodeListStr.toString();
    }
    
    private void lengthenTheRoad(ArrayList<String> roadNodesList, double radius)
    {
        int additionalLength = 30;
        
        // Construct new beginning node by decreasing x by 30 and set y depending on the radius
        addNodeWithLength(roadNodesList, radius, additionalLength * -1, ":", 0);

        // Construct new last node by increasing x by 30 and set y depending on the radius
        addNodeWithLength(roadNodesList, radius, additionalLength , ":", -1);

    }
    
    private void addNodeWithLength(ArrayList<String> roadNodesList, double radius, int additionalLength,
                                   String delimiter, int position)
    {
        String[] newNodeElements;
        // Add node at a specific position
        if (position > -1)
        {
            newNodeElements = roadNodesList.get(position).split(delimiter);

        }
        else // Add node at the end
        {
            newNodeElements = roadNodesList.get(roadNodesList.size() - 1).split(delimiter);
        }

        newNodeElements[0] = "" + AccidentParam.df6Digit.format((Double.parseDouble(newNodeElements[0]) + additionalLength));
        ConsoleLogger.print('d',"new xCoord 1st " + newNodeElements[0]);
        if (radius != 0)
        {
            newNodeElements[1] = "" + AccidentParam.df6Digit.format(AccidentConstructorUtil.computeXCircleFunc
                    (radius, Double.parseDouble(newNodeElements[0])));
            ConsoleLogger.print('d',"new yCoord 1st " + newNodeElements[1]);
        }
        ConsoleLogger.print('d',"new yCoord 1st " + newNodeElements[1]);
        String newNodeCoord = "";
        for (String element : newNodeElements)
        {
            newNodeCoord += element + delimiter;
        }

        if (position > -1)
        {
            roadNodesList.add(position, newNodeCoord.substring(0, newNodeCoord.length() - 1));
        }
        else
        {
            roadNodesList.add(newNodeCoord.substring(0, newNodeCoord.length() - 1));
        }
        
    }

    private String constructWaypointsAndVehiclesFor2Roads(VehicleAttr currentVehicle, String scenarioName)
    {
        Set<String> impactedCoords = new HashSet<String>();

        Path waypointFilePath = Paths.get(AccidentParam.waypointFilePath);
        Path vehicleFilePath = Paths.get(AccidentParam.vehicleFilePath);
        Path luaPathFollowerFilePath = Paths.get(AccidentParam.luaAIFilePath);
        Path luaAIPathFollowerConfigFilePath = Paths.get(AccidentParam.luaAIConfigFilePath);
        Path luaAILeaveTriggerPath = Paths.get(AccidentParam.luaAICarLeaveTriggerFilePath);

        StringBuilder waypointStrBuilderTemplate = new StringBuilder();
        StringBuilder waypointListStrBuilder = new StringBuilder();
        StringBuilder vehicleStrBuilderTemplate = new StringBuilder();
        StringBuilder vehicleListStrBuilder = new StringBuilder();
        StringBuilder luaPathStrBuilderTemplate = new StringBuilder();
        StringBuilder luaAIConfigStrBuilderTemplate = new StringBuilder();
        StringBuilder luaAILeaveTriggerStrBuilderTemplate = new StringBuilder();

        String waypointTemplate = "";
        String vehicleTemplate  = "";

        String vehicleInfoStr = "";

        Street currentStreet = currentVehicle.getStandingStreet();
        double laneNumber = Double.parseDouble(currentStreet.getStreetPropertyValue("lane_num"));

        double roadGradePercent = Double.parseDouble(currentStreet.getStreetPropertyValue("road_grade_deg"));

        // Approximate the grade degree. 1st get the base grade degree at point O
        String[] roadNodeArr = currentStreet.getStreetPropertyValue("road_node_list").split(";");


        try {
            // Load waypoint template and convert to String
            waypointStrBuilderTemplate.append(loadTemplateFileContent(waypointFilePath) + "\n");

            waypointTemplate = waypointStrBuilderTemplate.toString();

            // Load vehicle template and convert to String
            vehicleStrBuilderTemplate.append(loadTemplateFileContent(vehicleFilePath) + "\n");

            vehicleTemplate = vehicleStrBuilderTemplate.toString();
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('e',"Exception in init template when constructing waypoints \n" + ex);
        }



        double baseGradeHeight = 0;

        // Find the grade degree at point O
        for (String roadCoord : roadNodeArr)
        {
            if (roadCoord.startsWith("0 0"))
            {
                baseGradeHeight = Double.parseDouble(roadCoord.split(" ")[2]);
            }
        }

        // Compute the grade for each coord in the vehicle path
        ArrayList<String> currentVehiclePath = currentVehicle.getMovementPath();

        for (int n = 0; n < currentVehiclePath.size(); n++)
        {
            String currentCoord = currentVehiclePath.get(n).replace(":", " ");
            double selectedCoord = 0;
            // If the road is on N/S direction, get the yCoord
            if (currentStreet.getStreetPropertyValue("road_navigation").equals("N")
                || currentStreet.getStreetPropertyValue("road_navigation").equals("S"))
            {
                selectedCoord = Double.parseDouble(currentVehiclePath.get(n).split(":")[1]);
            }
            // If the road is on E/W direction, get the xCoord
            else if (currentStreet.getStreetPropertyValue("road_navigation").equals("E")
                    || currentStreet.getStreetPropertyValue("road_navigation").equals("W"))
            {
                selectedCoord = Double.parseDouble(currentVehiclePath.get(n).split(":")[0]);
            }

            double gradeChange = AccidentConstructorUtil.computeGradeIncrement(selectedCoord, 0, roadGradePercent);

            // Adjust the grade depending on whether this coord is before or after the O point

            if (roadGradePercent > 0) // decrease the grade
            {
                if (selectedCoord > 0) // Before O point (N or E side)
                {
                    currentVehiclePath.set(n, currentCoord + " " + AccidentParam.df6Digit.format(baseGradeHeight - gradeChange));
                }
                else if (selectedCoord <= 0) // After O point (S or W side)
                {
                    currentVehiclePath.set(n, currentCoord + " " + AccidentParam.df6Digit.format(baseGradeHeight + gradeChange));
                }
            }
            else if (roadGradePercent < 0) // Increase the grade
            {
                if (selectedCoord > 0) // Before O point (N or E side)
                {
                    currentVehiclePath.set(n, currentCoord + " " + AccidentParam.df6Digit.format(baseGradeHeight + gradeChange));
                }
                else if (roadGradePercent <= 0) // increase the grade
                {
                    currentVehiclePath.set(n, currentCoord + " " + AccidentParam.df6Digit.format(baseGradeHeight - gradeChange));
                }
            }
            else  // Level road, append 0
            {
                currentVehiclePath.set(n, currentCoord + " " + baseGradeHeight);
            }

        }
        currentVehicle.setMovementPath(currentVehiclePath);
        ConsoleLogger.print('d',"Waypoints of Vehicle " + currentVehicle.getVehicleId() + " is " + currentVehicle.getMovementPath());

        // Construct waypoint obj lists
        StringBuilder waypointPathLuaStrBuilder = new StringBuilder();

        // If this is a parked car, check if the second coord (crash coord) matches with the second last
        // coord of chosen vehicle (also should be crash coord)

        // Add the impact coord to the impact coord set so we can make the crash waypoints not overlap when constructing waypoint path
        vehicleListStrBuilder = constructVehicleObject("" + currentVehicle.getVehicleId(),
                currentVehiclePath.get(0), currentVehicle.getColor(), currentVehicle.getVehicleType(),
                currentVehicle.getPartConfig(),"1", vehicleListStrBuilder);

        int maxLength = currentVehiclePath.size();

        // For turn into path, dont take the last coord because the turn of the car is unpredictable, crash point is enof
        if (testCaseInfo.getCrashType().contains("turn into") || testCaseInfo.getCrashType().contains("rear-end"))
        {
            maxLength -= 1;
        }

        for (int i = 1; i < maxLength; i++) {

            String waypointName = "wp" + i + "_" + currentVehicle.getVehicleId();
            String waypointInfoStr = waypointTemplate.replace("$coord", currentVehiclePath.get(i));


            String scaleValue = "5 5 5";
            // Set the impact point name as "wp_crash" and only 1 vehicle set this
            if (i == currentVehiclePath.size() - 2)
            {
                waypointName = "wp_crash";

                if (currentVehicle.getVehicleId() != 1)
                {
                    waypointPathLuaStrBuilder.append("\'" + waypointName + "\'" + ",");
                    continue;
                }

                if (currentVehicle.getVehicleId() == 1)
                {
                    scaleValue = "";

                    for (int j = 0; j < 3; j++)
                        scaleValue += 3 + " ";//laneNumber * AccidentParam.laneWidth + " ";

                }
            }

            waypointPathLuaStrBuilder.append("\'" + waypointName + "\'" + ",");

            waypointInfoStr = waypointInfoStr.replace("$name", waypointName); // wp[index]_[carID]

            waypointInfoStr = waypointInfoStr.replace("$scale", scaleValue.trim());

            waypointListStrBuilder.append(waypointInfoStr);
        }

        currentVehicle.setWaypointPathNodeName(waypointPathLuaStrBuilder.
                deleteCharAt(waypointPathLuaStrBuilder.length() - 1).toString());

        // Construct LUA logic
        try
        {
            // Generate Lua AI Waypoints Follower Config
            luaAIConfigStrBuilderTemplate.append(loadTemplateFileContent(luaAIPathFollowerConfigFilePath) + "\n");


//            for (VehicleAttr currentVehicle : vehicleList)
//            {
                // Only configure AI Waypoint Follower if the car is moving
            if (currentVehicle.getMovementPath().size() > 1)
            {
                String firstAction = currentVehicle.getActionList().get(0);
                ConsoleLogger.print('d',"First action of vehicle#" + currentVehicle.getVehicleId() + " is " + firstAction + " on street is " + currentVehicle.getOnStreet());
                if (currentVehicle.getOnStreet() >= 1 && !firstAction.equals("stop") && !firstAction.equals("park") ) {

                    ConsoleLogger.print('d',"Append waypoint movement for non-stop vehicle#" + currentVehicle.getVehicleId());
                    String luaAIConfigTemplate = luaAIConfigStrBuilderTemplate.toString();
                    luaAIConfigTemplate = luaAIConfigTemplate.replace("$waypointNameList",
                            currentVehicle.getWaypointPathNodeName());


                    luaAIConfigTemplate = luaAIConfigTemplate.replace("$speed",
                            AccidentParam.df6Digit.format(AccidentConstructorUtil.appendExtraMeterPerSecSpeed(currentVehicle.getVelocity())));


                    luaAIConfigTemplate = luaAIConfigTemplate.replace("$actorID", currentVehicle.getVehicleId() + "");
                    allAIConfigStr.append(luaAIConfigTemplate + "\n\n");
                }
            }
//            }

            // Construct the leave trigger mechanism
            if (vehicleList.size() == 2 && currentVehicle.getLeaveTriggerDistance() != -1)
            {
                VehicleAttr strikerVehicle = null;
                VehicleAttr victimVehicle = null;

                for (VehicleAttr vehicleAttr : vehicleList)
                {
                    if (vehicleAttr.getActionList().get(0).equals("stop"))
                    {
                        victimVehicle = vehicleAttr;
                    }
                    else
                    {
                        strikerVehicle = vehicleAttr;
                    }
                }

                String carLeaveTriggerTemplate = loadTemplateFileContent(luaAILeaveTriggerPath);

                carLeaveTriggerTemplate = carLeaveTriggerTemplate
                        .replace("$P2ID", victimVehicle.getVehicleId() + "")
                        .replace("$wpList", victimVehicle.getWaypointPathNodeName())
                        .replace("$speed", AccidentParam.df6Digit.format(AccidentConstructorUtil.appendExtraMeterPerSecSpeed(victimVehicle.getVelocity())))
                        .replace("$P1ID", strikerVehicle.getVehicleId() + "")
                        // TODO: COmpute Trigger Distance using equation
                        .replace("$collisionDistance", AccidentParam.DISTANCE_BETWEEN_CARS + "");


                double leaveTriggerDistance = victimVehicle.getLeaveTriggerDistance();
                ConsoleLogger.print('d',"Victim Vehicle " + victimVehicle.getVehicleId() + " " + victimVehicle.getLeaveTriggerDistance());
                if (leaveTriggerDistance != -1)
                {
                    carLeaveTriggerTemplate = carLeaveTriggerTemplate.replace("$triggerDistance", "" + leaveTriggerDistance);
                }
                else
                {
                    carLeaveTriggerTemplate = carLeaveTriggerTemplate.replace("$triggerDistance", (strikerVehicle.getVelocity() * 0.8) + "");
                }

                allLuaAIConfigContent = allLuaAIConfigContent.replace("--$OtherVehicleStartToRunCode", carLeaveTriggerTemplate);


            } /// End processing 2 vehicles case

            processedAiConfigVehicleCount++;
            // Add the AI config into the AI Lua template
            if (processedAiConfigVehicleCount == vehicleList.size())
            {
                allLuaAIConfigContent = allLuaAIConfigContent.replace("$setAiMovementPath", allAIConfigStr.toString());
                ConsoleLogger.print('d'," Final allAIConfigStr " + allAIConfigStr.toString());
//            ConsoleLogger.print('d',"Lua AI Template \n" + luaAITemplate);

                Path scenarioConfigPath = Paths.get(AccidentParam.scenarioConfigFilePath + "\\" + scenarioName + ".lua");
                Files.write(scenarioConfigPath, allLuaAIConfigContent.getBytes());
                processedAiConfigVehicleCount = 0;
            }



//            ConsoleLogger.print('d',"Vehicle List \n" + vehicleListStrBuilder.toString());
//            ConsoleLogger.print('d',"Waypoint List \n" + waypointListStrBuilder.toString());
            return vehicleListStrBuilder.toString() + "\n\n"
                    + waypointListStrBuilder.toString() + "\n\n";
        }
        catch (Exception ex)
        {
            ConsoleLogger.print('e',"Error at constructing cars and wp for 2 roads \n ");
            ex.printStackTrace();
            return "Error at constructing cars and wp for 2 roads " + ex.toString();

        }

    }

//    private String constructWaypointsAndVehicles(ArrayList<String> vehiclePath, int laneNumber, boolean isPavementNeeded,
//                                                 double radius, String scenarioName)
    private String constructWaypointsAndVehicles(ArrayList<String> vehiclePath, String scenarioName)
    {
        Path waypointFilePath = Paths.get(AccidentParam.waypointFilePath);
        Path vehicleFilePath = Paths.get(AccidentParam.vehicleFilePath);
        Path luaPathFollowerFilePath = Paths.get(AccidentParam.luaAIFilePath);
        Path luaAIPathFollowerConfigFilePath = Paths.get(AccidentParam.luaAIConfigFilePath);
        Path luaAILeaveTriggerPath = Paths.get(AccidentParam.luaAICarLeaveTriggerFilePath);

        Set<String> impactedCoords = new HashSet<String>();

        StringBuilder waypointStrBuilderTemplate = new StringBuilder();
        StringBuilder waypointListStrBuilder = new StringBuilder();
        StringBuilder vehicleStrBuilderTemplate = new StringBuilder();
        StringBuilder vehicleListStrBuilder = new StringBuilder();
        StringBuilder luaPathStrBuilderTemplate = new StringBuilder();
        StringBuilder luaAIConfigStrBuilderTemplate = new StringBuilder();
        StringBuilder luaAILeaveTriggerStrBuilderTemplate = new StringBuilder();



        try {
            // Load waypoint template and convert to String
            List<String> waypointFileContent = Files.readAllLines(waypointFilePath, Charset.defaultCharset());

            for (int i = 0; i < waypointFileContent.size(); i++) {
                waypointStrBuilderTemplate.append(waypointFileContent.get(i) + "\n");
            }

            String waypointTemplate = waypointStrBuilderTemplate.toString();

            // Load vehicle template and convert to String
            List<String> vehicleFileContent = Files.readAllLines(vehicleFilePath, Charset.defaultCharset());

            for (int i = 0; i < vehicleFileContent.size(); i++) {
                vehicleStrBuilderTemplate.append(vehicleFileContent.get(i) + "\n");
            }

            String vehicleTemplate = vehicleStrBuilderTemplate.toString();

            //            // Construct Observer Vehicle Obj
            String vehicleInfoStr = ""; //vehicleTemplate.replace("$actorID", "0");
            //            vehicleInfoStr = vehicleInfoStr.replace("$position", vehiclePath.get(0));
            //            vehicleInfoStr = vehicleInfoStr.replace("$colorCode", "255 255 255");
            //            vehicleInfoStr = vehicleInfoStr.replace("$isAIControlled", "0");
            //            vehicleListStrBuilder.append(vehicleInfoStr);

            // Cut additional nodes from the base waypointFilePath

            vehiclePath.remove(0);
            vehiclePath.remove(vehiclePath.size() - 1);

            Street currentStreet = null;

            // If there is only 1 road, we work with that road only
            if (testCaseInfo.getStreetList().size() == 1) {
                currentStreet = testCaseInfo.getStreetList().get(0);
            }

            double radius = Double.parseDouble(currentStreet.getStreetPropertyValue("curve_radius"));

            int laneNumber = Integer.parseInt(currentStreet.getStreetPropertyValue("lane_num"));
            // Construct the waypoints based on the waypointFilePath of each vehicle
            for (VehicleAttr vehicleAttr : vehicleList) {
                ArrayList<String> vehicleMovementPath = vehicleAttr.getMovementPath();

                // If this is not the striker car, convert the coords in this waypointFilePath to BeamNG format
                if (!vehicleMovementPath.get(0).equals(vehiclePath.get(0))) {
                    //                    ConsoleLogger.print('d',"Not the processed coord list, vehicle is " + vehicleAttr.getVehicleId() +
                    //                            " on street? " + vehicleAttr.getOnStreet() + " vehMovPath size " + vehicleMovementPath.size());
                    String crashType = testCaseInfo.getCrashType().toLowerCase();

                    if (crashType.contains("forward impact")) {
//                        // If this is a static car, then check whether it is on the pavement or parking line, and set coord accordingly
//                        if (vehicleMovementPath.size() == 1
//                                && (vehicleAttr.getOnStreet() == 0 || vehicleAttr.getOnStreet() == -1)) {
//                            String convertedCoord = convertCoordToBeamNGFormat(vehicleMovementPath.get(0));
//
//                            // Append zCoord to a non zCoord coord
//                            if (convertedCoord.split(" ").length == 2)
//                            {
//                                if (!AccidentParam.isGradingConcerned)
//                                {
//                                    convertedCoord += " 0";
//                                }
//                            }
//
//                            String newYCoord = "";
//
//                            // For straight road, need to place the waypoint far away from the road a bit compared to curvy
//                            // road
//                            ConsoleLogger.print('d',"Standing Road side " + vehicleAttr.getStandingRoadSide());
//                            if (radius == 0.0) {
//
//                                double parkingLineExtraDistance = 0;
//
//                                if (!currentStreet.getStreetPropertyValue("road_park_line").equals("0")
//                                        && !currentStreet.getStreetPropertyValue("road_park_line").equals("") )
//                                {
//                                    ConsoleLogger.print('d',"Parking Line extra distance set to 2");
//                                    parkingLineExtraDistance = 2;
//                                }
//
//                                if (vehicleAttr.getStandingRoadSide().equals("left")) {
//                                    newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(" ")[1]) +
//                                            (int) (laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth / 2) + parkingLineExtraDistance - 0.5 );
//                                    //(int) (laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth / 2 + parkingLineExtraDistance) - 0.5 );
//                                } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
//                                    newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(" ")[1]) -
//                                            (int) (laneNumber / 2.0 * AccidentParam.laneWidth - AccidentParam.parkingLineWidth / 2 ) - parkingLineExtraDistance + 0.5);
//
//                                }
//
//                            } else {
//                                if (vehicleAttr.getStandingRoadSide().equals("left")) {
//                                    newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(" ")[1]) +
//                                            (laneNumber / 2.0 * AccidentParam.laneWidth) - 1.5);
//                                    //(laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth + 1) + 1);
//                                } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
//
//                                    newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(" ")[1]) -
//                                            (laneNumber / 2.0 * AccidentParam.laneWidth) + 1.5);
//                                    ConsoleLogger.print('d',"Right Curve Park " + Double.parseDouble(convertedCoord.split(" ")[1])
//                                    + " " + (laneNumber / 2.0 * AccidentParam.laneWidth) + " ");
//                                }
////                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(" ")[1]) -
////                                    (laneNumber / 2 * AccidentParam.laneWidth - 1))  ;
//                            }
//
//                            ConsoleLogger.print('d',"convert coord " + convertedCoord);
//                            ConsoleLogger.print('d',"newYCoord " + newYCoord);
//                            String newCoord = updateCoordElementAtDimension(1, convertedCoord, newYCoord);
//                            ConsoleLogger.print('d',"newCoord  " + newCoord);
//
//                            // Set the updated impact point to the other car
//                            for (VehicleAttr otherVehicle : vehicleList) {
//                                ConsoleLogger.print('d',"other veh ID " + otherVehicle.getVehicleId());
//                                if (otherVehicle.getVehicleId() != vehicleAttr.getVehicleId()) {
//                                    ArrayList<String> otherVehicleMovementPath = otherVehicle.getMovementPath();
//                                    for (int j = 0; j < otherVehicleMovementPath.size(); j++) {
//                                        String[] pathNodeElements = otherVehicleMovementPath.get(j).split(" ");
//                                        String[] convertedCoordElements = convertedCoord.split(" ");
//
//                                        // Check whether this is the crash points between the 2 cars
//                                        if (pathNodeElements[0].equals(convertedCoordElements[0])
//                                                && pathNodeElements[1].equals(convertedCoordElements[1])) {
//                                            // Update the right grade value (zCoord)
//
//                                            newCoord = updateCoordElementAtDimension(2, newCoord, pathNodeElements[2]);
//
//                                            // Adjust the car position far away a bit to ensure crash
//                                            String adjustedCoord = updateCoordElementAtDimension(0, newCoord,
//                                                    (Double.parseDouble(convertedCoordElements[0]) + 3) + "");
//
//                                            otherVehicleMovementPath.set(j, adjustedCoord);
//                                            otherVehicle.setMovementPath(otherVehicleMovementPath);
//
//                                            vehicleMovementPath.set(0, newCoord);
//                                            vehicleAttr.setMovementPath(vehicleMovementPath);
//
//                                            // For straight road, put the wp away the street a bit
//                                            if (radius == 0.0) {
//                                                if (vehicleAttr.getStandingRoadSide().equals("left")) {
//                                                    newYCoord = AccidentParam.df6Digit.format((Double.parseDouble(newYCoord)) - 1);
//                                                } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
//                                                    newYCoord = AccidentParam.df6Digit.format((Double.parseDouble(newYCoord)) + 1);
//                                                }
//                                                newCoord = updateCoordElementAtDimension(1, newCoord, newYCoord);
//
//                                                ConsoleLogger.print('d',"new coord straight road update " + newCoord);
//                                            }
//
//                                            adjustedCoord = updateCoordElementAtDimension(1, adjustedCoord, newYCoord);
//
//
//                                            ConsoleLogger.print('d',"Found same impact coord at " + j + " value " + adjustedCoord);
//                                            impactedCoords.add(adjustedCoord);
//                                        }
//                                    }
//                                }
//                            } // End looping through other vehicles and set impact points
//                        } // End checking vehicle is on parking line or pavement
//                        else {
                        for (int i = 0; i < vehicleMovementPath.size(); i++) {
                            String newCoord = convertCoordToBeamNGFormat(vehicleMovementPath.get(i));
                            vehicleMovementPath.set(i, newCoord);
                        }
//                        }
                    } // End adjusting waypoint for forward impact
                    else if (crashType.contains("sideswipe")) {
                        ConsoleLogger.print('d',"In Sideswipe crash");
                        // Convert x:y coords to BeamNG coord format
                        for (VehicleAttr currVehicle : vehicleList) {
                            ArrayList<String> currVehicleCoordList = currVehicle.getMovementPath();

                            // If this is not the chosen base vehicle, append the grade to the vehicle
                            if (!currVehicleCoordList.get(0).equals(vehiclePath.get(0))) {
                                double roadGradeDegree = Double.parseDouble(
                                        currentStreet.getStreetPropertyValue("road_grade_deg"));

                                double currentRoadGrade = Double.parseDouble(
                                        vehiclePath.get(vehiclePath.size() - 1).split(" ")[2]);

                                // Append the grade based on previous and current xCoords
                                for (int c = currVehicleCoordList.size() - 1; c > 0; c--) {
                                    String coordAtC = currVehicleCoordList.get(c);
                                    String prevCoord = currVehicleCoordList.get(c - 1);

                                    // Find the prev and current xCoords
                                    double xCoordAtC = Double.parseDouble(coordAtC.split(":")[0]);
                                    double xCoordPrev = Double.parseDouble(prevCoord.split(":")[0]);

                                    // If grading is not concerned, set as 0, otherwise, compute the grade
                                    double gradeIncrement = AccidentParam.isGradingConcerned ?
                                            AccidentConstructorUtil.computeGradeIncrement(xCoordPrev, xCoordAtC, currentRoadGrade)
                                            : 0;

                                    // Set the updated grade degree into the coord value at curr and prev positions
                                    currVehicleCoordList.set(c,
                                            convertCoordToBeamNGFormat(coordAtC) + " "
                                                    + AccidentParam.df6Digit.format(currentRoadGrade)
                                    );

                                    currVehicleCoordList.set(c - 1,
                                            convertCoordToBeamNGFormat(prevCoord + ":") + " "
                                                    + AccidentParam.df6Digit.format(currentRoadGrade - gradeIncrement)
                                    );

                                    currentRoadGrade -= gradeIncrement;
                                } // End convert x:y coord to BeamNG format for each coord
                            } // End checking if the current car is not the same as the base car

                        }
                    }
                } // End checking the same vehicle waypointFilePath
            } // End looping through each vehicle

            //}
            // Begin constructing waypoint objects

            ConsoleLogger.print('d',"Veh List Size " + vehicleList.size());
            for (VehicleAttr currentVehicle : vehicleList)
            {
                ArrayList<String> currentVehiclePath = currentVehicle.getMovementPath();

                // Construct AI Vehicle Obj
//                vehicleInfoStr = vehicleTemplate.replace("$actorID",
//                        "" + currentVehicle.getVehicleId());
//                vehicleInfoStr = vehicleInfoStr.replace("$position", currentVehiclePath.get(0));
//                vehicleInfoStr = vehicleInfoStr.replace("$colorCode", currentVehicle.getColor());
//                vehicleInfoStr = vehicleInfoStr.replace("$jbeam", currentVehicle.getVehicleType());
//                vehicleInfoStr = vehicleInfoStr.replace("$partConfig", currentVehicle.getPartConfig());
//                vehicleInfoStr = vehicleInfoStr.replace("$isAIControlled", "1");
//                vehicleListStrBuilder.append(vehicleInfoStr);
                ConsoleLogger.print('d',"Construct vehicle obj for vehicle#" + currentVehicle.getVehicleId());
                vehicleListStrBuilder = constructVehicleObject("" + currentVehicle.getVehicleId(),
                        currentVehiclePath.get(0), currentVehicle.getColor(), currentVehicle.getVehicleType(),
                        currentVehicle.getPartConfig(),"1", vehicleListStrBuilder);

                // Construct the waypoints for mobile car
                if (currentVehiclePath.size() > 1)
                {
                    // For sideswipe crash, add another waypoint at 10m ahead to make the cars move naturally
                    if (testCaseInfo.getCrashType().toLowerCase().contains("sideswipe"))
                    {
                        // If this is a parked vehicle, skip constructing waypoint path coz we will do it in Lua config
                        String lastWaypoint = currentVehiclePath.get(currentVehiclePath.size() - 1);

                        currentVehiclePath.add(updateCoordElementAtDimension(0,
                                lastWaypoint,
                                "" + Double.parseDouble(lastWaypoint.split(" ")[0] + 10)));

                        // Parked car will go further away if non-critical param is set
                        if (AccidentConstructorUtil.getNonCriticalDistance() > 0)
                        {
                            if (currentVehicle.getActionList().get(0).equals("park"))
                                currentVehiclePath.add(updateCoordElementAtDimension(0,
                                        lastWaypoint,
                                        "" + Double.parseDouble(lastWaypoint.split(" ")[0] + 15)));
                        }

                    }
                    // Construct waypoint obj lists
                    StringBuilder waypointPathLuaStrBuilder = new StringBuilder();

                    // If this is a parked car, check if the second coord (crash coord) matches with the second last
                    // coord of chosen vehicle (also should be crash coord)
                    if (currentVehicle.getOnStreet() <= 0 && currentVehiclePath.get(1).equals(vehiclePath.get(vehiclePath.size() - 2)))
                    {
                        for (int i = vehiclePath.size() - 2; i < vehiclePath.size(); i++)
                        {
                            ConsoleLogger.print('d',"Choose the base coord waypoint at " + i);
                            String waypointName = "wp" + i + "_" + vehicleIdOfBasePath;
                            waypointPathLuaStrBuilder.append("\'" + waypointName + "\',");
                        }

                        // If non-critical param is given, set the wp of the current vehicle, add the last waypoint inside
                        if (AccidentConstructorUtil.getNonCriticalDistance() > 0)
                        {
                            String waypointInfoStr = waypointTemplate.replace("$name", "wp_goal");
                            waypointPathLuaStrBuilder.append("\'wp_goal\',");
                            waypointInfoStr = waypointInfoStr.replace("$coord",
                                    currentVehiclePath.get(currentVehiclePath.size() - 1));
                            waypointInfoStr = waypointInfoStr.replace("$scale", "3 3 3");
                            waypointListStrBuilder.append(waypointInfoStr);
                        }


                        // Add the waypoint list into the current vehicle waypointNodeNameList
                        currentVehicle.setWaypointPathNodeName(waypointPathLuaStrBuilder.
                                deleteCharAt(waypointPathLuaStrBuilder.length() - 1).toString());

                        // Construct a lane of parked cars
                        // TODO: Check if any car is in the near pos of constructed parked cars
                        vehicleListStrBuilder = constructLaneFilledOfParkedCar(currentVehiclePath, currentStreet,
                                currentVehicle, laneNumber, vehicleTemplate, vehicleListStrBuilder);
                        break;
                    }

                    for (int i = 1; i < currentVehiclePath.size(); i++) {
                        String waypointName = "wp" + i + "_" + currentVehicle.getVehicleId();
                        String waypointInfoStr = waypointTemplate.replace("$name", waypointName); // wp[index]_[carID]

                        waypointPathLuaStrBuilder.append("\'" + waypointName + "\',");

                        String scaleValue = "5 5 5";

                        if (testCaseInfo.getCrashType().contains("sideswipe"))
                        {
                            scaleValue = "3 3 3";
                        }
                        // If this is an impact point, set the scale = laneNumber * laneWidth
                        if (impactedCoords.contains(currentVehiclePath.get(i)))
                        {
                            scaleValue = "";
                            for (int j = 0; j < 3; j++)
                                scaleValue += laneNumber * AccidentParam.laneWidth / 2 + " ";
                        }
                        waypointInfoStr = waypointInfoStr.replace("$coord", currentVehiclePath.get(i));
                        waypointInfoStr = waypointInfoStr.replace("$scale", scaleValue.trim());


                        waypointListStrBuilder.append(waypointInfoStr);


                    }

                    // Add the waypoint list into the current vehicle waypointNodeNameList
                    currentVehicle.setWaypointPathNodeName(waypointPathLuaStrBuilder.
                            deleteCharAt(waypointPathLuaStrBuilder.length() - 1).toString());
                }
                else if (currentVehiclePath.size() == 1 || currentVehicle.getOnStreet() < 1) // if this is a parked car
                {
                    ConsoleLogger.print('d',"Construct parked vehicle vehicle#" + currentVehicle.getVehicleId());
                    vehicleListStrBuilder = constructLaneFilledOfParkedCar(currentVehiclePath, currentStreet,
                            currentVehicle, laneNumber, vehicleTemplate, vehicleListStrBuilder);
                }

            } // End constructing Vehicle List and Waypoint list

            // Generate Lua File for AI Waypoints Follower

            List<String> luaAIFollowPathTemplateList = Files.readAllLines(luaPathFollowerFilePath, Charset.defaultCharset());

            for (String luaAITemplateLine : luaAIFollowPathTemplateList)
            {
                luaPathStrBuilderTemplate.append(luaAITemplateLine + "\n");
            }

            String luaAITemplate = luaPathStrBuilderTemplate.toString();

            // Generate Lua AI Waypoints Follower Config
            List<String> luaAIConfigTemplateList = Files.readAllLines(luaAIPathFollowerConfigFilePath, Charset.defaultCharset());

            for (String luaAIConfigLine : luaAIConfigTemplateList)
            {
                luaAIConfigStrBuilderTemplate.append(luaAIConfigLine + "\n");
            }

            // Construct the config for each vehicle
            StringBuilder allAIConfigStr = new StringBuilder();
            for (VehicleAttr currentVehicle : vehicleList)
            {
                // Only configure AI Waypoint Follower if the car is moving
                if (currentVehicle.getMovementPath().size() > 1)
                {
                    if (currentVehicle.getOnStreet() >= 1) {
                        String luaAIConfigTemplate = luaAIConfigStrBuilderTemplate.toString();
                        luaAIConfigTemplate = luaAIConfigTemplate.replace("$waypointNameList",
                                currentVehicle.getWaypointPathNodeName());

                        luaAIConfigTemplate = luaAIConfigTemplate.replace("$speed", (currentVehicle.getVelocity() / 2) + "");
                        luaAIConfigTemplate = luaAIConfigTemplate.replace("$actorID", currentVehicle.getVehicleId() + "");
                        allAIConfigStr.append(luaAIConfigTemplate + "\n\n");
                    }
                }
            }

            // If we have a sideswipe, construct the "wait and crash logic for parked car"
            if (testCaseInfo.getCrashType().toLowerCase().contains("sideswipe"))
            {
                // If there are 2 vehicles, construct the ID and waypoints based on the vehicle
                if (vehicleList.size() == 2)
                {
                    VehicleAttr strikerVehicle = null;
                    VehicleAttr victimVehicle = null;

                    for (VehicleAttr vehicleAttr : vehicleList)
                    {
                        if (vehicleAttr.getVehicleId() == 1 && vehicleAttr.getOnStreet() >= 1)
                        {
                            strikerVehicle = vehicleAttr;
                        }
                        else if (vehicleAttr.getVehicleId() == 2 && vehicleAttr.getOnStreet() < 1)
                        {
                            victimVehicle = vehicleAttr;
                        }
                    }

                    String carLeaveTriggerTemplate = loadTemplateFileContent(luaAILeaveTriggerPath);

                    carLeaveTriggerTemplate = carLeaveTriggerTemplate
                            .replace("$P2ID", victimVehicle.getVehicleId() + "")
                            .replace("$wpList", victimVehicle.getWaypointPathNodeName())
                            .replace("$speed", AccidentParam.defaultSpeed / 2 + "")
                            .replace("$P1ID", strikerVehicle.getVehicleId() + "")
                            // TODO: COmpute Trigger Distance using equation
                            .replace("$triggerDistance", (victimVehicle.getLeaveTriggerDistance()) + "")
                            .replace("$collisionDistance", AccidentParam.DISTANCE_BETWEEN_CARS + "");

                    ConsoleLogger.print('d',"Construct moving car logic for sideswipe, trigger distance " + victimVehicle.getLeaveTriggerDistance());

                    luaAITemplate = luaAITemplate.replace("--$OtherVehicleStartToRunCode", carLeaveTriggerTemplate);

                } /// End processing 2 vehicles case
            } // End processing sideswipe case

            // Add the AI config into the AI Lua template
            luaAITemplate = luaAITemplate.replace("$setAiMovementPath", allAIConfigStr.toString());
            ConsoleLogger.print('d',"Scenario COnfig path " + AccidentParam.scenarioConfigFilePath);
            ConsoleLogger.print('d',"Lua AI Template \n" + luaAITemplate);

            Path scenarioConfigPath = Paths.get(AccidentParam.scenarioConfigFilePath + "\\" + scenarioName + ".lua");
            Files.write(scenarioConfigPath, luaAITemplate.getBytes());

//            ConsoleLogger.print('d',"Vehicle List \n" + vehicleListStrBuilder.toString());
//            ConsoleLogger.print('d',"Waypoint List \n" + waypointListStrBuilder.toString());
            return vehicleListStrBuilder.toString() + "\n\n"
                    + waypointListStrBuilder.toString() + "\n\n";
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            ConsoleLogger.print('d',"Exception in Vehicles And Waypoints constructions " + ex);
            return "Error in Vehicles and Waypoints Construction " + ex ;
        }
    }

    private String constructWaypointsAndVehicles(String scenarioName)
    {
        Path waypointFilePath = Paths.get(AccidentParam.waypointFilePath);
        Path vehicleFilePath = Paths.get(AccidentParam.vehicleFilePath);
        Path luaPathFollowerFilePath = Paths.get(AccidentParam.luaAIFilePath);
        Path luaAIPathFollowerConfigFilePath = Paths.get(AccidentParam.luaAIConfigFilePath);
        Path luaAILeaveTriggerPath = Paths.get(AccidentParam.luaAICarLeaveTriggerFilePath);

        Set<String> impactedCoords = new HashSet<String>();

        StringBuilder waypointStrBuilderTemplate = new StringBuilder();
        StringBuilder waypointListStrBuilder = new StringBuilder();
        StringBuilder vehicleStrBuilderTemplate = new StringBuilder();
        StringBuilder vehicleListStrBuilder = new StringBuilder();
        StringBuilder luaPathStrBuilderTemplate = new StringBuilder();
        StringBuilder luaAIConfigStrBuilderTemplate = new StringBuilder();
        StringBuilder luaAILeaveTriggerStrBuilderTemplate = new StringBuilder();



        try {
            // Load waypoint template and convert to String
            List<String> waypointFileContent = Files.readAllLines(waypointFilePath, Charset.defaultCharset());

            for (int i = 0; i < waypointFileContent.size(); i++) {
                waypointStrBuilderTemplate.append(waypointFileContent.get(i) + "\n");
            }

            String waypointTemplate = waypointStrBuilderTemplate.toString();

            // Load vehicle template and convert to String
            List<String> vehicleFileContent = Files.readAllLines(vehicleFilePath, Charset.defaultCharset());

            for (int i = 0; i < vehicleFileContent.size(); i++) {
                vehicleStrBuilderTemplate.append(vehicleFileContent.get(i) + "\n");
            }

            String vehicleTemplate = vehicleStrBuilderTemplate.toString();

            for (VehicleAttr currentVehicle : vehicleList)
            {
                ArrayList<String> currentVehiclePath = currentVehicle.getMovementPath();

                // Convert default delimiter of coords in vehicle's trajectory to beamng format, this step shows that
                // AC3R can convert the default coord format to the one of 3D rendering software.
                for (int k = 0; k < currentVehiclePath.size(); k++)
                {
                    String beamngConvertedCoord = convertCoordToBeamNGFormat(currentVehiclePath.get(k));
                    currentVehiclePath.set(k, beamngConvertedCoord);
                }

                Street currentStreet = currentVehicle.getStandingStreet();

                double radius = Double.parseDouble(currentStreet.getStreetPropertyValue("curve_radius"));

                int laneNumber = Integer.parseInt(currentStreet.getStreetPropertyValue("lane_num"));

                ConsoleLogger.print('d',"Construct vehicle obj for vehicle#" + currentVehicle.getVehicleId());
                vehicleListStrBuilder = constructVehicleObject("" + currentVehicle.getVehicleId(),
                        currentVehiclePath.get(0), currentVehicle.getColor(), currentVehicle.getVehicleType(),
                        currentVehicle.getPartConfig(),"1", vehicleListStrBuilder);

                // Construct the waypoints for mobile car
                if (currentVehiclePath.size() > 1)
                {
                    // Construct waypoint obj lists
                    StringBuilder waypointPathLuaStrBuilder = new StringBuilder();

                    for (int i = 1; i < currentVehiclePath.size(); i++) {
                        String waypointName = "wp" + i + "_v" + currentVehicle.getVehicleId();
                        String waypointInfoStr = waypointTemplate.replace("$name", waypointName); // wp[index]_[carID]

                        waypointPathLuaStrBuilder.append("\'" + waypointName + "\',");

                        String scaleValue = "1 1 1";

                        waypointInfoStr = waypointInfoStr.replace("$coord", currentVehiclePath.get(i));
                        waypointInfoStr = waypointInfoStr.replace("$scale", scaleValue.trim());

                        waypointListStrBuilder.append(waypointInfoStr);
                    }

                    // Construct the vehicle path that goes through these wps
                    vehicleListStrBuilder.append(constructInvisibleTrajectory(currentVehicle.getVehicleId(),
                            currentVehiclePath.get(0), constructRoadNodeString(currentVehiclePath, AccidentParam.laneWidth)));


                    // Add the waypoint list into the current vehicle waypointNodeNameList
                    currentVehicle.setWaypointPathNodeName(waypointPathLuaStrBuilder.
                            deleteCharAt(waypointPathLuaStrBuilder.length() - 1).toString());
                }
                // if this is a parked car, check if we need to construct a lane filled of parked cars
                else if ((currentVehiclePath.size() == 1 || currentVehicle.getOnStreet() < 1))
                {
                    if (!currentStreet.getStreetPropertyValue("road_park_line_fill").equals(""))
                    {
                        ConsoleLogger.print('d', "Construct parked vehicle vehicle#" + currentVehicle.getVehicleId());
                        vehicleListStrBuilder.append(constructLaneFilledOfParkedCar(currentVehiclePath, currentStreet,
                                currentVehicle, laneNumber, vehicleTemplate, vehicleListStrBuilder));
                    }
                }

            } // End constructing Vehicle List and Waypoint list

            // Generate Lua File for AI Waypoints Follower
            List<String> luaAIFollowPathTemplateList = Files.readAllLines(luaPathFollowerFilePath, Charset.defaultCharset());

            for (String luaAITemplateLine : luaAIFollowPathTemplateList)
            {
                luaPathStrBuilderTemplate.append(luaAITemplateLine + "\n");
            }

            String luaAITemplate = luaPathStrBuilderTemplate.toString();

            // Generate Lua AI Waypoints Follower Config
            List<String> luaAIConfigTemplateList = Files.readAllLines(luaAIPathFollowerConfigFilePath, Charset.defaultCharset());

            for (String luaAIConfigLine : luaAIConfigTemplateList)
            {
                luaAIConfigStrBuilderTemplate.append(luaAIConfigLine + "\n");
            }

            // Construct the config for each vehicle
            StringBuilder allAIConfigStr = new StringBuilder();
            for (VehicleAttr currentVehicle : vehicleList)
            {
                // Only configure AI Waypoint Follower if the car is moving
                if (currentVehicle.getMovementPath().size() > 1)
                {
                    if (currentVehicle.getOnStreet() >= 1) {
                        String luaAIConfigTemplate = luaAIConfigStrBuilderTemplate.toString();
                        luaAIConfigTemplate = luaAIConfigTemplate.replace("$waypointNameList",
                                currentVehicle.getWaypointPathNodeName());

                        luaAIConfigTemplate = luaAIConfigTemplate.replace("$speed", (currentVehicle.getVelocity() / 2) + "");
                        luaAIConfigTemplate = luaAIConfigTemplate.replace("$actorID", currentVehicle.getVehicleId() + "");
                        allAIConfigStr.append(luaAIConfigTemplate + "\n\n");
                    }
                }
            }

            // If trigger distance is required to create a crash, create a trigger function in the scenario's Lua file
            if (testCaseInfo.getEnvPropertyValue("need_trigger").equals("T"))
            {
                // If there are 2 vehicles, construct the ID and waypoints based on the vehicle
                if (vehicleList.size() == 2) {
                    VehicleAttr strikerVehicle = null;
                    VehicleAttr victimVehicle = null;

                    for (int i = 0; i < vehicleList.size(); i++) {
                        // Select the first car moving on the road as striker, and other as victim
                        VehicleAttr currVehicle = vehicleList.get(i);
                        if (currVehicle.getVehicleId() == 1 && currVehicle.getOnStreet() >= 1) {
                            strikerVehicle = currVehicle;
                            victimVehicle = vehicleList.get(vehicleList.size() - 1 - i);
                        }
    //                        else if (vehicleAttr.getVehicleId() == 2 && vehicleAttr.getOnStreet() < 1)
    //                        {
    //                            victimVehicle = vehicleAttr;
    //                        }
                    }

                    String carLeaveTriggerTemplate = loadTemplateFileContent(luaAILeaveTriggerPath);

                    carLeaveTriggerTemplate = carLeaveTriggerTemplate
                            .replace("$P2ID", victimVehicle.getVehicleId() + "")
                            .replace("$wpList", victimVehicle.getWaypointPathNodeName())
                            .replace("$speed", AccidentParam.defaultSpeed / 2 + "")
                            .replace("$P1ID", strikerVehicle.getVehicleId() + "")
                            // TODO: COmpute Trigger Distance using equation
                            .replace("$triggerDistance", (victimVehicle.getLeaveTriggerDistance()) + "")
                            .replace("$collisionDistance", AccidentParam.DISTANCE_BETWEEN_CARS + "");

                    ConsoleLogger.print('d', "Construct moving car logic for sideswipe, trigger distance " + victimVehicle.getLeaveTriggerDistance());

                    luaAITemplate = luaAITemplate.replace("--$OtherVehicleStartToRunCode", carLeaveTriggerTemplate);
                } // End processing 2 vehicles case
            } // End checking trigger distance

            // Add the AI config into the AI Lua template
            luaAITemplate = luaAITemplate.replace("$setAiMovementPath", allAIConfigStr.toString());
            ConsoleLogger.print('d',"Scenario COnfig path " + AccidentParam.scenarioConfigFilePath);
            ConsoleLogger.print('d',"Lua AI Template \n" + luaAITemplate);

            Path scenarioConfigPath = Paths.get(AccidentParam.scenarioConfigFilePath + "\\" + scenarioName + ".lua");
            Files.write(scenarioConfigPath, luaAITemplate.getBytes());

            ConsoleLogger.print('d',"Vehicle List \n" + vehicleListStrBuilder.toString());
            ConsoleLogger.print('d',"Waypoint List \n" + waypointListStrBuilder.toString());
            return vehicleListStrBuilder.toString() + "\n\n"
                    + waypointListStrBuilder.toString() + "\n\n";
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            ConsoleLogger.print('d',"Exception in Vehicles And Waypoints constructions " + ex);
            return "Error in Vehicles and Waypoints Construction " + ex ;
        }
    }

    private String convertCoordToBeamNGFormat(String xyCoord)
    {
        String[] coordElements = xyCoord.split(AccidentParam.defaultCoordDelimiter);

        String newCoord = "";

        if (coordElements.length == 2 || coordElements.length == 3)
        {
            newCoord = xyCoord.replace(AccidentParam.defaultCoordDelimiter, AccidentParam.beamngCoordDelimiter);;
        }
        else
        {
            newCoord = "invalidCoord";
        }

        return newCoord;
    }

    /*
     *  coordDimension : 0 = x
     *                   1 = y
     *                   2 = z
     */
    private String updateCoordElementAtDimension(int coordDimension, String baseCoord, String newValueAtCoordDimension)
    {
        String[] coordElements = baseCoord.split(" ");
        coordElements[coordDimension] = newValueAtCoordDimension;
        String updatedCoord = coordElements[0] + " " + coordElements[1] + " " + coordElements[2];
        return updatedCoord;
    }

    // Update the x, y, or z value for each coord in a given nodes list
    private void updateCoordElementForNodeList(ArrayList<String> baseNodeList, int[] xyzOffSet)
    {
        for (int n = 0; n < baseNodeList.size(); n++)
        {
            String[] coordElements = baseNodeList.get(n).split(" ");
            for (int i = 0; i < xyzOffSet.length; i++)
            {
                if (xyzOffSet[i] != 0)
                {
                    coordElements[i] = "" + (Double.parseDouble(coordElements[i]) + xyzOffSet[i]);
                }
            }

            baseNodeList.set(n, coordElements[0] + " " + coordElements[1] + " " + coordElements[2]);
        }
    }

    private ArrayList<String> constructNodeListFromNodeListStr(String nodeListKeyName,
                                                               Street currentStreet,
                                                               boolean removeWidth)
    {
        String[] nodeListStr = currentStreet.getStreetPropertyValue(nodeListKeyName).split(";");

        if (removeWidth)
        {
            for (int i = 0; i < nodeListStr.length; i++)
            {
                nodeListStr[i] = nodeListStr[i].substring(0, nodeListStr[i].lastIndexOf(" "));
            }
        }

        ArrayList<String> nodeListArrStr = new ArrayList<>(Arrays.asList(nodeListStr));

        return nodeListArrStr;
    }

    private String loadTemplateFileContent(Path filePath)
    {
        StringBuilder stringBuilder = new StringBuilder();
        List<String> fileContent = null;
        try {
            fileContent = Files.readAllLines(filePath, Charset.defaultCharset());
        } catch (IOException e) {
            ConsoleLogger.print('d',"Error at loading template file " + filePath.getFileName());
            e.printStackTrace();
        }

        for (int i = 0; i < fileContent.size(); i++) {
            stringBuilder.append(fileContent.get(i) + "\n");
        }

        return stringBuilder.toString();
    }

    private StringBuilder constructLaneFilledOfParkedCar(ArrayList<String> currentVehiclePath, Street currentStreet,
                                                         VehicleAttr currentVehicle, int laneNumber,
                                                         String vehicleTemplate, StringBuilder vehicleListStrBuilder)
    {
        String[] baseParkedCarElem = currentVehiclePath.get(0).split(" ");
        int distanceBetweenCars = 10;
        String parkLineOccupiedStat = currentStreet.getStreetPropertyValue("road_park_line");
        // Construct a line of parked cars if we have them
        if (!parkLineOccupiedStat.equals("0"))
        {
            // Construct the cars in left parked line
            if (parkLineOccupiedStat.equals("1") || parkLineOccupiedStat.equals("3"))
            {
                String basePos = currentVehiclePath.get(0);

                // Find the anchor position of the parked car, if the parked car is not parking in the left
                // lane then construct a symmetric car on the left lane
                if (currentVehicle.getStandingRoadSide().equals("right"))
                {
                    basePos = updateCoordElementAtDimension(1, basePos,
                            "" + (Double.parseDouble(baseParkedCarElem[1]) +
                                    laneNumber * AccidentParam.laneWidth + AccidentParam.parkingLineWidth / 2));
                }

                // Construct parked car before and after the anchor car
                for (int c = 1; c < 2 ; c++)
                {
                    // Construct car ahead of anchor car
                    String updatedXPos = updateCoordElementAtDimension(0, basePos,
                            (Double.parseDouble(baseParkedCarElem[0]) - c * distanceBetweenCars - 2) + "") ;

                    vehicleListStrBuilder = constructVehicleObject("leftLineParkedCarF" + c, updatedXPos,
                            "0 0 0", currentVehicle.getVehicleType(), currentVehicle.getPartConfig(),
                            "1", vehicleListStrBuilder);

                    // Construct car behind anchor car

                    updatedXPos = updateCoordElementAtDimension(0, basePos,
                            (Double.parseDouble(baseParkedCarElem[0]) + c * distanceBetweenCars + 2) + "") ;

                    vehicleListStrBuilder = constructVehicleObject("leftLineParkedCarB" + c, updatedXPos,
                            "0 0 0", currentVehicle.getVehicleType(), currentVehicle.getPartConfig(),
                            "1", vehicleListStrBuilder);
                }

            } // End constructing filled parked cars on left lane

            // Construct the cars in right parked line
            if (parkLineOccupiedStat.equals("2") || parkLineOccupiedStat.equals("3"))
            {
                String basePos = currentVehiclePath.get(0);

                // Find the anchor position of the parked car, if the parked car is not parking in the right
                // lane then construct a symmetric car on the left lane
                if (currentVehicle.getStandingRoadSide().equals("left"))
                {
                    basePos = updateCoordElementAtDimension(1, basePos,
                            "" + (Double.parseDouble(baseParkedCarElem[1]) -
                                    laneNumber * AccidentParam.laneWidth - AccidentParam.parkingLineWidth / 2));
                    ConsoleLogger.print('d',"Update Base POS");
                }

                // Construct parked car before and after the anchor car
                for (int c = 1; c < 2; c++)
                {
//                    String parkedVehicleInfoStr = vehicleTemplate.replace("$actorID",
//                            "rightLineParkedCarF" + c);

                    basePos = updateCoordElementAtDimension(1, basePos,
                            (Double.parseDouble(basePos.split(" ")[1]) - 1) + "") ;

                    String updatedXPos = updateCoordElementAtDimension(0, basePos,
                            (Double.parseDouble(baseParkedCarElem[0]) - c * distanceBetweenCars - 2) + "") ;


                    vehicleListStrBuilder = constructVehicleObject("rightLineParkedCarF" + c, updatedXPos,
                            "0 0 0", currentVehicle.getVehicleType(), currentVehicle.getPartConfig(),
                            "1", vehicleListStrBuilder);

//                    parkedVehicleInfoStr = vehicleTemplate.replace("$actorID",
//                            "rightLineParkedCarB" + c);

                    updatedXPos = updateCoordElementAtDimension(0, basePos,
                            (Double.parseDouble(baseParkedCarElem[0]) + c * distanceBetweenCars + 2) + "") ;

//                    parkedVehicleInfoStr = parkedVehicleInfoStr.replace("$position", updatedXPos);
//                    parkedVehicleInfoStr = parkedVehicleInfoStr.replace("$colorCode", "0 0 0");
//                    parkedVehicleInfoStr = parkedVehicleInfoStr.replace("$jbeam", currentVehicle.getVehicleType());
//                    parkedVehicleInfoStr = parkedVehicleInfoStr.replace("$partConfig", currentVehicle.getPartConfig());
//                    parkedVehicleInfoStr = parkedVehicleInfoStr.replace("$isAIControlled", "0");
//                    vehicleListStrBuilder.append(parkedVehicleInfoStr);

                    vehicleListStrBuilder = constructVehicleObject("rightLineParkedCarB" + c, updatedXPos,
                            "0 0 0", currentVehicle.getVehicleType(), currentVehicle.getPartConfig(),
                            "1", vehicleListStrBuilder);
                }

            } // End constructing filled parked cars on left lane
        }
        return vehicleListStrBuilder;
    }

    private String constructRoadObject(String laneName, String priority, Street roadObj, String initPosition,
                                       String drivable, String roadCoordListStr)
    {
        String roadTemplate = loadTemplateFileContent(Paths.get(AccidentParam.roadFilePath));
        String material = null;

        // Construct road texture
        ConsoleLogger.print('d',"Construct Road Component " + laneName);
        if (!laneName.contains("pavement") && !laneName.contains("roadDivision")) {
            // Find road material based on road type
            AccidentConcept roadConcept = ontologyHandler.findExactConcept(roadObj.getStreetPropertyValue("road_type"));
            if (roadConcept != null) {
                ConsoleLogger.print('d',"Get Texture of road concept " + roadObj.getStreetPropertyValue("road_type"));
                if (roadConcept.getDataProperties() != null) {
                    material = roadConcept.getDataProperties().get("texture");
                    ConsoleLogger.print('d',"Selected Street Material " + material);
                }

                // If the road does not contain a dedicated texture, apply default texture based on road material
                if (material == null) {
                    ConsoleLogger.print('d',"Road does not have dedicated texture, applies material texture of " +
                            roadObj.getStreetPropertyValue("road_material"));
                    AccidentConcept roadMaterialConcept = ontologyHandler.findExactConcept
                            (roadObj.getStreetPropertyValue("road_material"));

                    if (roadMaterialConcept != null) {
                        if (roadMaterialConcept.getDataProperties() != null)
                            material = roadMaterialConcept.getDataProperties().get("texture");
                    }
                }


            }
            // Even road material does not have texture, applies asphalt material
            if (material == null) {
                ConsoleLogger.print('d',"No texture found, applies default asphalt material");
                material = AccidentParam.asphaltMaterial;
            }

        }
        else
        {
            if (laneName.contains("pavement"))
            {
                AccidentConcept pavementConcept = ontologyHandler.findExactConcept
                        (roadObj.getStreetPropertyValue("pavement_type"));
                try {
                    if (pavementConcept != null) {
                        ConsoleLogger.print('d',"Get Texture of pavement concept " + roadObj.getStreetPropertyValue("pavement_type"));
                        if (pavementConcept.getDataProperties() != null)
                            material = pavementConcept.getDataProperties().get("texture");
                    }
                }
                catch (Exception ex)
                {
                    ConsoleLogger.print('e',"Exception at finding pavement texture \n" + ex);
                }
                // Applies default pavement texture if no specific texture is found
                if (material == null)
                {
                    ConsoleLogger.print('d',"No texture found, applies default pavement material");
                    material = AccidentParam.pavementMaterial;
                }
            }
            else
            {
                material = AccidentParam.laneDivisionMaterial;
            }
        }

        String roadObjectStr = roadTemplate
            .replace("$laneName", laneName)
            .replace("$priority", priority)
            .replace("$material", material)
            .replace("$initCoord", initPosition)
            .replace("$drivable", drivable)
            .replace("$nodeList", roadCoordListStr);
        return roadObjectStr;
    }

    private String constructInvisibleTrajectory(int vehicleId, String initPosition, String roadCoordListStr)
    {
        String roadTemplate = loadTemplateFileContent(Paths.get(AccidentParam.roadFilePath));
        String roadObjectStr = roadTemplate
                .replace("$laneName", String.format("v%d_trajectory", vehicleId))
                .replace("$priority", "" + 10)
                .replace("$material", "road_invisible")
                .replace("$initCoord", initPosition)
                .replace("$drivable", "1")
                .replace("$nodeList", roadCoordListStr);
        return roadObjectStr;
    }

    // Construct a text of a vehicle model in BeamNG, then append the vehicle info into the given StringBuilder
    private StringBuilder constructVehicleObject(String actorID, String position, String colorCode, String jBeamModelName,
                                          String partConfigName, String isAIControlled, StringBuilder vehicleListStrBuilder)
    {
        
        String vehicleTemplate = loadTemplateFileContent(Paths.get(AccidentParam.vehicleFilePath));

        // If roadGrade is not concerned, construct the car with z = 0

//        if (!AccidentParam.isGradingConcerned)
//        {
//            position = updateCoordElementAtDimension(2, position, "0");
//        }

        String vehicleInfoStr = vehicleTemplate.replace("$actorID", actorID)
            .replace("$position", position)
            .replace("$colorCode", colorCode)
            .replace("$jbeam", jBeamModelName)
            .replace("$partConfig", partConfigName)
            .replace("$isAIControlled", isAIControlled);

        VehicleAttr vehicle = null;

        String travelDirection = "";

        // If a mainVehicleID is given, use it to find travelling direction
        if (AccidentConstructorUtil.isNumeric(actorID))
        {
            vehicle = AccidentConstructorUtil.findVehicleBasedOnId(Integer.parseInt(actorID), vehicleList);
            travelDirection = vehicle.getTravellingDirection();
        }
        else
        {
            // If there is only 1 road, turn all other cars to the east
//            ConsoleLogger.print('d',"ActorID for parkedCar " + actorID );
            if (testCaseInfo.getStreetList().size() == 1)
            {
                if (actorID.contains("ParkedCar"))
                {
                    ConsoleLogger.print('d',"Set " + actorID + " to head east");
                    travelDirection = "E";
                }
            }

        }


        ConsoleLogger.print('d',"Travelling direction in Veh " + actorID + " Construction : " + travelDirection );

        if (travelDirection.equals("W"))
        {
            vehicleInfoStr = vehicleInfoStr.replace("$rotationMat", AccidentParam.headWest);
        }
        else if (travelDirection.equals("N"))
        {
            vehicleInfoStr = vehicleInfoStr.replace("$rotationMat", AccidentParam.headNorth);
        }
        else if (travelDirection.equals("E"))
        {
            vehicleInfoStr = vehicleInfoStr.replace("$rotationMat", AccidentParam.headEast);
        }
        else if (travelDirection.equals("S"))
        {
            vehicleInfoStr = vehicleInfoStr.replace("$rotationMat", AccidentParam.headSouth);
        }
        else if (travelDirection.equals("SW"))
        {
            vehicleInfoStr = vehicleInfoStr.replace("$rotationMat", AccidentParam.headSouthWest);
        }

        vehicleListStrBuilder.append(vehicleInfoStr);

        ConsoleLogger.print('d',"VehicleListStrBuilder \n" + vehicleListStrBuilder.toString());

        return vehicleListStrBuilder;
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

    private String adjustParkingPostion(String baseCoord, Street currentStreet,
                                      double radius, int laneNumber, VehicleAttr vehicleAttr)
    {
        String newYCoord = "";
        if (radius == 0.0) {

            double parkingLineExtraDistance = 0;

            if (!currentStreet.getStreetPropertyValue("road_park_line").equals("0")
                    && !currentStreet.getStreetPropertyValue("road_park_line").equals("") )
            {
                ConsoleLogger.print('d',"Parking Line extra distance set to 2");
                parkingLineExtraDistance = 2;
            }

            if (vehicleAttr.getStandingRoadSide().equals("left")) {
                newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(baseCoord.split(" ")[1]) +
                        (int) (laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth / 2) - 0.5 + parkingLineExtraDistance);
            } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
                newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(baseCoord.split(" ")[1]) -
                        (int) (laneNumber / 2.0 * AccidentParam.laneWidth - AccidentParam.parkingLineWidth / 2) + 0.5 - parkingLineExtraDistance);

            }

        } else {
            if (vehicleAttr.getStandingRoadSide().equals("left")) {
                newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(baseCoord.split(" ")[1]) +
                        (laneNumber / 2.0 * AccidentParam.laneWidth + AccidentParam.parkingLineWidth + 1) + 1);
            } else if (vehicleAttr.getStandingRoadSide().equals("right")) {
                newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(baseCoord.split(" ")[1]) -
                        (laneNumber / 2.0 * AccidentParam.laneWidth - AccidentParam.parkingLineWidth - 1) - 1);
            }
//                            newYCoord = AccidentParam.df6Digit.format(Double.parseDouble(convertedCoord.split(" ")[1]) -
//                                    (laneNumber / 2 * AccidentParam.laneWidth - 1))  ;
        }

        return newYCoord;
    }

    private String constructRoadNodeString(ArrayList<String> pathCoordList, double laneWidth)
    {
        StringBuilder nodeListStr = new StringBuilder();

//        for (String roadNode : pathCoordList) {
//            nodeListStr.append("\t\tNode = \"" + roadNode + "\";\n");
//        }

        nodeListStr.append(appendWidthToNodeList(laneWidth, pathCoordList));

        nodeListStr.append("\n");
        return nodeListStr.toString();
    }

}