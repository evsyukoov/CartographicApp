package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public interface OutputContentGenerator {

    ByteArrayOutputStream generateFile(List<Point> points, FileFormat fileFormat) throws IOException;

    ByteArrayOutputStream generateCsv(List<Point> points) throws IOException;

    ByteArrayOutputStream generateTxt(List<Point> points) throws IOException;

    ByteArrayOutputStream generateDxf(List<Point> points, List<Pline> lines);

    ByteArrayOutputStream generateKml(List<Point> points) throws IOException;

    ByteArrayOutputStream generateGpx(List<Point> points) throws IOException;
}
