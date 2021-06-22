package org.saarland.geometry;

import java.util.ArrayList;
import java.util.List;

public class LineString {
    private Point A;
    private Point B;
    double a = 1;
    double b = 1;
    double length = 0;


    public LineString(Point p1, Point p2) {
        this.A = p1;
        this.B = p2;
        this.setNormalVector();
        this.setLength();
    }

    private void setLength() {
        double x1=this.A.x;
        double y1=this.A.y;
        double x2=this.B.x;
        double y2=this.B.y;
        this.length = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    private void setNormalVector() {
        double x1 = this.A.x;
        double y1 = this.A.y;
        double x2 = this.B.x;
        double y2 = this.B.y;

        if (x1 == x2 && y1 != y2) {
            this.a = 1;
            this.b = 0;
        }
        if (x1 != x2 && y1 == y2) {
            this.a = 0;
            this.b = 1;
        } else {
            this.a = y1 - y2;
            this.b = x2 - x1;
        }
    }

    public double[] normalVector() {
        return new double[]{this.a, this.b};
    }

    public String toString() {
        return "LINESTRING(" + this.A.toString() + ',' + this.B.toString() + ')';
    }

    public double angleWithLine(LineString ln) {
        double a1 = this.a;
        double a2 = this.b;
        double b1 = ln.a;
        double b2 = ln.b;
        double cosa = Math.abs(a1*b1 + a2*b2) / (Math.sqrt(a1*a1 + a2*a2) * Math.sqrt(b1*b1 + b2*b2));
        return Math.acos(cosa)*180.0d/Math.PI;
    }

    public double angleWithPoint(Point C) {
        Point A = this.A;
        Point B = this.B;

        double AB = new LineString(A, B).length;
        double BC = new LineString(B, C).length;
        double AC = new LineString(A, C).length;
        double cosB = (AB*AB + BC*BC - AC*AC) / (2 * AB * BC);
        double beta = Math.acos(cosB)*180.0d/Math.PI;
        return beta;
    }

    public List<ArrayList<Float>> toArrayFloat() {
        List<ArrayList<Float>> list = new ArrayList<>();
        list.add(this.A.toArrayFloat());
        list.add(this.B.toArrayFloat());
        return list;
    }
}