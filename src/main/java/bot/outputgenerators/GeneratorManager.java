package bot.outputgenerators;

import bot.Client;
import bot.enums.OutputFileType;
import bot.enums.TransType;
import convert.Point;
import convert.Polyline;
import convert.Transformator;
import org.osgeo.proj4j.Proj4jException;

import java.io.*;
import java.util.LinkedList;
import java.util.function.Function;

public class GeneratorManager {

    Client client;

    Transformator transformator;

    File output;

    public GeneratorManager(Client client) {
        this.client = client;
        if (client.getTransType() != TransType.WGS_TO_WGS) {
            transformator = new Transformator(client);
            transformator.initTransformation();
        }
        output = new File(String.format("%s.%s", client.getSavePath(),
                client.getOutputFileType().name().toLowerCase()));
    }

    public void run() throws IOException, Proj4jException, IllegalStateException {
        if (!client.getInfoReader().isDxf()) {
            if (client.getOutputFileType() == OutputFileType.CSV)
                writeCSV();
            else if (client.getOutputFileType() == OutputFileType.GPX)
                writeGPX();
            else if (client.getOutputFileType() == OutputFileType.KML)
                writeKML();
        } else {
            if (client.getOutputFileType() == OutputFileType.CSV)
                writeCsvFromDxf();
            else if (client.getOutputFileType() == OutputFileType.GPX)
                writeGPXFromDxf();
            else if (client.getOutputFileType() == OutputFileType.KML)
                writeKMLFromDxf();
        }
    }

    public File getOutput() {
        return output;
    }

