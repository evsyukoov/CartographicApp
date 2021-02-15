package convert;//import com.oracle.tools.packager.Log;
//import jdk.internal.org.jline.utils.Log;
import com.github.fracpete.gpsformats4j.Convert;
import com.github.fracpete.gpsformats4j.formats.CSV;
import com.github.fracpete.gpsformats4j.formats.KML;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Converter
{
    private int transformType; //0 - из Плоской СК в WGS, 1 - из WGS
    private  File input;
    private File output;
    private long    id;
    private String extension;
    String text;
    private LinkedList<Point> readedPoints;
    Transformator transformator;

    String kmlDIR;

    public Converter(File file, long id, String extension) {
        this.input = file;
        this.id = id;
        this.readedPoints = new LinkedList<Point>();
        this.extension = extension;
        transformType = 0;
    }

    public Converter(String text) {
        this.text = text;
        this.readedPoints = new LinkedList<Point>();
    }

    public int readText()
    {
        String []arr = text.split("\n");
        boolean flag = false;
        int type = 0;
        Point p;
        for (int i = 0;i < arr.length; i++)
        {
            type = transformType;
            if ((p = parseLine(arr[i])) != null)
            {
                if (flag && type != transformType)
                    return (0);
                readedPoints.add(p);
            }
            else
                return (0);
            flag = true;
        }
        return (1);
    }

    public int readFile() {
        BufferedReader fr;
        int type = 0;
        String line;
        boolean flag = false;
        try {
            fr = new BufferedReader(new FileReader(input));
            while ((line = fr.readLine()) != null) {
                Point point;
                if (line.isEmpty())
                    continue;
                type = transformType;
                if ((point = parseLine(line)) != null) {
                    //если в файле встретились различные типы координат
                    if (flag && type != transformType)
                        return (0);
                    readedPoints.add(point);
                }
                else
                    return (0);

                flag = true;
            }
        } catch (IOException e) { ;
            return (-1);
        }
        return (1);
    }

    //смотрим какие координаты в текстовике: WGS-84 или плоские
    private Boolean matchWGS(String s)
    {
        Pattern p = Pattern.compile("^\\d{1,3}\\.?\\d*$");
        Matcher m = p.matcher(s);
        boolean res;
        if ((res = m.find()))
        {
            if (!s.contains("."))
            {
                p = Pattern.compile("^\\d{1,3}$");
                m = p.matcher(s);
                return m.find();
            }
        }
        return res;
    }

    private Boolean isWGS(String s1, String s2)
    {
        return matchWGS(s1) && matchWGS(s2);

    }

    // читаем и парсим одну строчку
    private Point     parseLine(String line)
    {
        String[] splitted = line.split("\\s*;\\s*");
        double h = 0;
        double x;
        double y;
        if (splitted.length > 4 || splitted.length < 3)
            return null;
        try {
            if (isWGS(splitted[1], splitted[2]))
                transformType = 1;
            else
                transformType = 0;
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

    public int getTransformType() {
        return transformType;
    }

    public LinkedList<Point> getReadedPoints() {
        return readedPoints;
    }

    private int readFromKML(File input){
        Convert convert = new Convert();
        convert.setInputFile(input);
        convert.setInputFormat(KML.class);
        File out = new File(kmlDIR + id);
        try {
            out.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
            return (-1);
        }
        convert.setOutputFile(out);
        convert.setOutputFormat(CSV.class);
        String msg = convert.execute();
        if (msg != null)
            return (0);
        if (out.length() == 0) {
            out.delete();
            return (0);
        }
        try (BufferedReader bfr = new BufferedReader(new FileReader(out))) {
            String line;
            while ((line = bfr.readLine()) != null)
            {
                if (!line.startsWith("Track,Time,Latitude,Longitude,Elevation")) {
                    String []arr = line.split("\\s*,\\s*");
                    readedPoints.add(new Point(arr[0], Double.parseDouble(arr[3]), Double.parseDouble(arr[2]), Double.parseDouble(arr[4])));
                }
                out.delete();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return (-1);
        }
        return (1);
    }

    private int     createTmpDirectory()
    {
        Path path = Paths.get(kmlDIR);
        try {
            if (Files.notExists(path))
                Files.createDirectory(path);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return (0);
        }
        return (1);
    }

    private int     readFromKMZ() {
            Archivator arch = new Archivator(input, kmlDIR);
            if (arch.extractFile() == 0)
                return (-1);
            ArrayList<File> extracted = arch.getFromArchive();
            for (File file : extracted) {
                int ret = readFromKML(file);
                file.delete();
                if (ret == -1 || ret == 0)
                    return ret;
            }
            return (1);
        }

    public  int run()
    {
        if (extension.equalsIgnoreCase("txt") || extension.equalsIgnoreCase("csv")) {
            int ret = readFile();
            input.delete();
            return(ret);
        }
        else if (extension.equalsIgnoreCase("kml"))
        {
            transformType = 1;
            kmlDIR = "./src/main/resources/uploaded/" + id + "/";
            if (createTmpDirectory() == 0)
                return (-1);
            int ret = readFromKML(input);
            input.delete();
            return (ret);
        }
        else if (extension.equalsIgnoreCase("KMZ"))
        {
            kmlDIR = "./src/main/resources/uploaded/" + id + "/";
            if (createTmpDirectory() == 0)
                return (-1);
            int ret = readFromKMZ();
            input.delete();
            transformType = 1;
            return (ret);
        }
        else
            return 0;
    }

    public void print(){
        for (Point p : readedPoints) {
            System.out.printf("%s  %f  %f  %f\n", p.name, p.x, p.y, p.h);
        }
    }
}


