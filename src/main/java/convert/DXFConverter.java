package convert;
import bot.BotState;

import java.io.*;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DXFConverter {
    private static final java.util.logging.Logger logger = Logger.getLogger(BotState.class.getName());

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

    private int parseLine(String line)
    {
        if (line.equals("empty"))
            return (0);
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
        return (1);
    }

    private void checkBlocksAndLines()
    {
        if (blocks.isEmpty())
            blocks = null;
        if (plines.isEmpty())
            plines = null;
    }

    //сам конвертер написан на с++ с использованием dxflib
    public int   parseDXF()
    {
        Runtime r = Runtime.getRuntime();
        Process p = null;
        String[] cmd = new String[]{CPLUSPLUS_BINARY, dxfName};
        try{
            p = r.exec(cmd);
            InputStream is = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String res;
            while ((res = br.readLine()) != null) {
                int ret = parseLine(res);
                if (ret == 0) {

                    return (2);
                }
            }

            is.close();
            br.close();
            checkBlocksAndLines();
            int exitValue = p.waitFor();
            if (exitValue == 0)
            {
                logger.log(Level.SEVERE, "Problems with parsing dxf on server");
                return (-1);
            }

        }
        catch(Exception e)
        {
            logger.log(Level.SEVERE, "Problems with parsing on server exception");
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
