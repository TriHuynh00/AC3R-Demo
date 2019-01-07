package org.saarland.accidentelementmodel;

import org.saarland.accidentconstructor.ConsoleLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Dell on 16/10/2017.
 */
public class TestCaseInfo {

    private String name;

    private String crashType;

    private HashMap<String, String> testCaseProp;

//    // Environment Property
//    String weatherCondition = "normal";
//    String lighting = "normal";
//    String laneNumber = "2";
//    String laneDivision = "undivided";
//    String roadType = "road";
//
//    /* I = Straight Road
//     * C = 90 Deg curve
//     * V = < 90 Deg curve
//     * ) = Bow curve with large radius
//     */
//    String curveShape = RoadShape.STRAIGHT;
//    int curveRadiusInMeter = 500;
//    String curveDirection = "left";

    private ArrayList<VehicleAttr> vehicleList;

    private ArrayList<Obstacle> obstacleList;

    private ArrayList<Street> streetList;

    private LinkedList<String> accidentFlows; // May Have

    public ArrayList<Street> getStreetList() {
        return streetList;
    }

    public String getCrashType() {
        return crashType;
    }

    public void setCrashType(String crashType) {
        this.crashType = crashType;
    }

    public void setStreetList(ArrayList<Street> streetList) {
        this.streetList = streetList;
    }

    public HashMap<String, String> getTestCaseProp() {
        return testCaseProp;
    }

    public void setTestCaseProp(HashMap<String, String> testCaseProp) {
        this.testCaseProp = testCaseProp;
    }

    public ArrayList<VehicleAttr> getVehicleList() {
        return vehicleList;
    }

    public void setVehicleList(ArrayList<VehicleAttr> vehicleList) {
        this.vehicleList = vehicleList;
    }

    public ArrayList<Obstacle> getObstacleList() {
        return obstacleList;
    }

    public void setObstacleList(ArrayList<Obstacle> obstacleList) {
        this.obstacleList = obstacleList;
    }

    public LinkedList<String> getAccidentFlows() {
        return accidentFlows;
    }

    public void setAccidentFlows(LinkedList<String> accidentFlows) {
        this.accidentFlows = accidentFlows;
    }

    public TestCaseInfo(String filePath)
    {
        init(filePath);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void printTestCaseInfo()
    {
        ConsoleLogger.print('d',"Test Case Name: " + name);
        ConsoleLogger.print('d',"Crash Type: " + crashType);
        Set<String> keySet = testCaseProp.keySet();
        for (String key : keySet)
        {
            ConsoleLogger.print('d',key + ":" + testCaseProp.get(key));
        }
    }

    private void init()
    {
        testCaseProp = new HashMap<String, String>();
        testCaseProp.put("weather"        , "normal"); // rainy, sunny, cloudy
        testCaseProp.put("lighting"       , "normal"); // dark, bright, light
        testCaseProp.put("lane_num"       , ""); // total number of lane
        testCaseProp.put("need_trigger"   , "F");
        testCaseProp.put("road_division"  , "undivided");
        testCaseProp.put("road_park_line" , ""); // #parking_lines : 0 - none; 1 - left only, 2 - right only, 3 - both
        testCaseProp.put("road_park_line_fill" , "0"); // specify whether parking line(s) is filled with cars
        testCaseProp.put("road_type"      , "road");
        testCaseProp.put("road_shape"     , RoadShape.STRAIGHT);
        testCaseProp.put("road_grade"     , "level");
        testCaseProp.put("road_direction" , "2-way");
        testCaseProp.put("road_grade_deg" , "0");
        testCaseProp.put("road_material"  , "asphalt");
        testCaseProp.put("curve_radius"   , "0"); // In Meters
        testCaseProp.put("curve_direction", "none");
        testCaseProp.put("junction_type"  , "none");
        testCaseProp.put("speed_limit"    , "");

        obstacleList  = new ArrayList<Obstacle>();
        accidentFlows = new LinkedList<String>();
        streetList    = new ArrayList<Street>();
    }

    public Street createNewStreet()
    {
        Street newStreet = new Street();
        newStreet.putValToKey("road_ID", "" + (streetList.size() + 1));
        streetList.add(newStreet);
        return newStreet;

    }

    private void init(String testCaseName)
    {
        name = testCaseName;
        init();
    }

    // Only add values that have a key being specified in the testCaseProp list
    public boolean putValToKey(String key, String value) {
        if (!testCaseProp.containsKey(key)) {
            return false;
        } else {
            testCaseProp.put(key, value);
            return true;
        }
    }

    // Get the value of a testCase property
    public String getEnvPropertyValue(String key) {
        if (testCaseProp.containsKey(key)) {
            return testCaseProp.get(key);
        }
        return "";
    }
}
