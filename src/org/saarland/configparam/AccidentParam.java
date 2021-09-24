package org.saarland.configparam;

import java.text.DecimalFormat;

public class AccidentParam {
    public static final boolean DEBUG = true;
    public static final int SIMULATION_DURATION = 2; // Duration in steps
    public static final int DISTANCE_BETWEEN_CARS = 9;
    public static final int ROAD_PIECE_NODE = 4;
    public static final int BEAMNG_PORT = 64256;
    public static final int UNKNOWN_VEHICLE = 300;

    public static String[] LEFTRIGHTARR = {"left", "right"};

    public static String[] cardinalDirectionPair = new String[] {"E/W", "N/S", "NE/SW", "NW/SE"};

    public static String userFolder = System.getProperty("user.home") + "\\Documents\\";
//    public static String userFolder = "D:\\Games\\";
    public static String beamNGUserPath = userFolder + "BeamNG.research";
//    public static String beamNGUserPath = userFolder + "BeamNG.research.v1.7.0.1";
    public static String environmentFile = beamNGUserPath + "\\levels\\smallgrid\\main\\MissionGroup\\items.level.json";

    public static String finalResultLocation = beamNGUserPath + "\\levels\\smallgrid";
    public static String scenarioConfigFilePath  = beamNGUserPath + "\\levels\\smallgrid\\scenarios";

    public static String templateFilePath  = "template";

    public static String vehicleFilePath                = templateFilePath + "\\vehicle.txt";
    public static String waypointFilePath               = templateFilePath + "\\waypoint.txt";
    public static String roadFilePath                   = templateFilePath + "\\road.txt";
    public static String headerFilePath                 = templateFilePath + "\\header.txt";
    public static String luaAIFilePath                  = templateFilePath + "\\followPathLua.txt";
    public static String luaAIConfigFilePath            = templateFilePath + "\\followPathAIConfig.txt";
    public static String luaAICarLeaveTriggerFilePath   = templateFilePath + "\\carLeaveAtDistance.txt";
    public static String scenarioJsonFilePath           = templateFilePath + "\\scenarioJson.txt";

    public static String skyFilePath                    = templateFilePath + "\\sky.txt";
    public static String rainyFilePath                  = templateFilePath + "\\rain.txt";
    public static String lampFilePath                   = templateFilePath + "\\lamp.txt";
    public static String cloudFilePath                  = templateFilePath + "\\cloud.txt";
    public static String pointLightFilePath             = templateFilePath + "\\pointLight.txt";


    public static String beamNGpyPath =  userFolder + "AC3RGithub\\demo\\ac3r.py";

    public static String beamNGpyPlusPath = userFolder + "AC3R-Demo\\demo\\ac3r.py";
//    public static String beamNGpyPlusPath = "D:\\University\\MasterProgram\\Thesis\\Draft\\AC3RGithub\\demo\\BeamNGpy\\ac3rp.py";


    public static DecimalFormat df2Digit = new DecimalFormat("####.##");
    public static DecimalFormat df6Digit = new DecimalFormat("####.######");

    public static String DB_CRASH_CIREN = "CIREN";
    public static String DB_CRASH_NMVCCS = "NMVCCS";

    public static double laneWidth = 4;  // lane Width unit in Beamng
    public static int parkingLineWidth = 2;  // parking lane Width unit in Beamng
    public static double shoulderPadding = 0.5;

    public static int defaultSpeed  = 20; // mph
    public static double accelerationTo20Mph = 2.5; // 2.9; // m/s2

    public static String NO_CRASH_STR = "noCrash";
    public static String FAILED_TO_GENERATE = "generationFail";

    public static boolean isGradingConcerned = false;

    public static String defaultCoordDelimiter = ":";
    public static String beamngCoordDelimiter = " ";
    public static String STREET_NODE_DELIMITER = ";";

    /************ MATERIAL TEXTURE **************/

    public static String asphaltMaterial = "road_asphalt_2lane";
    public static String dirtMaterial = "road_dirt";
    public static String pavementMaterial = "sidewalk_concrete";
    public static String twoWayLaneDivisionMaterial = "line_yellow";
    public static String laneDivisionMaterial = "crossing_white";
    public static String shoulderDivisionMaterial = "line_white";

    public static int RIGHTMOSTLANE = 100;

    // Rotation Matrix (heading toward certain direction)
    public static String headEast = "0.000197529793 0.999126077 0.041796688 -0.997556925 0.00311672688 -0.0697889775 -0.069858253 -0.0416807905 0.996685803";
    public static String headWest = "0.000197529793 -0.997556925 -0.069858253 0.999126077 0.00311672688 -0.0416807905 0.041796688 -0.0697889775 0.996685803";
    public static String headNorth = "-0.99960494 0.00156929635 -0.0280615631 0.00156912173 -0.993766546 -0.111469768 -0.0280615743 -0.111469768 0.993371546";
    public static String headSouth = "1 0 0 0 0.999999762 -0.000690534245 0 0.000690534245 0.999999762";
    public static String headSouthWest = "0.642858148 -0.764493704 -0.0477782972 0.765054226 0.643900871 -0.00914313272 0.0377543531 -0.0306752548 0.998816133";
    public static String baseOrientation = "0.0140541 0.0558275 -0.998342";
}
