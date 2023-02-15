package ru.evsyukoov.transform.dto;

import ru.evsyukoov.transform.convert.Point;

import java.util.ArrayList;
import java.util.List;

public class AutocadFileInfo extends FileInfo {

    private List<Polyline> polylines = new ArrayList<>();

    public static class Polyline {

        List<Point> polyline = new ArrayList<>();

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

    public List<Polyline> getPolylines() {
        return polylines;
    }
}
