package convert;//import com.oracle.tools.packager.Log;
//import jdk.internal.org.jline.utils.Log;
import java.io.*;
        import java.util.LinkedList;
import java.util.List;

public class Converter {
    private  File input;
    private File output;
    private long    id;
    String text;
    private List<Point> convertedPoints;
    private LinkedList<Point> readedPoints;
    Transformator transformator;

    public Converter(File file) {
        this.input = file;
        this.readedPoints = new LinkedList<Point>();
    }

    public Converter(String text) {
        this.text = text;
        this.readedPoints = new LinkedList<Point>();
    }

    public int readLine()
    {
        Point point;
        if ((point = parseLine(text)) != null)
        {
            readedPoints.add(point);
            return (1);
        }
        return (0);
    }

    public int readFile() {
        BufferedReader fr = null;
        try {
            fr = new BufferedReader(new FileReader(input));
        } catch (FileNotFoundException e) {
            return (0);
        }
        String line;
        try {
            while ((line = fr.readLine()) != null) {
                Point point;
                System.out.println(line);
                if (line.isEmpty())
                    continue;
                if ((point = parseLine(line)) != null) {
                    readedPoints.add(point);
                }
                else
                    return (0);
            }
        } catch (IOException e) { ;
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
        double h = 0;
        double x;
        double y;
        if (splitted.length > 4 || splitted.length < 3)
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

    public LinkedList<Point> getReadedPoints() {
        return readedPoints;
    }
}


