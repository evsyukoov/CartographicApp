package ru.evsyukoov.transform.dto;

import ru.evsyukoov.transform.enums.CoordinatesType;
import ru.evsyukoov.transform.enums.FileFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Файл/текст после парсинга
 */
public class FileInfo {

    private FileFormat format;

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

    public FileFormat getFormat() {
        return format;
    }

    public void setFormat(FileFormat format) {
        this.format = format;
    }
}
