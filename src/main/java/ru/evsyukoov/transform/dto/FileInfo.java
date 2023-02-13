package ru.evsyukoov.transform.dto;

import ru.evsyukoov.transform.convert.Point;
import ru.evsyukoov.transform.enums.CoordinatesType;

import java.util.ArrayList;
import java.util.List;

/**
 * Файл/текст после парсинга
 */
public class FileInfo {

    private CoordinatesType coordinatesType;

    private List<Point> points = new ArrayList<>();

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public CoordinatesType getCoordinatesType() {
        return coordinatesType;
    }

    public void setCoordinatesType(CoordinatesType coordinatesType) {
        this.coordinatesType = coordinatesType;
    }
}
