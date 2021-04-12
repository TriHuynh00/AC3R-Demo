package org.saarland.accidentelementmodel;

import org.saarland.accidentconstructor.ConsoleLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Street {

    private HashMap<String, String> streetProp;

    public HashMap<String, String> getStreetProp() {
        return streetProp;
    }

    public void setStreetProp(HashMap<String, String> streetProp) {
        this.streetProp = streetProp;
    }

    public Street() {

        streetProp = new HashMap<String, String>();
        streetProp.put("road_ID", "");
        streetProp.put("lane_num", ""); // total number of lane
        streetProp.put("road_division", "undivided"); // divided, undivided
        streetProp.put("road_park_line", ""); // #parking_lines : 0 or "" - none; 1 - left only, 2 - right only, 3 - both
        streetProp.put("road_park_line_fill", ""); // specify whether parking line(s) is filled with cars
        streetProp.put("road_type", ""); // street; road
        streetProp.put("road_shape", ""); // RoadShape.STRAIGHT
        streetProp.put("road_grade", ""); // level, uphill
        streetProp.put("road_direction", ""); // 2-way, 1-way
        streetProp.put("road_navigation", ""); // N, E, S, W, NE, NW, SE, SW
        streetProp.put("road_grade_deg", ""); // >= 0
        streetProp.put("road_material", ""); // asphalt
        streetProp.put("road_angle", "0"); // the angle of diagonal road, see fuzzAngleOfRoad function in AccidentConstructor
        streetProp.put("curve_radius", ""); // In Meters
        streetProp.put("curve_direction", "none");
        streetProp.put("speed_limit", "");
        streetProp.put("pavement_type", "pavement");
        streetProp.put("road_node_list", ""); // Main road
        streetProp.put("right_pavement_node_list", ""); // Pavement constructed in positive y plan
        streetProp.put("left_pavement_node_list", ""); // Pavement constructed in negative y plan
        streetProp.put("right_parkline_node_list", ""); // parking line constructed in positive y plan
        streetProp.put("left_parkline_node_list", ""); // parking line constructed in negative y plan
        streetProp.put("is_single_road_piece", "F"); // define whether this road is intersected in the middle: F = the road stops at intersection, T = the road continue after intersection
        streetProp.put("traffic_sign_list", ""); // All traffic control signs in this road
    }

    // Only add values that have a key being specified in the streetProp list
    public void putValToKey(String key, String value) {
        if (!streetProp.containsKey(key)) {
            return;
        } else {
            streetProp.put(key, value);
        }
    }

    // Get the value of a street property
    public String getStreetPropertyValue(String key) {
        if (streetProp.containsKey(key)) {
            return streetProp.get(key);
        }
        return "";
    }

    public void printStreetInfo() {
        Set<String> keySet = streetProp.keySet();
        for (String key : keySet) {
            ConsoleLogger.print('d',key + ":" + streetProp.get(key));
        }
    }
}