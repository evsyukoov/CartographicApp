package convert;//import com.oracle.tools.packager.Log;
import exceptions.WrongFileFormatException;
import bot.enums.InputCoordinatesType;
import com.github.fracpete.gpsformats4j.Convert;
import com.github.fracpete.gpsformats4j.formats.CSV;
import com.github.fracpete.gpsformats4j.formats.KML;
import com.opencsv.CSVParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoReader
{
    private int transformType; //0 - из Плоской СК в WGS, 1 - из WGS
    private  File input;
    private File output;
    private InputCoordinatesType inputCoordinatesType;
    private long    id;
    private String extension;
    String text;
    private LinkedList<Point> readedPoints;

    private boolean isDxf;

    DXFConverter fromDXF;

    String kmlDIR;

    public InfoReader(File file, long id, String extension) {
        this.input = file;
        this.id = id;
        this.readedPoints = new LinkedList<Point>();
        this.extension = extension;
        transformType = 0;
    }

    public InputCoordinatesType getInputCoordinatesType() {
        return inputCoordinatesType;
    }

    public InfoReader(String text) {
        this.text = text;
        this.readedPoints = new LinkedList<Point>();
    }

    public void readText() throws Exception {
        String []arr = text.split("\n");
        boolean flag = false;
        int type = 0;
        Point p;
        for (int i = 0;i < arr.length; i++)
        {
            type = transformType;
            if ((p = parseLine(arr[i])) != null) {
                if (flag && type != transformType)
                    throw new WrongFileFormatException(String.format
                            ("Различные типы координат в файле. Проверьте строку %s\n", arr[i]));
                readedPoints.add(p);
            }
            flag = true;
        }
        inputCoordinatesType = transformType == 1 ? InputCoordinatesType.WGS : InputCoordinatesType.MSK;
    }

    public DXFConverter getFromDXF() {
        return fromDXF;
    }

    public void readFile() throws Exception {
        BufferedReader fr;
        int type = 0;
        String line;
        boolean flag = false;
        fr = new BufferedReader(new FileReader(input));
        while ((line = fr.readLine()) != null) {
            if (line.isEmpty())
                continue;
            type = transformType;
            Point point = parseLine(line);
            //если в файле встретились различные типы координат
            if (flag && type != transformType)
                throw new WrongFileFormatException(String.format
                        ("Различные типы координат в файле. Проверьте строку %s\n", line));
            readedPoints.add(point);

            flag = true;
        }
        inputCoordinatesType = transformType == 1 ? InputCoordinatesType.WGS : InputCoordinatesType.MSK;
    }

    public InfoReader() {
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
    public Point     parseLine(String line) throws Exception {
        String[] splitted = line.split("\\s*;\\s*");
        double h = 0;
        double x;
        double y;
        if (splitted.length > 4 || splitted.length < 3)
            throw new WrongFileFormatException(
                    String.format("Неверный формат строки. Строка: %s", line));
        if (isWGS(splitted[1], splitted[2]))
            transformType = 1;
        else
            transformType = 0;
        try {
            x = Double.parseDouble(splitted[1]);
            y = Double.parseDouble(splitted[2]);
            if (splitted.length == 4) {
                h = Double.parseDouble(splitted[3]);
            }
        } catch (NumberFormatException e) {
            throw new WrongFileFormatException(String.format("Ошибочный текст, строка: %s", line), e);
        }
        return (new Point(splitted[0], x, y, h));
    }

    public int getTransformType() {
        return transformType;
    }

    public LinkedList<Point> getReadedPoints() {
        return readedPoints;
    }

    private void readFromKML(File input) throws Exception  {
        Convert convert = new Convert();
        convert.setInputFile(input);
        convert.setInputFormat(KML.class);
        File out = new File(kmlDIR + id);
        out.createNewFile();
        convert.setOutputFile(out);
        convert.setOutputFormat(CSV.class);
        String msg = convert.execute();
        if (msg != null || out.length() == 0) {
            out.delete();
            throw new WrongFileFormatException("Некорректный kml-файл");
        }
        CSVParser parser = new CSVParser();
        String line = null;
        try (BufferedReader bfr = new BufferedReader(new FileReader(out))) {
            while ((line = bfr.readLine()) != null)
            {
                if (!line.startsWith("Track,Time,Latitude,Longitude,Elevation")) {
                    String []arr = parser.parseLine(line);
                    readedPoints.add(new Point(arr[0], Double.parseDouble(arr[3]), Double.parseDouble(arr[2]), Double.parseDouble(arr[4])));
                }
            }
            out.delete();
        }
        catch (NumberFormatException e) {
            throw new WrongFileFormatException(
                    String.format("Неверный формат точки: %s", line), e);
        }
    }

    private void     createTmpDirectory() throws IOException
    {
        Path path = Paths.get(kmlDIR);
        if (Files.notExists(path))
            Files.createDirectory(path);
    }

    private void     readFromKMZ() throws Exception {
        Archivator arch = new Archivator(input, kmlDIR);
        arch.extractFile();
        ArrayList<File> extracted = arch.getFromArchive();
        for (File file : extracted) {
            readFromKML(file);
            file.delete();
        }
    }

    public  void run() throws Exception {
        if (extension.equalsIgnoreCase("txt") || extension.equalsIgnoreCase("csv")) {
            readFile();
            //input.delete();
        }
        else if (extension.equalsIgnoreCase("kml"))
        {
            kmlDIR = "./src/main/resources/uploaded/" + id + "/";
            createTmpDirectory();
            readFromKML(input);
            inputCoordinatesType = InputCoordinatesType.WGS;
            //input.delete();
        }
        else if (extension.equalsIgnoreCase("KMZ"))
        {
            kmlDIR = "./src/main/resources/uploaded/" + id + "/";
            createTmpDirectory();
            readFromKMZ();
            //input.delete();
            inputCoordinatesType = InputCoordinatesType.WGS;
        }
        else if (extension.equalsIgnoreCase("dxf"))
        {
            isDxf = true;
            inputCoordinatesType = InputCoordinatesType.MSK;
            DXFConverter fromDXF = new DXFConverter(input.getAbsolutePath());
            fromDXF.parseDXF();
            this.fromDXF = fromDXF;
            //input.delete()
        }
        else {
            throw new WrongFileFormatException(String.format("Неизвестный формат файла"));
        }
    }

    public boolean isDxf() {
        return isDxf;
    }

    public void print(){
        for (Point p : readedPoints) {
            System.out.printf("%s  %f  %f  %f\n", p.name, p.x, p.y, p.h);
        }
    }
}


