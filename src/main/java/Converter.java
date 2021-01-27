//import com.oracle.tools.packager.Log;
import org.osgeo.proj4j.ProjCoordinate;
//import jdk.internal.org.jline.utils.Log;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Converter {
    private  File input;
    private File output;
    private long    id;
    private List<Point> convertedPoints;
    Transformator transformator;

    public Converter(File file, Transformator transformator, long id) {
        this.input = file;
        this.id = id;
        convertedPoints = new ArrayList<Point>();
        this.transformator = transformator;
    }

    private int readFile() {
        if (transformator.initTransformation() == 0) {
            //Log.debug("Bad transformation parametrs");
            return (0);
        }
        BufferedReader fr = null;
        try {
            fr = new BufferedReader(new FileReader(input));
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
                        return (-1);
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

    public int    convert() throws IOException {
        if (readFile() == -1)
            return (-1);
        output = new File("./output/" + id);
        FileWriter fw = new FileWriter(output);
        for (Point p : convertedPoints) {
            fw.write(String.format("%s, %f, %f, %f", p.name, p.x, p.y, p.h));
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

    public File getOutput() {
        return output;
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


