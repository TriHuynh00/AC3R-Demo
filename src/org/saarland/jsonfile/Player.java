package org.saarland.jsonfile;
import org.saarland.accidentconstructor.VehicleTrajectory;
import org.saarland.geometry.LineString;
import org.saarland.geometry.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Player {
    private ArrayList<Point> points = new ArrayList<Point>();
    private final String follow = "follow";
    private final String turn = "turn";
    private ArrayList<VehicleTrajectory> trajectories = new ArrayList<VehicleTrajectory>();


    public Player(ArrayList<VehicleTrajectory> paths) {
        this.setPoints(paths);
        this.setTrajectories();
//        System.out.println(this.getTrajectories());
//        System.exit(0);
    }

    private void setPoints(ArrayList<VehicleTrajectory> paths) {
        List<ArrayList<Float>> trajectories = new ArrayList<>();
        for (VehicleTrajectory v : paths) {
            trajectories.addAll(v.getTrajectory());
        }

        ArrayList<ArrayList<Float>> list = new ArrayList<>();
        for (ArrayList<Float> el : trajectories) {
            if (!list.contains(el)) {
                list.add(el);
            }
        }

        ArrayList<Point> points = new ArrayList<Point>();
        for (ArrayList<Float> trajec : list) {
            points.add(new Point(trajec.get(0), trajec.get(1), trajec.get(2)));
        }
        this.points = points;
    }

    private void setTrajectories() {
        for (int i=0; i < this.points.size(); i++) {
            if (i+2 >= this.points.size()) {
                break;
            }
            Point A = this.points.get(i);
            Point B = this.points.get(i+1);
            Point C = this.points.get(i+2);

            LineString AB = new LineString(A,B);
            LineString BC = new LineString(B,C);
            double angle = AB.angleWithLine(BC);

            if (angle >= 178 && angle <= 180 || angle >= 0 && angle <= 2) {
                System.out.println("We have 2 straight segments!");
                trajectories.add(new VehicleTrajectory(follow, AB.toArrayFloat()));
                trajectories.add(new VehicleTrajectory(follow, BC.toArrayFloat()));
            } else {
                double consBPlus = 0.1;
                Point Bplus = new Point(B.x + consBPlus * (B.x - A.x), B.y + consBPlus * (B.y - A.y), 0);

                double consCPlusX = 0.8;
                double consCPlusY = 0.6;
                Point Cplus = new Point(Bplus.x + consCPlusX * (C.x - Bplus.x), Bplus.y + consCPlusY * (C.y - Bplus.y), 0);

                // Segment 01: AB - Straight
                trajectories.add(new VehicleTrajectory(follow, AB.toArrayFloat()));
                // Segment 02: BB' - Straight
                trajectories.add(new VehicleTrajectory(follow, new LineString(B, Bplus).toArrayFloat()));
                // Segment 03: B'C'C - Curve
                List<ArrayList<Float>> curve = new ArrayList<>();
                curve.add(Bplus.toArrayFloat());
                curve.add(Cplus.toArrayFloat());
                curve.add(C.toArrayFloat());
                trajectories.add(new VehicleTrajectory(turn, curve));
            }
        }
    }

    public ArrayList<VehicleTrajectory> getTrajectories() {
        ArrayList<VehicleTrajectory> list = new ArrayList<>();

        int i = 0;
        while (i < this.trajectories.size()) {
            VehicleTrajectory el = this.trajectories.get(i);
            if (!this.isExisted(list, el)) {
                list.add(el);
            }
            i++;
        }
        return list;
    }

    private boolean isExisted(ArrayList<VehicleTrajectory> list, VehicleTrajectory target) {
        for (VehicleTrajectory el : list) {
            if (el.getAction().equals(target.getAction())
                && el.getTrajectory().get(0).equals(target.getTrajectory().get(0))
                && el.getTrajectory().get(1).equals(target.getTrajectory().get(1))) {
                return true;
            }
        }
        return false;
    }
}
