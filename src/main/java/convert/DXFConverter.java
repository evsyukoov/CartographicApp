package convert;
import com.google.inject.internal.cglib.core.$Block;

import java.io.*;
import java.util.LinkedList;

public class DXFConverter {
    final static String CPLUSPLUS_BINARY = "/Users/denis/Denis/DxfParser/converter";

    LinkedList<Point> blocks;
    LinkedList<Polyline> plines;

    public LinkedList<Point> getBlocks() {
        return blocks;
    }

    public LinkedList<Polyline> getPlines() {
        return plines;
    }

    String dxfName;

    public DXFConverter(String dxfName) {
        this.dxfName = dxfName;

    }

    private Point parsePoint(String pointText)
    {
        String []pointArr = pointText.split(",");
        Point p = new Point(pointArr[0], Double.parseDouble(pointArr[2]), Double.parseDouble(pointArr[1]));
        return p;
    }

    private void parseLine(String line)
    {
        if (line.startsWith("blocks"))
        {
            if (line.split(":")[1].equals("false"))
                blocks = null;
            else
            {
                blocks = new LinkedList<>();
                String[] text = line.substring(line.indexOf('{') + 1, line.indexOf('}')).split(";");
                for (String s : text)
                    blocks.push(parsePoint(s));
            }
        }
        else if (line.startsWith("plines"))
        {
            if (line.split(":")[1].equals("false"))
                plines = null;
            else
            {
                plines = new LinkedList<>();
                String []polilines = line.substring(line.indexOf('{') + 1, line.indexOf('}')).split("\\s");
                for (String s : polilines) {
                    Polyline pline = new Polyline();
                    String[] text = s.split(";");
                    for (String pointText : text) {
                        pline.addPoint(parsePoint(pointText));
                    }
                    plines.add(pline);
                }
            }
        }
    }

    //сам конвертер написан на с++ с использованием dxflib
    public int   parseDXF()
    {
        Runtime r =Runtime.getRuntime();
        Process p = null;
        String cmd[]={CPLUSPLUS_BINARY, dxfName};
        try{
            p = r.exec(cmd);
            while (p.isAlive());

            if (p.exitValue() == 2) {
                System.out.println("There is no blocks and polylines in dxf");
                return (2);
            }
            else if (p.exitValue() == 0)
            {
                System.out.println("Problems with parsing on server");
                return (-1);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String res;
            while((res = br.readLine()) != null)
                parseLine(res);
            br.close();
        }
        catch(Exception e)
        {
            System.out.println("Problems with parsing on server exception");
            return (-1);
        }
        return (1);
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

//    public static void main(String[] args)  {
//        DXFConverter dxfConverter = new DXFConverter("/Users/denis/Desktop/test3.dxf");
//        //dxfConverter.print();
//
//
//    }
}
