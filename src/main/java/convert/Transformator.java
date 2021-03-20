package convert;//import com.sun.deploy.security.SelectableSecurityManager;
//import com.sun.javafx.tools.packager.Log;
//import jdk.internal.org.jline.utils.Log;
import org.osgeo.proj4j.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;

public class Transformator {
    private String params;
    private CoordinateTransform         transformation;
    private String outputFileName;
    private int transformType;

    private DXFConverter dxf;

    //точки от клиента
    private LinkedList<Point> input;

    //трансформированные точки
    private LinkedList<Point> output;

    //список сформированных файлов на отправку

    private ArrayList<File> files;


    private final static String WGS84 = "EPSG:4326";


    public Transformator(String params, LinkedList<Point> input, String outputFileName, int transformType) {
        this.outputFileName = outputFileName;
        this.params = params;
        this.input = input;
        this.transformType = transformType;
        files = new ArrayList<File>();
    }

    public Transformator(String params, DXFConverter dxf, String outputFileName, int transformType) {
        this.outputFileName = outputFileName;
        this.params = params;
        this.dxf = dxf;
        this.transformType = transformType;
        files = new ArrayList<File>();
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
                "<Point><coordinates>%s,%s,%s</coordinates></Point></Placemark>\r\n", p.name, p.y, p.x, p.h);
        fw2.write(line);
        String line2 = String.format("<wpt lat=\"%s\" lon=\"%s\"><name>%s</name><desc>%s</desc></wpt>\n", p.x, p.y, p.name, p.name);
        fw3.write(line2);
    }

    public int transform()
    {
        if (transformType == 0) {
            if (dxf != null)
                input = dxf.getBlocks();
            return (transformToWGS());
        }
        else
            return (transformToLocal());
    }



    private int  transformToLocal()
    {
        Point p;
        if (initTransformation() == 0)
            return (0);
        File f1 = new File(outputFileName + ".csv");
        files.add(f1);
        try {
            Writer csv = new OutputStreamWriter(new FileOutputStream(f1), "Windows-1251");
            //System.out.println(input);
            for (Point point : input)
            {
                if ((p = transformOnePoint(point)) != null)
                    csv.write(String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, point.h));
                else
                    return (0);
            }
            csv.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return (-1);
        }
        return (1);
    }

    private void     writeLineFromDXFPline(Writer csv, Writer kml, Writer gpx, Point point) throws IOException
    {
        csv.write(String.format("%s;%s;%s;%s", point.name, point.x, point.y, point.h));

    }

    //допишем в созданные csv/kml полилинии из dxf, если пользователь прислал dxf
    private int     transfromPolygonesToWGS(Writer csv, Writer kml, Writer gpx) throws IOException {
        int i = 1;
        for(Polyline pline : dxf.getPlines())
        {
            kml.write(String.format("<Placemark><name>78</name><description>Unknown Area Type&#60;br&#62;styleUrl: #area%d</description>", i));
            kml.write("<Style><LineStyle><color>A6000000</color><width>2</width></LineStyle></Style>");
            kml.write(String.format("<LineString><extrude>%d</extrude><coordinates>", i));
            csv.write(String.format("Polyline N %d\n", i));
            gpx.write("<trk><name>injgeo</name><trkseg>");
            int j = 1;
            for(Point point : pline.getPline()) {
                Point res;
                if ((res = transformOnePoint(point)) == null)
                    return (0);
                kml.write(String.format("%s,%s,%s ", res.y, res.x, res.h));
                csv.write(String.format("Vertex N %d;%s;%s;%s\n", j++,res.x, res.y, res.h));
                gpx.write(String.format("<trkpt lat=\"%s\" lon=\"%s\"><ele>%s</ele></trkpt>", res.x, res.y, res.h));
            }
            gpx.write("</trkseg></trk>");
            kml.write("</coordinates></LineString></Placemark>\n");
            i++;
        }
        return (1);
    }

    //трансформируем и сразу создаем все 3 файла (csv, kml и gpx)
    private int     transformToWGS()
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
            Writer csv = new OutputStreamWriter(new FileOutputStream(file1), "windows-1251");
            Writer kml = new OutputStreamWriter(new FileOutputStream(file2));
            Writer gpx = new OutputStreamWriter(new FileOutputStream(file3));
            writeHeaders(kml, gpx);
            if (input != null) {
                for (Point point : input) {
                    if ((p = transformOnePoint(point)) != null) {
                        writeLine(kml, gpx, p);
                        csv.write(String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, p.h));
                    } else
                        return (0);
                }
            }
            if (dxf != null && dxf.getPlines() != null) {
                if (transfromPolygonesToWGS(csv, kml, gpx) == 0)
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
            return (-1);
        }
        return (1);
    }

    private int       initTransformation()
    {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem src;
        CoordinateReferenceSystem target;

        try {
            if (transformType == 0) {
                src = factory.createFromParameters(null, params);
                target = factory.createFromName(WGS84);
            }
            else {
                target = factory.createFromParameters(null, params);
                src = factory.createFromName(WGS84);
            }
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
