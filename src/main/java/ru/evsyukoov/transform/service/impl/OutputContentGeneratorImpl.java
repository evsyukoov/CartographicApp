package ru.evsyukoov.transform.service.impl;

import com.jsevy.jdxf.DXFCircle;
import com.jsevy.jdxf.DXFDocument;
import com.jsevy.jdxf.DXFGraphics;
import com.jsevy.jdxf.DXFStyle;
import com.jsevy.jdxf.DXFText;
import com.jsevy.jdxf.RealPoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.evsyukoov.transform.constants.Const;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.service.OutputContentGenerator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OutputContentGeneratorImpl implements OutputContentGenerator {

    private final AffineTransform transformationRotate;

    private final DXFStyle dxfStyle;

    private final Transformer xmlPrettyPrint;

    @Value("${text.delimetr}")
    private String delimetr;

    @Autowired
    public OutputContentGeneratorImpl(AffineTransform transformationRotate,
                                      DXFStyle dxfStyle, Transformer xmlPrettyPrint) {
        this.transformationRotate = transformationRotate;
        this.dxfStyle = dxfStyle;
        this.xmlPrettyPrint = xmlPrettyPrint;
    }

    @Override
    public ByteArrayOutputStream generateFile(List<Point> points, List<Pline> lines, FileFormat format) throws Exception {
        if (CollectionUtils.isEmpty(points) && CollectionUtils.isEmpty(lines)) {
            return null;
        }
        ByteArrayOutputStream baos = null;
        if (format == FileFormat.KML) {
            baos = this.generateKml(points, lines);
        } else if (format == FileFormat.GPX) {
            baos = this.generateGpx(points, lines);
        } else if (format == FileFormat.TXT) {
            baos = this.generateTxt(points, lines);
        } else if (format == FileFormat.CSV) {
            baos = this.generateCsv(points, lines);
        } else if (format == FileFormat.DXF) {
            baos = this.generateDxf(points, lines);
        }
        return baos;
    }

    @Override
    public ByteArrayOutputStream generateCsv(List<Point> points, List<Pline> lines) throws IOException {
        Charset charset = Charset.forName(Const.WIN_1251_ENCODING);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Point p : points) {
                out.write(String.format("%s%s%s%s%s%s%s\n", p.name, delimetr, p.x, delimetr, p.y, delimetr, p.h).getBytes(charset));
            }
            int i = 1;
            for (Pline line : lines) {
                int j = 1;
                out.write(String.format("Polyline N %d\n", i).getBytes(charset));
                for (Point p : line.getPolyline()) {
                    out.write(
                            String.format("Vertex N %d%s%s%s%s%s%s\n", j, delimetr, p.x, delimetr, p.y, delimetr, p.h).getBytes(charset));
                    j++;
                }
                i++;
            }
            return out;
        }
    }

    @Override
    public ByteArrayOutputStream generateTxt(List<Point> points, List<Pline> lines) throws IOException {
        return generateCsv(points, lines);
    }

    @Override
    public ByteArrayOutputStream generateDxf(List<Point> points, List<Pline> lines) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DXFDocument dxfDocument = new DXFDocument("DXF Document");
            dxfDocument.getGraphics().setTransform(transformationRotate);
            writeBlocks(dxfDocument, points);
            writeLines(dxfDocument, lines);
            String text = dxfDocument.toDXFString();
            out.write(text.getBytes());
            return out;
        }
    }

    private void writeLines(DXFDocument document, List<Pline> lines) {
        DXFGraphics graphics = document.getGraphics();
        for (Pline line : lines) {
            List<Double> xPoints = new ArrayList<>();
            List<Double> yPoints = new ArrayList<>();
            line.getPolyline().forEach(point -> {
                // X и Y автокада повернуты на 90, при парсинге DXF это также учитывается
                xPoints.add(point.y);
                yPoints.add(point.x);
            });
            graphics.drawPolyline(ArrayUtils.toPrimitive(xPoints.toArray(new Double[0])),
                    ArrayUtils.toPrimitive(yPoints.toArray(new Double[0])), xPoints.size());
        }
    }

    private void writeBlocks(DXFDocument document, List<Point> points) {
        DXFGraphics graphics = document.getGraphics();
        for (Point p : points) {
            // X и Y автокада повернуты на 90, при парсинге DXF это также учитывается
            RealPoint point = new RealPoint(p.y, p.x, p.h);
            DXFCircle circle = new DXFCircle(point, Const.CIRCLE_RADIUS, graphics);
            DXFText text = new DXFText(p.name, shiftPoint(point), dxfStyle, graphics);
            document.addEntity(circle);
            document.addEntity(text);
        }
    }

    private RealPoint shiftPoint(RealPoint point) {
        return new RealPoint(point.x, point.y + Const.TEXT_SHIFTING, 0);
    }

    @Override
    public ByteArrayOutputStream generateKml(List<Point> points, List<Pline> lines) throws Exception {
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
            return xmlPrettyPrint(out);
        }
    }

    @Override
    public ByteArrayOutputStream generateGpx(List<Point> points, List<Pline> lines) throws Exception {
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
            return xmlPrettyPrint(out);
        }
    }

    private ByteArrayOutputStream xmlPrettyPrint(ByteArrayOutputStream baos) throws TransformerException, ParserConfigurationException, IOException, SAXException {
        InputSource inputSource= new InputSource(new ByteArrayInputStream(baos.toByteArray()));
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSource);

        ByteArrayOutputStream baosAfterPrettyPrint = new ByteArrayOutputStream();
        xmlPrettyPrint.transform(new DOMSource(document), new StreamResult(baosAfterPrettyPrint));
        return baosAfterPrettyPrint;
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
