package org.saarland.accidentconstructor;

import org.saarland.accidentelementmodel.VehicleAttr;

import java.util.*;

public class VehicleTrajectoryFactory {
    private VehicleAttr vehicle;
    private HashMap<String, LinkedList> vehicleActionAndCoord;

    public VehicleTrajectoryFactory(VehicleAttr vehicle, HashMap<String, LinkedList> vehicleActionAndCoord) {
        this.vehicle = vehicle;
        this.vehicleActionAndCoord = vehicleActionAndCoord;
    }

    public ArrayList<VehicleTrajectory> generateVehicleTrajectories() {
        ArrayList<VehicleTrajectory> vehicleTrajectories = new ArrayList<VehicleTrajectory>();
        for (Map.Entry<String, LinkedList> entry : this.vehicleActionAndCoord.entrySet()) {
            if ((entry.getKey() == "follow" || entry.getKey() == "turn" || entry.getKey() == "stop") &&
                    !entry.getValue().isEmpty()) {
                for (Object st : entry.getValue()) {
                    String points = (String)st;
                    List<ArrayList<Float>> trajectories = new ArrayList<ArrayList<Float>>();
                    if (entry.getKey() == "stop") {
                        // Text Format: [[-2.0,97.88,0]]
                        String point = points.substring(1, points.length() - 1).replaceAll("\\s+","");
                        ArrayList<Float> pointXYZ = new ArrayList<Float>();
                        for (String field : point.split(",")) {
                            pointXYZ.add(Float.parseFloat(field));
                        }
                        trajectories.add(pointXYZ); // Key is stop, so we have 1 pointXYZ
                    } else {
                        // Text Format for Follow: [-2.0,97.88,0], [-2.0,17.88,0]
                        // Text Format for Turn: [-2.0,17.88,0], [-2.0,2.0,0], [-9.439806,-11.657189,0]
                        for (String point: points.split(", ")) {
                            ArrayList<Float> pointXYZ = new ArrayList<Float>();
                            point = point.substring(1, point.length()-1).replaceAll("\\s+","");
                            for (String field : point.split(",")) {
                                pointXYZ.add(Float.parseFloat(field));
                            }
                            trajectories.add(pointXYZ); // Key is follow/turn, so we have more than 1 pointXYZ
                        }
                    }
                    vehicleTrajectories.add(new VehicleTrajectory(entry.getKey(), trajectories));
                }
            }
        }

        // Find vehicle starting point from the method getMovementPath()
        String pointStr = this.vehicle.getMovementPath().get(0);
        ArrayList<Float> startPoint = new ArrayList<Float>();
        for (String field : pointStr.split(" ")) {
            startPoint.add(Float.parseFloat(field));
        }

        // Find the node containing the start point
        VehicleTrajectory headNode = findVehicleTrajectory(vehicleTrajectories, startPoint);
        vehicleTrajectories.remove(headNode);
        ArrayList<VehicleTrajectory> sortedList = new ArrayList<VehicleTrajectory>();
        sortedList.add(headNode);

        // Sort the nodes
        while (!vehicleTrajectories.isEmpty()) {
            startPoint = sortedList.get(sortedList.size() - 1).getLastPoint();
            VehicleTrajectory index = this.findVehicleTrajectory(vehicleTrajectories, startPoint);
            vehicleTrajectories.remove(index);
            sortedList.add(index);
        }
        return sortedList;
    }


    private VehicleTrajectory findVehicleTrajectory(ArrayList<VehicleTrajectory> list, ArrayList<Float> point) {
        for (VehicleTrajectory v : list) {
            if (v.findPoint(point)) {
                v.setFirstPoint(point);
                return v;
            }
        }
        return null;
    }
}
