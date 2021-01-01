package org.saarland.accidentelementmodel;

import org.saarland.configparam.AccidentParam;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Dell on 16/10/2017.
 */
public class VehicleAttr {
    private int vehicleId = -1;

    private String travellingDirection = "";

    private int velocity = AccidentParam.defaultSpeed;

    // Onstreet Values:
    // -1: Car is on parking line
    // 0: Car is on curb or pavement
    // 1: Car is on the street
    // 2: Car is leaving from the side of the street
    private int onStreet = 1;
    private String standingRoadSide = ""; // traveling on left or right lane
    private Street standingStreet;

    // The lane number which the vehicle travels on, counting from the lane next to the right pavement outward.
    // By default, vehicle is set to travel on the lane closest to the right pavement

    // Lane number is count from the middle of the lane to the pavement, i.e. the lane closest to the road median is
    // lane #1. For example, if a road is 2-way, and has 2 lanes on each side (total 4 lanes). Then for each side,
    // lane #1 is nearest to the median, and lane #2 is nearest to the pavement.
    private int travelOnLaneNumber = AccidentParam.RIGHTMOSTLANE;

    // Vehicle appearance attributes
    private String yearMakeModel = "";
    private String vehicleType = ""; // truck, sedan, etc.
    private String beamngVehicleModel = "";
    private String partConfig = "";
    private String color = "";

    private LinkedList<String> actionList;

    private LinkedList<ActionDescription> actionDescriptionList = new LinkedList<ActionDescription>();

    private ArrayList<String> movementPath;

    private String waypointPathNodeName;

    private double leaveTriggerDistance = -1;

    private LinkedList<String> damagedComponents = new LinkedList<String>();

    public double getLeaveTriggerDistance() {
        return leaveTriggerDistance;
    }

    public void setLeaveTriggerDistance(double leaveTriggerDistance) {
        this.leaveTriggerDistance = leaveTriggerDistance;
    }

    public int getTravelOnLaneNumber() {
        return travelOnLaneNumber;
    }

    public void setTravelOnLaneNumber(int travelOnLaneNumber) {
        this.travelOnLaneNumber = travelOnLaneNumber;
    }

    public Street getStandingStreet() {
        return standingStreet;
    }

    public void setStandingStreet(Street standingStreet) {
        this.standingStreet = standingStreet;
    }

    public String getWaypointPathNodeName() {
        return waypointPathNodeName;
    }

    public void setWaypointPathNodeName(String waypointPathNodeName) {
        this.waypointPathNodeName = waypointPathNodeName;
    }

    public String getStandingRoadSide() {
        return standingRoadSide;
    }

    public void setStandingRoadSide(String standingRoadSide) {
        this.standingRoadSide = standingRoadSide;
    }

    public String getPartConfig() {
        return partConfig;
    }

    public void setPartConfig(String partConfig) {
        this.partConfig = partConfig;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getTravellingDirection() {
        return travellingDirection;
    }

    public void setTravellingDirection(String travellingDirection) {
        this.travellingDirection = travellingDirection;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public String getYearMakeModel() {
        return yearMakeModel;
    }

    public void setYearMakeModel(String yearMakeModel) {
        this.yearMakeModel = yearMakeModel;
    }

    public String getBeamngVehicleModel() {
        return beamngVehicleModel;
    }

    public void setBeamngVehicleModel(String vehicleType) {
        this.beamngVehicleModel = vehicleType;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public LinkedList<String> getActionList() {
        return actionList;
    }

    public LinkedList<ActionDescription> getActionDescriptionList() {
        return actionDescriptionList;
    }

    public void setActionList(LinkedList<String> newActionList) {
        this.actionList = newActionList;
    }

    public void setActionDescriptionList(LinkedList<ActionDescription> newActionDescriptionList) {
        this.actionDescriptionList = newActionDescriptionList;
    }


    public ArrayList<String> getMovementPath() {
        return movementPath;
    }

    public void setMovementPath(ArrayList<String> movementPath) {
        this.movementPath = movementPath;
    }

    public int getOnStreet() {
        return onStreet;
    }

    public void setOnStreet(int onStreet) {
        this.onStreet = onStreet;
    }

    public VehicleAttr() {
        actionList = new LinkedList<String>();
    }

    public LinkedList<String> getDamagedComponents() {
        return damagedComponents;
    }

}
