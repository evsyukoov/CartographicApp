package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.service.OutputContentGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
@Slf4j
public class OutputContentGeneratorImpl implements OutputContentGenerator {

    @Override
    public ByteArrayOutputStream generateFile(List<Point> points, FileFormat format) throws IOException {
        if (CollectionUtils.isEmpty(points)) {
            return null;
        }
        ByteArrayOutputStream baos = null;
        if (format == FileFormat.KML) {
            baos = this.generateKml(points);
        } else if (format == FileFormat.GPX) {
            baos = this.generateGpx(points);
        } else if (format == FileFormat.TXT) {
            baos = this.generateTxt(points);
        } else if (format == FileFormat.CSV) {
            baos = this.generateCsv(points);
        }
        return baos;
    }

    @Override
    public ByteArrayOutputStream generateCsv(List<Point> points) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeKMLHeader(out);
            for (Point p : points) {
                out.write((String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, p.h).getBytes()));
            }
            out.write(("</Document>\n</kml>").getBytes());
            return out;
        }
    }

    @Override
    public ByteArrayOutputStream generateTxt(List<Point> points) throws IOException {
        return generateCsv(points);
    }

    @Override
    public ByteArrayOutputStream generateDxf(List<Point> points, List<Pline> lines) {
        return null;
    }

    @Override
    public ByteArrayOutputStream generateKml(List<Point> points) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeKMLHeader(out);
            for (Point p : points) {
                out.write((String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                        "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", p.name, p.y, p.x, p.h)).getBytes());
            }
            out.write(("</Document>\n</kml>").getBytes());
            return out;
        }
    }

    @Override
    public ByteArrayOutputStream generateGpx(List<Point> points) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeGPXHeader(out);
            for (Point p : points) {
                out.write((String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", p.x, p.y, p.name, p.name)).getBytes());
            }
            out.write(("</Document>\n</kml>").getBytes());
            return out;
        }
    }

    private void writeKMLHeader(OutputStream out) throws IOException {
        out.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://earth.google.com/kml/2.0\">" +
                "<Document><Style id=\"z1\"><IconStyle><scale>1.2</scale><color>ffFFFFFF</color><Icon>" +
                "<href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon></IconStyle></Style>\n").getBytes());
    }

    private void writeGPXHeader(OutputStream out) throws IOException {
        out.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx\nxmlns=\"http://www.topografix.com/GPX/1/1\"" +
                "\nversion=\"1.1\"\ncreator=\"InjGeo_bot\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/WaypointExtension/v1 \">\n").getBytes());
    }
}
