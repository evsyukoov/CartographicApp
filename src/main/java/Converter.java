//import com.oracle.tools.packager.Log;
import org.osgeo.proj4j.ProjCoordinate;
//import jdk.internal.org.jline.utils.Log;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Converter {
    private final File file;
    private List<Point> convertedPoints;
    Transformator transformator;

    public Converter(File file, Transformator transformator) {
        this.file = file;
        convertedPoints = new ArrayList<Point>();
        this.transformator = transformator;
    }

    public int readFile() {
        if (transformator.initTransformation() == 0) {
            //Log.debug("Bad transformation parametrs");
            return (0);
        }
        BufferedReader fr = null;
        try {
            fr = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            //Log.debug(e);
            return (0);
        }
        String line;
        try {
            while ((line = fr.readLine()) != null) {
                Point point;
                if (line.isEmpty())
                    continue;
                if ((point = parseLine(line)) != null) {
                    Point result = transformator.transformOnePoint(point);
                    if (result == null)
                        return (0);
                    convertedPoints.add(result);
                }
                else
                    return (0);
            }
        } catch (IOException e) {
            //Log.debug(e);
            return (0);
        }
        return (1);
    }

    private Point     parseLine(String line)
    {
        String[] splitted = line.split("\\s*,\\s*");
        double h = -10000;
        double x;
        double y;
        if (splitted.length > 4)
            return null;
        try {
            x = Double.parseDouble(splitted[1]);
            y = Double.parseDouble(splitted[2]);
            if (splitted.length == 4) {
                h = Double.parseDouble(splitted[3]);
            }

        }
        catch (NumberFormatException e)
        {
            //Log.debug(e);
            return null;
        }
        return (new Point(splitted[0], x, y, h));
    }

    @Override
    public String toString() {
        String res = "";
        for (Point point : convertedPoints) {
            res += String.format("%s:%f:%f:%f\n", point.name, point.x, point.y, point.h);
        }
        return (res);
    }
}


