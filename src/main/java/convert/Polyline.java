package convert;

import java.util.LinkedList;

public class Polyline {
    LinkedList<Point> pline;

    public Polyline() {
        pline = new LinkedList<>();
    }

    public void addPoint(Point p)
    {
        pline.add(p);
    }

    public LinkedList<Point> getPline() {
        return pline;
    }
}
