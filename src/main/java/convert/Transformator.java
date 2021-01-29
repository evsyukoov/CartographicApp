package convert;//import com.sun.deploy.security.SelectableSecurityManager;
//import com.sun.javafx.tools.packager.Log;
//import jdk.internal.org.jline.utils.Log;
import org.osgeo.proj4j.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class Transformator {
    private String params;
    private CoordinateTransform         transformation;

    //точки от клиента
    private LinkedList<Point> input;



    //точки переведенные
    private String result;

    private final static String WGS84 = "EPSG:4326";


    public Transformator(String params, LinkedList<Point> input) {
        this.params = params;
        this.input = input;
        result = "";
    }

    public int      transform()
    {
        Point p;
        if (initTransformation() == 0)
            return (0);
        System.out.printf("Param: %s\n", params);
        for (Point point : input) {
            if ((p = transformOnePoint(point)) != null)
                result += String.format("%s   ,   %s  ,  %s  ,  %s\n", point.name, p.x, p.y, point.h);
            else
                return (0);
        }

        return (1);
    }

    public File transformToGPX()
    {
        return null;
    }

    public File transformToKML()
    {
        Point p;
        StringBuilder result;
        File file = new File("./resources/send/1.kml");
        try {
            FileWriter fw = new FileWriter(file);
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://earth.google.com/kml/2.0\"><Document><Style id=\"z1\"><IconStyle><scale>1.2</scale><color>ffFFFFFF</color><Icon><href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon></IconStyle></Style>\n");
            for (Point point : input) {
                if ((p = transformOnePoint(point)) != null) {
                    String line = String.format("<Placemark><name>%s</name><description></description><stileUrl>#z1</stileUrl>" +
                            "<Point><coordinates>%s,%s</coordinates></Point></Placemark>\r\n", point.name, p.y, p.x);
                    fw.write(line);
                    //break;

                }
            }
            fw.write("</Document>\n</kml>");
            fw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return (null);
        }
        return (file);
    }

    private int       initTransformation()
    {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem src;
        CoordinateReferenceSystem target;

        //System.out.printf("Params: %s\n", params);
        try {
            src = factory.createFromParameters(null, params.substring(0, params.length() - 2));
            target = factory.createFromName(WGS84);
        }
        catch (Proj4jException e)
        {
            //Log.debug(e);
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
            //Log.debug(e);
            return (null);
        }
        return (tgt);
    }

    public String getOutput() {
        return result;
    }

}
