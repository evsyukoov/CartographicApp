package convert;//import com.sun.deploy.security.SelectableSecurityManager;
//import com.sun.javafx.tools.packager.Log;
//import jdk.internal.org.jline.utils.Log;
import org.osgeo.proj4j.*;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class Transformator {
    private String params;
    private CoordinateTransform         transformation;
    private String outputFileName;

    //точки от клиента
    private LinkedList<Point> input;

    //трансформированные точки
    private LinkedList<Point> output;

    //список сформированных файлов на отправку

    private ArrayList<File> files;


    private final static String WGS84 = "EPSG:4326";


    public Transformator(String params, LinkedList<Point> input, String outputFileName) {
        this.outputFileName = outputFileName;
        this.params = params;
        this.input = input;
        files = new ArrayList<File>(3);
    }

    private void writeHeaders(Writer fw2, Writer fw3) throws IOException
    {
        fw2.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://earth.google.com/kml/2.0\">" +
                "<Document><Style id=\"z1\"><IconStyle><scale>1.2</scale><color>ffFFFFFF</color><Icon>" +
                "<href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon></IconStyle></Style>\n");
        fw3.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx\nxmlns=\"http://www.topografix.com/GPX/1/1\"" +
                "\nversion=\"1.1\"\ncreator=\"InjGeo_bot\"\nxmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/WaypointExtension/v1 \">\n");

    }

    private void    writeLine(Writer fw2, Writer fw3, Point p) throws IOException
    {
        String line = String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                "<Point><coordinates>%s,%s</coordinates></Point></Placemark>\r\n", p.name, p.y, p.x);
        fw2.write(line);
        String line2 = String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", p.x, p.y, p.name, p.name);
        fw3.write(line2);
    }

    //трансформируем и сразу создаем все 3 файла (csv, kml и gpx)
    public int     transform()
    {
        Point p;
        if (initTransformation() == 0)
            return (0);
        File file1 = new File(outputFileName + ".csv");
        File file2 = new File(outputFileName + ".kml");
        File file3 = new File(outputFileName + ".gpx");
        files.add(file1);
        files.add(file2);
        files.add(file3);
        try {
            Writer csv = new OutputStreamWriter(new FileOutputStream(file1));
            //FileWriter csv = new FileWriter(file1);
            Writer kml = new OutputStreamWriter(new FileOutputStream(file2));
            //FileWriter gpx = new FileWriter(file3);
            Writer gpx = new OutputStreamWriter(new FileOutputStream(file3));
            writeHeaders(kml, gpx);
            for (Point point : input) {
                if ((p = transformOnePoint(point)) != null) {
                    writeLine(kml, gpx, p);
                    csv.write(String.format("%s,%s,%s,%s\n", p.name, p.x, p.y, p.h));
                }
                else
                    return (0);
            }
            kml.write("</Document>\n</kml>");
            gpx.write("</gpx>");
            csv.close();
            gpx.close();
            kml.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return (1);
    }

    private int       initTransformation()
    {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem src;
        CoordinateReferenceSystem target;

        try {
            src = factory.createFromParameters(null, params.substring(0, params.length() - 2));
            target = factory.createFromName(WGS84);
        }
        catch (Proj4jException e)
        {
            return (0);
        }
        transformation = new CoordinateTransformFactory().createTransform(src, target);
        return (1);
    }

    private     Point    transformOnePoint(Point point)
    {
        ProjCoordinate result = new ProjCoordinate();
        Point tgt;
        ProjCoordinate src;
        try {
            src = new ProjCoordinate(point.y, point.x);
            transformation.transform(src, result);
            tgt = new Point(point.name, result.y, result.x);
        }
        catch (Exception e)
        {
            return (null);
        }
        return (tgt);
    }

    public ArrayList<File> getFiles() {
        return files;
    }
}