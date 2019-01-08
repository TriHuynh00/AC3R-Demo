package org.saarland.accidentelementmodel;

import org.saarland.accidentconstructor.AccidentConstructorUtil;
import org.saarland.accidentconstructor.ConsoleLogger;
import org.saarland.configparam.AccidentParam;

import java.util.HashMap;
import java.util.Map;

/* 
 *  A Set of dictionary to specify the forward/backward and left/right coords in relation with the traveling direction 
 *  For each forward/backward/left/right direction, the value of xCoord and yCoord modification contains 2 components
 *  xCoord and yCoord modification, seperated by a ";" sign. 
 *  
 *  For example, the forward coord justification of the North traveling direction is "0;+", meaning by keeping the original
 *  xCoord unchanged, and increasing the original yCoord by some meters, we got a forward coord of North direction. 
 *  So if a coord is "5 12" (xCoord = 5 and yCoord = 12), making the car moving forward by 7 meters means the value 
 *  of the new coord is "5 19".
 *  
 *  The possible modification are:
 *  0 : no change
 *  + : increase xCoord / yCoord
 *  - : decrease xCoord / yCoord
 */

public class NavigationDictionary {
    public static HashMap<String, String> NorthNavDict = new HashMap<String, String>();

    public static HashMap<String, String> SouthNavDict = new HashMap<String, String>();

    public static HashMap<String, String> EastNavDict = new HashMap<String, String>();

    public static HashMap<String, String> WestNavDict = new HashMap<String, String>();

    public static HashMap<String, String> NorthEastNavDict = new HashMap<String, String>();

    public static HashMap<String, String> NorthWestNavDict = new HashMap<String, String>();

    public static HashMap<String, String> SouthEastNavDict = new HashMap<String, String>();

    public static HashMap<String, String> SouthWestNavDict = new HashMap<String, String>();

    public NavigationDictionary()
    {

    }

    public static void init()
    {
        NorthNavDict.put("forward", "0;+");
        NorthNavDict.put("backward", "0;-");
        NorthNavDict.put("left", "-;0");
        NorthNavDict.put("right", "+;0");

        SouthNavDict.put("forward", "0;-");
        SouthNavDict.put("backward", "0;+");
        SouthNavDict.put("left", "+;0");
        SouthNavDict.put("right", "-;0");

        WestNavDict.put("forward", "-;0");
        WestNavDict.put("backward", "+;0");
        WestNavDict.put("left", "0;-");
        WestNavDict.put("right", "0;+");

        EastNavDict.put("forward", "+;0");
        EastNavDict.put("backward", "-;0");
        EastNavDict.put("left", "0;+");
        EastNavDict.put("right", "0;-");

        NorthEastNavDict.put("forward", "+;+");
        NorthEastNavDict.put("backward", "-;-");
        NorthEastNavDict.put("left", "-;+");
        NorthEastNavDict.put("right", "+;-");

        NorthWestNavDict.put("forward", "-;+");
        NorthWestNavDict.put("backward", "+;-");
        NorthWestNavDict.put("left", "-;-");
        NorthWestNavDict.put("right", "+;+");

        SouthEastNavDict.put("forward", "+;-");
        SouthEastNavDict.put("backward", "-;+");
        SouthEastNavDict.put("left", "+;+");
        SouthEastNavDict.put("right", "-;-");

        SouthWestNavDict.put("forward", "-;-");
        SouthWestNavDict.put("backward", "+;+");
        SouthWestNavDict.put("left", "-;+");
        SouthWestNavDict.put("right", "+;-");
    }

    public static double setCoordValue(double value, String sign)
    {
        double adjustedValue = 0;
        ConsoleLogger.print('d', "sign is " + sign);
        switch (sign)
        {
            case "0":
                adjustedValue = 0;
                break;
            case "+": // Positive coord value
                adjustedValue = Math.abs(value);
                break;
            case "-": // Ensure a negative value is returned
                adjustedValue = Math.abs(value) * -1;
                break;
            default:
                adjustedValue = 0;
                break;
        }
        ConsoleLogger.print('d', "adjustedValue is " + adjustedValue);
        return adjustedValue;
    }

    // Construct an XYZ coord given the length of the segment, radius, and moving direction
    public static String createNESWCoordBasedOnNavigation(double segmentLength, double radius, String navigationDirection,
                                                          HashMap<String, String> navigationDict, String delimiter)
    {
        String[] directionCoordConfig = navigationDict.get(navigationDirection).split(";");
        double currXCoord = 0;
        double currYCoord = 0;

        // xCoord, yCoord value configuration
        currXCoord = setCoordValue(segmentLength, directionCoordConfig[0]);

        currYCoord = setCoordValue(segmentLength, directionCoordConfig[1]);

        // Compute the curved coord based on the road radius.
        if (radius != 0)
        {
            if (currXCoord != 0)
                currYCoord = AccidentConstructorUtil.computeXCircleFunc(radius, currXCoord);
            else
                currXCoord = AccidentConstructorUtil.computeXCircleFunc(radius, currYCoord);
        }

        ConsoleLogger.print('d', "currXCoord is " + currXCoord);
        ConsoleLogger.print('d', "currYCoord is " + currYCoord);

        String finalCoord = currXCoord + delimiter + currYCoord;

        // If grade is NOT considered, append 0 into the final coord
        if (!AccidentParam.isGradingConcerned)
            finalCoord += delimiter + "0";
        return finalCoord;
    }
    
    public static HashMap<String, String> selectDictionaryFromTravelingDirection(String roadCardinalDirection)
    {
        HashMap<String, String> navigationDict = null;
        switch (roadCardinalDirection) {
            case "N":
                navigationDict = NorthNavDict;
                break;
            case "S":
                navigationDict = SouthNavDict;
                break;
            case "E":
                navigationDict = EastNavDict;
                break;
            case "W":
                navigationDict = WestNavDict;
                break;
            case "NE":
                navigationDict = NorthEastNavDict;
                break;
            case "NW":
                navigationDict = NorthWestNavDict;
                break;
            case "SE":
                navigationDict = SouthEastNavDict;
                break;
            case "SW":
                navigationDict = SouthWestNavDict;
                break;
            default:
                navigationDict = EastNavDict;
                break;
        }
        return navigationDict;
    }
}
