package org.saarland.geometry;

import java.util.ArrayList;

public class Point {
    public double x = 0;
    public double y = 0;
    public double z = 0;

    public Point(double p1, double p2, double p3) {
        this.x = p1;
        this.y = p2;
        this.z = p3;
    }

    public String toString() {
        return this.x + "," + this.y;
    }

    public ArrayList<Float> toArrayFloat() {
        ArrayList<Float> list = new ArrayList<Float>();
        list.add((float) this.x);
        list.add((float) this.y);
        list.add((float) this.z);
        return list;
    }
}