package ru.evsyukoov.transform.dto;

import ru.evsyukoov.transform.enums.FileFormat;

import java.util.ArrayList;
import java.util.List;

public class OutputInfo {

    private List<Point> points = new ArrayList<>();

    private List<Pline> lines = new ArrayList<>();

    private List<FileFormat> chosenFormat;

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public List<Pline> getLines() {
        return lines;
    }

    public void setLines(List<Pline> lines) {
        this.lines = lines;
    }

    public List<FileFormat> getChosenFormat() {
        return chosenFormat;
    }

    public void setChosenFormat(List<FileFormat> chosenFormat) {
        this.chosenFormat = chosenFormat;
    }
}
