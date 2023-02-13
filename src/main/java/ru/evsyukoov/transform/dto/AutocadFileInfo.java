package ru.evsyukoov.transform.dto;

import ru.evsyukoov.transform.convert.Point;
import java.util.List;

public class AutocadFileInfo extends FileInfo {

    private List<Polyline> polylines;

    public static class Polyline {

        List<Point> polyline;

        public List<Point> getPolyline() {
            return polyline;
        }

        public void setPolyline(List<Point> polyline) {
            this.polyline = polyline;
        }
    }
}
