package bot.outputgenerators;

import bot.Client;
import bot.enums.OutputFileType;
import bot.enums.TransType;
import convert.Point;
import convert.Transformator;
import org.osgeo.proj4j.Proj4jException;

import java.io.*;
import java.util.LinkedList;

public class GeneratorManager {

    Client client;

    Transformator transformator;

    File output;

    public GeneratorManager(Client client) {
        this.client = client;
        transformator = new Transformator(client);
        output = new File(String.format("%s.%s", client.getSavePath(), client.getOutputFileType()));
    }

    public int run()
    {
        try {
            if (client.getOutputFileType() == OutputFileType.CSV)
                writeCSV();
            else if (client.getOutputFileType() == OutputFileType.GPX)
                writeGPX();
            else if (client.getOutputFileType() == OutputFileType.KML)
                writeKML();
        }
        catch (IOException e)
        {
            return (-1);
        }
        catch (Proj4jException e)
        {
            return (0);
        }
        return (1);
    }

    public File getOutput() {
        return output;
    }

    public void writeCSV() throws IOException, Proj4jException {
        try(Writer csv = new OutputStreamWriter(new FileOutputStream(output), "windows-1251")) {
            if (transformator != null)
                transformator.initTransformation();
            LinkedList<Point> src = client.getInfoReader().getReadedPoints();
            for (Point p : src) {
                if (client.getTransType() == TransType.WGS_TO_WGS) {
                    csv.write(String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, p.h));
                } else if (client.getTransType() == TransType.MSK_TO_WGS || client.getTransType() == TransType.WGS_TO_MSK) {
                    Point res = transformator.transformOnePoint(p);
                    csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, res.h));
                } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                    transformator.initExtraTransformation();
                    Point res = transformator.twoStepTransform(p);
                    csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, res.h));
                }
            }
        }
    }

    public void writeGPX() throws IOException, Proj4jException {
        try(Writer gpx = new OutputStreamWriter(new FileOutputStream(output), "windows-1251")) {
            if (transformator != null)
                transformator.initTransformation();
            LinkedList<Point> src = client.getInfoReader().getReadedPoints();
            gpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx\nxmlns=\"http://www.topografix.com/GPX/1/1\"" +
                    "\nversion=\"1.1\"\ncreator=\"InjGeo_bot\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/WaypointExtension/v1 \">\n");
            for (Point p : src) {
                gpx.write(String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", p.x, p.y, p.name, p.name));
            }
            gpx.write("</gpx>");
        }
    }

    public void writeKML() throws IOException, Proj4jException {
        try(Writer kml = new OutputStreamWriter(new FileOutputStream(output), "windows-1251")) {
            if (transformator != null)
                transformator.initTransformation();
            LinkedList<Point> src = client.getInfoReader().getReadedPoints();
            kml.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://earth.google.com/kml/2.0\">" +
                    "<Document><Style id=\"z1\"><IconStyle><scale>1.2</scale><color>ffFFFFFF</color><Icon>" +
                    "<href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon></IconStyle></Style>\n");
            for (Point p : src) {
                kml.write(String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                        "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", p.name, p.y, p.x, p.h));
            }
            kml.write("</Document>\n</kml>");
        }
    }
}
