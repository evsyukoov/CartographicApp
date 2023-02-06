package ru.evsyukoov.transform.convert;

import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import java.io.*;
import java.util.LinkedList;

public class DXFConverter {
    final static String CPLUSPLUS_BINARY = "/Users/denis/Denis/DxfParser/converter";

    LinkedList<Point> blocks;
    LinkedList<Polyline> plines;
    Polyline pline;

    public LinkedList<Point> getBlocks() {
        return blocks;
    }

    public LinkedList<Polyline> getPlines() {
        return plines;
    }

    String dxfName;

    public DXFConverter(String dxfName) {
        this.dxfName = dxfName;
        plines = new LinkedList<>();
        blocks = new LinkedList<>();

    }

    private Point parsePoint(String pointText)
    {
        String []pointArr = pointText.split(",");
        Point p = new Point(pointArr[0], Double.parseDouble(pointArr[2]), Double.parseDouble(pointArr[1]), Double.parseDouble(pointArr[3]));
        return p;
    }

    private void parseLine(String line) throws WrongFileFormatException
    {
        if (line.equals("empty")) {
            throw new WrongFileFormatException("Нет блоков и замкнутых полилиний в чертеже");
        }
        else if (line.startsWith("bl"))
            blocks.push(parsePoint(line.substring(line.indexOf(',') + 1)));
        else if (line.equals("endBlocks"))
            pline = new Polyline();
        else if (line.equals("endPline"))
        {
            plines.add(pline);
            pline = new Polyline();
        }
        else if (Character.isDigit(line.charAt(0)))
            pline.addPoint(parsePoint(line));
    }
    //сам конвертер написан на с++ с использованием dxflib
    public void parseDXF() throws Exception {
        Runtime r = Runtime.getRuntime();
        Process p = null;
        String[] cmd = new String[]{CPLUSPLUS_BINARY, dxfName};
        p = r.exec(cmd);
        InputStream is = p.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String res;
        while ((res = br.readLine()) != null) {
            parseLine(res);
        }

        is.close();
        br.close();
        int exitValue = p.waitFor();
        if (exitValue == 0) {
            throw new WrongFileFormatException("Неизвестная ошибка во время парсинга dxf файла. Сообщите техподдержке");
        }
    }

    public void    print()
    {
        if (this.getBlocks() != null) {
            for (Point point : this.getBlocks()) {
                System.out.printf("name: %s, x: %f, y: %f\n", point.name, point.x, point.y);
            }
        }
        System.out.println("\n\n");
        if (this.getPlines() != null)
        {
            for (Polyline polyline : this.getPlines()) {
                for (Point point : polyline.getPline()) {
                    System.out.printf("name: %s, x: %f, y: %f\n", point.name, point.x, point.y);
                }
                System.out.println("\n\n");
            }
        }
    }
}
