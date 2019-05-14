package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.Street;
import org.saarland.accidentelementmodel.TestCaseInfo;
import org.saarland.configparam.AccidentParam;
import org.saarland.ontologyparser.OntologyHandler;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class EnvironmentConstructor {
    TestCaseInfo testCase;
    OntologyHandler parser;

    public EnvironmentConstructor(TestCaseInfo testCaseInfo, OntologyHandler parser)
    {
        this.parser = parser;
        testCase = testCaseInfo;
    }

    public String contructEnvironmentObjects()
    {
        HashMap<String, String> testCaseProp = testCase.getTestCaseProp();

        StringBuilder environmentPropObjStrBuilder = new StringBuilder();

        // Cons


        // Construct Sky object
        try {
            String skyTemplate = AccidentConstructorUtil.loadTemplateContent(AccidentParam.skyFilePath);

            double brightness = 1;

            String[] lightingInfo = testCaseProp.get("lighting").split(" ");

            // Extract the lighting info in the test case
            for (String lightingProp : lightingInfo)
            {
                if (lightingProp.equals(""))
                {
                    continue;
                }
                else
                {
                    ConsoleLogger.print('d',"Lighting Prop found " + lightingProp);
                    // If light is not set and the environment is lighted, then set brightness to 0.5
                    if (lightingProp.equals("light"))
                    {
                        environmentPropObjStrBuilder.append(constructLightPoles() + "\n\n");
//                        ConsoleLogger.print('d',"Light pole prefab " + environmentPropObjStrBuilder.toString());
                        if (brightness == -10)
                        {
                            brightness = 0.5;
                        }
                    }
                    else if (lightingProp.equals("normal"))
                    {
                        brightness = 0.8;
                    }

                    else {
                        // If the environment is dark and no lighting is specified, assume light presents
                        if (testCaseProp.get("lighting").contains("dark") &&
                                !testCaseProp.get("lighting").contains("n_light"))
                        {
                            brightness = Double.parseDouble(parser.findExactConcept(lightingProp).
                                    getDataProperties().get("lighting_brightness"));
                            ConsoleLogger.print('d',"Found brightness value is " + brightness);
                            ConsoleLogger.print('d',"Construct dark unspecified lighting scenario");
                            environmentPropObjStrBuilder.append(constructLightPoles() + "\n\n");
                        }

                        else if (lightingProp.equals("n_light"))
                        {
                            brightness = 0.1;
                        }
                    }
                } // End processing lighting props
            } // End looping through lighting info

            skyTemplate = skyTemplate.replace("$brightness", brightness + "");
            skyTemplate = skyTemplate.replace("$skyBrightness", brightness * 300 + "");
            environmentPropObjStrBuilder.append(skyTemplate);
        } // End try constructing lighting env prop
        catch (Exception ex)
        {
            ConsoleLogger.print('e',"Error in constructing Sky object! \n");
            ex.printStackTrace();
        }
        // Construct weather conditions
        try
        {
            String[] weatherEffects = testCase.getEnvPropertyValue("weather").split(" ");

            for (String effect : weatherEffects)
            {
                if (effect.equals(""))
                {
                    continue;
                }
                String weatherTemplate = "";
                if (effect.contains("rain"))
                {
                    ConsoleLogger.print('d',"Construct Rainy Environment");
                    weatherTemplate = AccidentConstructorUtil.loadTemplateContent(AccidentParam.rainyFilePath);

                }
                else if (effect.startsWith("cloud"))
                {
                    ConsoleLogger.print('d',"Construct Cloudy Environment");
                    weatherTemplate = AccidentConstructorUtil.loadTemplateContent(AccidentParam.cloudFilePath);
                }
                environmentPropObjStrBuilder.append(weatherTemplate);
            }

        }
        catch (Exception ex)
        {
            ConsoleLogger.print('e',"Error in constructing weather condition effect \n");
            ex.printStackTrace();
        }

        // Construct Road Sign
        try {
            environmentPropObjStrBuilder.append(constructTrafficSign());
        } catch (Exception ex) {
            ConsoleLogger.print('e',"Error in contructing road sign ");
            ex.printStackTrace();
        }

        return environmentPropObjStrBuilder.toString();
    }

    private String constructLightPoles()
    {
        StringBuilder lightPolesPrefabStr = new StringBuilder();

        String lightPoleTemplate = "";
        String pointLightTemplate = "";

        int lightPoleCounter = 0;

        try
        {
            lightPoleTemplate = AccidentConstructorUtil.loadTemplateContent(AccidentParam.lampFilePath);
            pointLightTemplate = AccidentConstructorUtil.loadTemplateContent(AccidentParam.pointLightFilePath);
        }
        catch (IOException ex)
        {
            ConsoleLogger.print('e',"Error in constructing light poles and light points prefab \n");
            ex.printStackTrace();
        }


        ArrayList<Street> streetList = testCase.getStreetList();

        // If there are 2 streets, there is a junction, try to avoid that

//        if (streetList.size() >= 2)
//        {
        for (int i = 0; i < streetList.size(); i++)
        {
            Street currentStreet = streetList.get(i);

            String[] roadNodeList = currentStreet.getStreetPropertyValue("road_node_list").split(";");

            // Construct light poles on 2 sides along the street

            for (int n = 0; n < roadNodeList.length; n++)
            {

                String[] roadNodeCoord = roadNodeList[n].split(" ");

                double nodeXCoord = Double.parseDouble(roadNodeCoord[0]);
                double nodeYCoord = Double.parseDouble(roadNodeCoord[1]);

                // No grading concern => z = 0
                if (!AccidentParam.isGradingConcerned)
                {
                    roadNodeCoord[2] = "0";
                }

                for (int s = -1; s < 1; s += 2)
                {
                    ConsoleLogger.print('d',"Process Street #" + currentStreet.getStreetPropertyValue("road_ID") + " node " + roadNodeList[n]);
                    // If abs(x) > abs(y), this road span mostly horizontally, adjust the y axis and add poles
                    if (Math.abs(nodeXCoord) >= Math.abs(nodeYCoord) )
                    {
                        if (testCase.getStreetList().size() >= 2 && Math.abs(nodeXCoord) < 30)
                        {
                            continue;
                        }
                        // Adjust the light position a bit so that the 2 lines of light poles won't be parallel

                        String currentLampCoord = nodeXCoord + " " +
                                (nodeYCoord + 3 * s + s * Integer.parseInt(currentStreet.getStreetPropertyValue("lane_num")) * AccidentParam.laneWidth)
                                + " " + roadNodeCoord[2];

//                        if (s == 1)
//                        {
//                            currentLampCoord = AccidentConstructorUtil.updateCoordElementAtDimension(0,
//                                    currentLampCoord + "", nodeXCoord - 20 + "");
//                        }

                        lightPolesPrefabStr.append(
                                lightPoleTemplate.replace("$pos", currentLampCoord)
                                                 .replace("$ID", lightPoleCounter + "") + "\n\n");

                        // Raise the lamp zCoord for +10 to place the point light
                        currentLampCoord = AccidentConstructorUtil.updateCoordElementAtDimension(2, currentLampCoord,
                                Double.parseDouble(roadNodeCoord[2]) + 15 + "", AccidentParam.beamngCoordDelimiter);

                        ConsoleLogger.print('d',"Add light point " + currentLampCoord);

                        lightPolesPrefabStr.append(
                                pointLightTemplate.replace("$pos", currentLampCoord)
                                        .replace("$ID", lightPoleCounter++ + "") + "\n\n");
                    }
                    // If abs(x) < abs(y), this road span mostly vertically, add poles to the x axis
                    else if (Math.abs(nodeXCoord) < Math.abs(nodeYCoord) && Math.abs(nodeYCoord) > 30)
                    {
                        String currentLampCoord =
                                (nodeXCoord + 3 * s + s * Integer.parseInt(currentStreet.getStreetPropertyValue("lane_num")) * AccidentParam.laneWidth)
                                + " " + nodeYCoord + " " + roadNodeCoord[2];
//
//                        if (s == 1)
//                        {
//                            currentLampCoord = AccidentConstructorUtil.updateCoordElementAtDimension(1,
//                                    currentLampCoord + "", nodeYCoord - 20 + "");
//                        }

                        lightPolesPrefabStr.append(
                                lightPoleTemplate.replace("$pos", currentLampCoord)
                                        .replace("$ID", lightPoleCounter + "") + "\n\n");

                        // Raise the lamp zCoord for +10 to place the point light
                        currentLampCoord = AccidentConstructorUtil.updateCoordElementAtDimension(2, currentLampCoord,
                                Double.parseDouble(roadNodeCoord[2]) + 15 + "", AccidentParam.beamngCoordDelimiter);

                        ConsoleLogger.print('d',"Add light point " + currentLampCoord);

                        lightPolesPrefabStr.append(
                                pointLightTemplate.replace("$pos", currentLampCoord)
                                        .replace("$ID", lightPoleCounter++ + "") + "\n\n");
                    }
                }
            } // End constructing poles on 2 sides of the road
//            }


        }
        return lightPolesPrefabStr.toString() + "\n\n";
    }

    private String constructTrafficSign()
    {
        ArrayList<Street> streetList = testCase.getStreetList();
        String roadSignObjStr = "";
        int signID = 1;
        for (int i = 0; i < streetList.size(); i++)
        {
            Street currentStreet = streetList.get(i);
            String roadSignList = currentStreet.getStreetPropertyValue("traffic_sign_list");

            // Construct all known road signs
            if (!roadSignList.equals("") || roadSignList != null)
            {
                ConsoleLogger.print('d',String.format("Road sign list for Street %s is %s ",
                        currentStreet.getStreetPropertyValue("road_ID"), roadSignList));

                // The distance from the center of intersection to the road sign
                double distanceToRoadSign = 30;
                double currentStreetWidth = AccidentParam.laneWidth *
                        Double.parseDouble(currentStreet.getStreetPropertyValue("lane_num"));
                // Construct stop sign

                    // Place the stop sign at a distance equal to the length of other road. Only in case there are 2 streets.
                Street otherStreet = null;

                if (streetList.size() == 2)
                {
                    otherStreet = streetList.get(streetList.size() - 1 - i);
                    distanceToRoadSign =
                            AccidentParam.laneWidth * Double.parseDouble(otherStreet.getStreetPropertyValue("lane_num"));
                }

                // If 2 legs of the intersection belongs to this road i.e. this road spans over the intersection, then
                // placing the 2 stop sign at both legs
                boolean isSingleRoadPiece = !currentStreet.getStreetPropertyValue("is_single_road_piece").equals("F");
                // Placing stop sign on the road
                for (String sign : roadSignList.split(";"))
                {
                    if (sign.equals("")) continue;

                    String roadSignTemplate = "";
                    try {
                        roadSignTemplate = AccidentConstructorUtil.loadTemplateContent(
                                AccidentParam.templateFilePath + "\\" + sign + ".txt");
                    } catch (Exception ex) {
                        ConsoleLogger.print('e',"Cannot find template for road sign " + sign);
                        ex.printStackTrace();
                        continue;
                    }
                    int currentRoadAngle = Integer.parseInt(currentStreet.getStreetPropertyValue("road_angle"));
                    double[] stopSignCoord = AccidentConstructorUtil.computeNewCoordOfRotatedLine(distanceToRoadSign,
                            currentRoadAngle);


                    ConsoleLogger.print('d',"Current Road Angle for Street " + currentStreet.getStreetPropertyValue("road_ID") + " is " + currentRoadAngle);
//                    ConsoleLogger.print('d',"stopSignCoord init " + stopSignCoord[0] + " " + stopSignCoord[1]);
                    // North road, put stop sign on the right with distance from the middle of the road is roadWidth/2 + 3
                    if (currentRoadAngle == 0)
                    {
                        double adjustedXCoord = 0 - (currentStreetWidth / 2 + 2);
                        //ConsoleLogger.print('d',"adjustedXCoord = " + adjustedXCoord);
                        String roadSignStr = roadSignTemplate.replace("$pos",
                                adjustedXCoord + " " + stopSignCoord[1] + " 0")
                                .replace("$rotationMatrix", AccidentParam.headSouth)
                                .replace("$ID", signID++ + "");

                        roadSignObjStr += roadSignStr + "\n\n";
                        ConsoleLogger.print('d',signID + " Construct road sign for " + sign + " at " + adjustedXCoord + " " + stopSignCoord[1] + " 0");
                        //signID++;
                    }

                    // Construct a stop sign at the opposite direction (opposite leg) for 2-way or non-specified way road
                    if (!isSingleRoadPiece
                            && (currentStreet.getStreetPropertyValue("road_direction").equals("2-way")
                            || currentStreet.getStreetPropertyValue("road_direction").equals("")))
                    {
                        int oppositeRoadAngle = currentRoadAngle + 180;
                        // if the opposite road angle is more than 360, cut it
                        if (oppositeRoadAngle > 360)
                        {
                            oppositeRoadAngle = oppositeRoadAngle - 360;
                        }

                        stopSignCoord = AccidentConstructorUtil.computeNewCoordOfRotatedLine(distanceToRoadSign,
                                oppositeRoadAngle);

                        ConsoleLogger.print('d',"Opposite stop sign coord " + stopSignCoord[0] + " " + stopSignCoord[1]);

                        if (oppositeRoadAngle == 180) // South direction
                        {
                            stopSignCoord[0] = 0 + (currentStreetWidth / 2 + 2);
                            ConsoleLogger.print('d',signID + " Construct road sign for " + sign + " at " + stopSignCoord[0] + " " + stopSignCoord[1] + " 0");

                            String roadSignStr = roadSignTemplate.replace("$pos",
                                    stopSignCoord[0] + " " + stopSignCoord[1] + " 0")
                                    .replace("$rotationMatrix", AccidentParam.headNorth)
                                    .replace("$ID", signID++ + "");
                            signID++;
                            roadSignObjStr += roadSignStr + "\n\n";

                        }

                    }
                }
            } // End checking if any road sign exists
        } // End looping through streets

        return roadSignObjStr;
    }

}
