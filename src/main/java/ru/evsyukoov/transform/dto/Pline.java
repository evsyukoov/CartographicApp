package ru.evsyukoov.transform.dto;

import java.util.ArrayList;
import java.util.List;

public class Pline {

    private List<Point> polyline = new ArrayList<>();

    public List<Point> getPolyline() {
        return polyline;
    }

    public void setPolyline(List<Point> polyline) {
        this.polyline = polyline;
    }

    public void addPoint(Point point) {
        polyline.add(point);
    }
}
