package org.saarland.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LineStringTest {
    @Test
    @DisplayName("Normal Vector with A(1,2) B(3,-1)")
    public void testNormalVector00() {
        double[] actuals = new LineString(new Point(1, 2, 0), new Point(3, -1, 0)).normalVector();
        double[] expecteds = new double[]{3.0, 2.0};
        assertArrayEquals(expecteds, actuals, "A(1,2) B(3,-1) => (3,2)");
    }

    @Test
    @DisplayName("Normal Vector with A(3,2) B(-2,4)")
    public void testNormalVector01() {
        double[] actuals = new LineString(new Point(3, 2, 0), new Point(-2, 4, 0)).normalVector();
        double[] expecteds = new double[]{-2.0, -5.0};
        assertArrayEquals(expecteds, actuals, "A(3,2) B(-2,4) => (2.0, 5.0)");
    }

    @Test
    @DisplayName("Normal Vector with A(2,-4) B(5,-13)")
    public void testNormalVector02() {
        double[] actuals = new LineString(new Point(2, -4, 0), new Point(5, -13, 0)).normalVector();
        double[] expecteds = new double[]{9.0, 3.0};
        assertArrayEquals(expecteds, actuals, "A(2,4) B(5,13) => (3.0, 1.0)");
    }

    @Test
    @DisplayName("Normal Vector with A(1,41) B(5,49)")
    public void testNormalVector03() {
        double[] actuals = new LineString(new Point(1,41,0), new Point(5,49, 0)).normalVector();
        double[] expecteds = new double[]{-8.0, 4.0};
        assertArrayEquals(expecteds, actuals, "A(1,41) B(5,49) => (-2.0, 1.0)");
    }

    @Test
    @DisplayName("Find angle between A(2,-4) B(5,-13) and A(1,41) B(5,49)")
    public void findAngleWith00() {
        LineString ln1 = new LineString(new Point(2, -4, 0), new Point(5, -13, 0));
        LineString ln2 = new LineString(new Point(1,41,0), new Point(5,49, 0));
        int actuals = (int) ln1.angleWithLine(ln2);
        int expecteds = 45;
        assertEquals(expecteds, actuals);
    }

    @Test
    @DisplayName("Find angle between A(1,5) B(5,-7) and A(1,7) B(5,15)")
    public void findAngleWith01() {
        LineString ln1 = new LineString(new Point(1,5,0), new Point(5,-7,0));
        LineString ln2 = new LineString(new Point(1,7,0), new Point(5,15,0));
        int actuals = (int) ln1.angleWithLine(ln2);
        int expecteds = 45;
        assertEquals(expecteds, actuals);
    }

    @Test
    @DisplayName("Find angle between A(-40.0,-2.0) B(-20.0,-2.0) and C(3.0,-6.0)")
    public void findAngleWithPoint() {
        Point A = new Point(-40.0, -2.0, 0.0);
        Point B = new Point(-20.0, -2.0, 0.0);
        Point C = new Point(3.0, -6.0, 0.0);
        int actuals = (int) new LineString(A, B).angleWithPoint(C);
        int expecteds = 170;
        assertEquals(expecteds, actuals);
    }
}