    private void writeKMLHeader(Writer kml) throws IOException {
        kml.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://earth.google.com/kml/2.0\">" +
                "<Document><Style id=\"z1\"><IconStyle><scale>1.2</scale><color>ffFFFFFF</color><Icon>" +
                "<href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon></IconStyle></Style>\n");
    }

    private void writeGPXHeader(Writer gpx) throws IOException {
        gpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx\nxmlns=\"http://www.topografix.com/GPX/1/1\"" +
                "\nversion=\"1.1\"\ncreator=\"InjGeo_bot\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/WaypointExtension/v1 \">\n");
    }

    private void writeCSV() throws IOException, Proj4jException, IllegalStateException {
        try (Writer csv = new OutputStreamWriter(new FileOutputStream(output), "windows-1251")) {
            LinkedList<Point> src = client.getInfoReader().getReadedPoints();
            if (client.getTransType() == TransType.WGS_TO_WGS) {
                for (Point p : src) {
                    csv.write(String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, p.h));
                }
            } else if (client.getTransType() == TransType.MSK_TO_WGS || client.getTransType() == TransType.WGS_TO_MSK) {
                for (Point p : src) {
                    Point res = transformator.transformOnePoint(p);
                    csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, p.h));
                }
            } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                for (Point p : src) {
                    transformator.initExtraTransformation();
                    Point res = transformator.twoStepTransform(p);
                    csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, p.h));
                }
            }
        }
    }

    private void writeGPX() throws IOException, Proj4jException {
        try (Writer gpx = new OutputStreamWriter(new FileOutputStream(output))) {
            LinkedList<Point> src = client.getInfoReader().getReadedPoints();
            writeGPXHeader(gpx);
            if (client.getTransType() == TransType.WGS_TO_WGS) {
                for (Point p : src) {
                    gpx.write(String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", p.x, p.y, p.name, p.name));
                }
            } else if (client.getTransType() == TransType.MSK_TO_WGS || client.getTransType() == TransType.WGS_TO_MSK) {
                for (Point p : src) {
                    Point point = transformator.transformOnePoint(p);
                    gpx.write(String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n",
                            point.x, point.y, point.name, point.name));
                }
            } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                for (Point p : src) {
                    transformator.initExtraTransformation();
                    Point point = transformator.twoStepTransform(p);
                    gpx.write(String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n",
                            point.x, point.y, point.name, point.name));
                }
            }
            gpx.write("</gpx>");
        }
    }


    private void writeKML() throws IOException, Proj4jException {
        try (Writer kml = new OutputStreamWriter(new FileOutputStream(output))) {
            LinkedList<Point> src = client.getInfoReader().getReadedPoints();
            writeKMLHeader(kml);
            if (client.getTransType() == TransType.WGS_TO_WGS) {
                for (Point p : src) {
                    kml.write(String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                            "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", p.name, p.y, p.x, p.h));
                }
            } else if (client.getTransType() == TransType.MSK_TO_WGS || client.getTransType() == TransType.WGS_TO_MSK) {
                for (Point p : src) {
                    Point point = transformator.transformOnePoint(p);
                    kml.write(String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                            "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", point.name, point.y, point.x, point.h));
                }
            } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                for (Point p : src) {
                    transformator.initExtraTransformation();
                    Point point = transformator.twoStepTransform(p);
                    kml.write(String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                            "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", point.name, point.y, point.x, point.h));
                }
            }
            kml.write("</Document>\n</kml>");
        }
    }

    private void writeCsvFromDxf() throws IOException, Proj4jException {
        try (Writer csv = new OutputStreamWriter(new FileOutputStream(output), "windows-1251")) {
            LinkedList<Point> points = client.getInfoReader().getFromDXF().getBlocks();
            LinkedList<Polyline> plines = client.getInfoReader().getFromDXF().getPlines();
            if (client.getTransType() == TransType.MSK_TO_WGS) {
                for (Point p : points) {
                    Point res = transformator.transformOnePoint(p);
                    csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, p.h));
                }
                int i = 1;
                for (Polyline pline : plines) {
                    csv.write(String.format("Polyline N %d\n", i));
                    int j = 1;
                    for (Point point : pline.getPline()) {
                        Point res = transformator.transformOnePoint(point);
                        csv.write(String.format("Vertex N %d;%s;%s;%s\n", j++,res.x, res.y, point.h));
                    }
                    i++;
                }
            } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                transformator.initExtraTransformation();
                for (Point p : points) {
                    Point res = transformator.twoStepTransform(p);
                    csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, p.h));
                }
                int i = 1;
                for (Polyline pline : plines) {
                    csv.write(String.format("Polyline N %d\n", i));
                    int j = 1;
                    for (Point point : pline.getPline()) {
                        Point res = transformator.twoStepTransform(point);
                        csv.write(String.format("Vertex N %d;%s;%s;%s\n", j++,res.x, res.y, point.h));
                    }
                    i++;
                }
            }
        }
    }

    private void writeGPXFromDxf() throws IOException, Proj4jException {
        try (Writer gpx = new OutputStreamWriter(new FileOutputStream(output))) {
            writeGPXHeader(gpx);
            LinkedList<Point> points = client.getInfoReader().getFromDXF().getBlocks();
            LinkedList<Polyline> plines = client.getInfoReader().getFromDXF().getPlines();
            if (client.getTransType() == TransType.MSK_TO_WGS) {
                for (Point p : points) {
                    Point res = transformator.transformOnePoint(p);
                    gpx.write(String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", res.x, res.y, res.name, res.name));
                }
                for (Polyline pline : plines) {
                    gpx.write("<trk><name>injgeo</name><trkseg>");
                    for (Point point : pline.getPline()) {
                        Point res = transformator.transformOnePoint(point);
                        gpx.write(String.format("<trkpt lat=\"%s\" lon=\"%s\"><ele>%s</ele></trkpt>", res.x, res.y, res.h));
                    }
                    gpx.write("</trkseg></trk>");
                }
            } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                transformator.initExtraTransformation();
                for (Point p : points) {
                    Point res = transformator.twoStepTransform(p);
                    gpx.write(String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", res.x, res.y, res.name, res.name));
                }
                for (Polyline pline : plines) {
                    gpx.write("<trk><name>injgeo</name><trkseg>");
                    for (Point point : pline.getPline()) {
                        Point res = transformator.twoStepTransform(point);
                        gpx.write(String.format("<trkpt lat=\"%s\" lon=\"%s\"><ele>%s</ele></trkpt>", res.x, res.y, res.h));
                    }
                    gpx.write("</trkseg></trk>");
                }
            }
            gpx.write("</gpx>");
        }
    }

    private void writeKMLFromDxf() throws IOException, Proj4jException {
        try (Writer kml = new OutputStreamWriter(new FileOutputStream(output))) {
            writeKMLHeader(kml);
            LinkedList<Point> points = client.getInfoReader().getFromDXF().getBlocks();
            LinkedList<Polyline> plines = client.getInfoReader().getFromDXF().getPlines();
            if (client.getTransType() == TransType.MSK_TO_WGS) {
                for (Point p : points) {
                    Point res = transformator.transformOnePoint(p);
                    kml.write(String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                            "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", res.name, res.y, res.x, res.h));
                }
                int i = 1;
                for (Polyline pline : plines) {
                    kml.write(String.format("<Placemark><name>78</name><description>Unknown Area Type&#60;br&#62;styleUrl: #area%d</description>", i));
                    kml.write("<Style><LineStyle><color>A6000000</color><width>2</width></LineStyle></Style>");
                    kml.write(String.format("<LineString><extrude>%d</extrude><coordinates>", i));
                    for (Point point : pline.getPline()) {
                        Point res = transformator.transformOnePoint(point);
                        kml.write(String.format("%s,%s,%s ", res.y, res.x, res.h));
                    }
                    kml.write("</coordinates></LineString></Placemark>\n");
                    i++;
                }
            } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                transformator.initExtraTransformation();
                for (Point p : points) {
                    Point res = transformator.twoStepTransform(p);
                    kml.write(String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                            "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", res.name, res.y, res.x, res.h));
                }
                int i = 1;
                for (Polyline pline : plines) {
                    kml.write(String.format("<Placemark><name>78</name><description>Unknown Area Type&#60;br&#62;styleUrl: #area%d</description>", i));
                    kml.write("<Style><LineStyle><color>A6000000</color><width>2</width></LineStyle></Style>");
                    kml.write(String.format("<LineString><extrude>%d</extrude><coordinates>", i));
                    for (Point point : pline.getPline()) {
                        Point res = transformator.twoStepTransform(point);
                        kml.write(String.format("%s,%s,%s ", res.y, res.x, res.h));
                    }
                    kml.write("</coordinates></LineString></Placemark>\n");
                    i++;
                }
            }
            kml.write("</Document>\n</kml>");
        }
    }
}
