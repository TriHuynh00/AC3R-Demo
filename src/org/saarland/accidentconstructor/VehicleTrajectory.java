package org.saarland.accidentconstructor;

import java.util.ArrayList;
import java.util.List;

public class VehicleTrajectory {
    private String action;
    private List<ArrayList<Float>> trajectory;
    private ArrayList<Float> firstPoint;
    private ArrayList<Float> lastPoint;

    public VehicleTrajectory(String action, List<ArrayList<Float>>segments) {
        this.action = action;
        this.trajectory = segments;
        this.firstPoint = segments.get(0);
        this.lastPoint = segments.get(segments.size()-1);
    }

    public String getAction() { return this.action; }
    public List<ArrayList<Float>> getTrajectory() { return this.trajectory; }
    public ArrayList<Float> getFirstPoint() { return this.firstPoint ;}
    public ArrayList<Float> getLastPoint() { return this.lastPoint ;}
    public boolean findPoint(ArrayList<Float> point) {
        for (ArrayList<Float> el : this.trajectory) {
            if (el.equals(point)) {
                return true;
            }
        }
        return false;
    }
    public void setFirstPoint(ArrayList<Float> point) {
        if (this.findPoint(point) && !this.firstPoint.equals(point)) {
            ArrayList<Float> tmp = this.firstPoint;
            this.firstPoint = this.lastPoint;
            this.lastPoint = tmp;
        }
    }

    @Override
    public String toString(){
        return ("action: " + action + ", trajectories: " +  trajectory.toString());
    }
}
