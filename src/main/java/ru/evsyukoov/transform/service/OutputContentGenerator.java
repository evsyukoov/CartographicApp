package ru.evsyukoov.transform.service;

import org.kabeja.io.GenerationException;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public interface OutputContentGenerator {

    ByteArrayOutputStream generateFile(List<Point> points, FileFormat fileFormat) throws IOException, GenerationException;

    ByteArrayOutputStream generateFile(List<Point> points, List<Pline> lines, FileFormat fileFormat) throws IOException, GenerationException;

    ByteArrayOutputStream generateCsv(List<Point> points) throws IOException;

    ByteArrayOutputStream generateTxt(List<Point> points) throws IOException;

    ByteArrayOutputStream generateDxf(List<Point> points) throws IOException, GenerationException;

    ByteArrayOutputStream generateKml(List<Point> points) throws IOException;

    ByteArrayOutputStream generateGpx(List<Point> points) throws IOException;

    ByteArrayOutputStream generateDxf(List<Point> points, List<Pline> lines) throws IOException, GenerationException;

    ByteArrayOutputStream generateKml(List<Point> points, List<Pline> lines) throws IOException;

    ByteArrayOutputStream generateGpx(List<Point> points, List<Pline> lines) throws IOException;
}
