package bot.outputgenerators;

import bot.enums.TransType;
import convert.Point;
import org.osgeo.proj4j.Proj4jException;

import java.io.*;
import java.util.LinkedList;

public class CSVgenerator  extends GeneratorManager {

    public void write() throws IOException, Proj4jException {
        Writer csv = new OutputStreamWriter(new FileOutputStream(output), "windows-1251");
        if (transformator != null)
            transformator.initTransformation();
        LinkedList<Point> src = client.getInfoReader().getReadedPoints();
        for (Point p : src) {
            if (client.getTransType() == TransType.WGS_TO_WGS)
            {
                csv.write(String.format("%s;%s;%s;%s\n", p.name, p.x, p.y, p.h));
            }
            else if (client.getTransType() == TransType.MSK_TO_WGS || client.getTransType() == TransType.WGS_TO_MSK)
            {
                Point res = transformator.transformOnePoint(p);
                csv.write(String.format("%s;%s;%s;%s\n", res.name, res.x, res.y, res.h));
            }
            else if (client.getTransType() == TransType.MSK_TO_MSK)
            {

            }

        }
    }
}
