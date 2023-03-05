package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public interface OutputContentGenerator {

    ByteArrayOutputStream generateFile(List<Point> points, List<Pline> lines, FileFormat format) throws Exception;

    ByteArrayOutputStream generateCsv(List<Point> points, List<Pline> lines) throws IOException;

    ByteArrayOutputStream generateTxt(List<Point> points, List<Pline> lines) throws IOException;

    ByteArrayOutputStream generateDxf(List<Point> points, List<Pline> lines) throws IOException;

    ByteArrayOutputStream generateKml(List<Point> points, List<Pline> lines) throws Exception;

    ByteArrayOutputStream generateGpx(List<Point> points, List<Pline> lines) throws Exception;
}
