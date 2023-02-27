package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.kabeja.DraftDocument;
import org.kabeja.common.Block;
import org.kabeja.dxf.generator.DXFGenerator;
import org.kabeja.entities.Attrib;
import org.kabeja.entities.Insert;
import org.kabeja.io.GenerationException;
import org.kabeja.math.Point3D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.service.OutputContentGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class OutputContentGeneratorImpl implements OutputContentGenerator {

    private final DXFGenerator dxfGenerator;

    private final Block defaultAutocadBlock;

    @Autowired
    public OutputContentGeneratorImpl(DXFGenerator dxfGenerator,
                                      Block defaultAutocadBlock) {
        this.dxfGenerator = dxfGenerator;
        this.defaultAutocadBlock = defaultAutocadBlock;
    }

    @Override
    public ByteArrayOutputStream generateFile(List<Point> points, FileFormat format) throws IOException, GenerationException {
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
        } else if (format == FileFormat.DXF) {
            baos = this.generateDxf(points);
        }
        return baos;
    }

    @Override
    public ByteArrayOutputStream generateFile(List<Point> points, List<Pline> lines, FileFormat format) throws IOException, GenerationException {
        if (CollectionUtils.isEmpty(points) && CollectionUtils.isEmpty(lines)) {
            return null;
        }
        ByteArrayOutputStream baos = null;
        if (format == FileFormat.KML) {
            baos = this.generateKml(points, lines);
        } else if (format == FileFormat.GPX) {
            baos = this.generateGpx(points, lines);
        } else if (format == FileFormat.TXT) {
            baos = this.generateTxt(points);
        } else if (format == FileFormat.CSV) {
            baos = this.generateCsv(points);
        } else if (format == FileFormat.DXF) {
            baos = this.generateDxf(points, lines);
        }
        return baos;
    }

    @Override
    public ByteArrayOutputStream generateCsv(List<Point> points) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Point p : points) {
                out.write(String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, p.h).getBytes());
            }
            return out;
        }
    }

    @Override
    public ByteArrayOutputStream generateTxt(List<Point> points) throws IOException {
        return generateCsv(points);
    }

    @Override
    public ByteArrayOutputStream generateDxf(List<Point> points, List<Pline> lines) throws IOException, GenerationException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DraftDocument draftDocument = new DraftDocument();
            writeBlocks(draftDocument, points, out);
            // write lines ...
            return out;
        }
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
            out.write(("</gpx>").getBytes());
            return out;
        }
    }

    @Override
    public ByteArrayOutputStream generateDxf(List<Point> points) throws IOException, GenerationException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DraftDocument draftDocument = new DraftDocument();
            return writeBlocks(draftDocument, points, out);
        }
    }

    private ByteArrayOutputStream writeBlocks(DraftDocument draftDocument, List<Point> points, ByteArrayOutputStream out) throws GenerationException {
        draftDocument.addBlock(defaultAutocadBlock);
        for (Point p : points) {
            Insert insert = new Insert();
            insert.setBlockName(defaultAutocadBlock.getName());
            Point3D point = new Point3D(p.x, p.y, p.h);
            insert.setInsertPoint(point);

            Attrib attrib = new Attrib();
            attrib.setBlockEntity(true);
            attrib.setBlockAttribute(true);
            attrib.setTag("NAME");
            attrib.setInsertPoint(point);
            attrib.setHeight(20);
            attrib.setText(p.name);
            insert.addAttribute(attrib);
            draftDocument.addEntity(insert);
        }
        dxfGenerator.generate(draftDocument, Collections.emptyMap(), out);
        return out;
    }

    @Override
    public ByteArrayOutputStream generateKml(List<Point> points, List<Pline> lines) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeKMLHeader(out);
            for (Point p : points) {
                out.write((String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                        "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", p.name, p.y, p.x, p.h)).getBytes());
            }
            int i = 1;
            for (Pline pline : lines) {
                out.write(String.format("<Placemark><name>78</name><description>Unknown Area Type&#60;br&#62;styleUrl: #area%d</description>", i).getBytes());
                out.write("<Style><LineStyle><color>A6000000</color><width>2</width></LineStyle></Style>".getBytes());
                out.write(String.format("<LineString><extrude>%d</extrude><coordinates>", i).getBytes());
                for (Point p : pline.getPolyline()) {
                    out.write(String.format("%s,%s,%s ", p.y, p.x, p.h).getBytes());
                }
                out.write("</coordinates></LineString></Placemark>\n".getBytes());
                i++;
            }
            out.write(("</Document>\n</kml>").getBytes());
            return out;
        }
    }

    @Override
    public ByteArrayOutputStream generateGpx(List<Point> points, List<Pline> lines) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeGPXHeader(out);
            for (Point p : points) {
                out.write((String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", p.x, p.y, p.name, p.name)).getBytes());
            }
            for (Pline pline : lines) {
                out.write("<trk><name>injgeo</name><trkseg>".getBytes());
                for (Point p : pline.getPolyline()) {
                    out.write(String.format("<trkpt lat=\"%s\" lon=\"%s\"><ele>%s</ele></trkpt>", p.x, p.y, p.h).getBytes());
                }
                out.write("</trkseg></trk>".getBytes());
            }
            out.write("</gpx>".getBytes());
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
