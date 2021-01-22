package Helper;

//класс для загрузки проекций в БД

import com.oracle.tools.packager.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public class Helper {
    LinkedList<SystemParam> params;

    public Helper() {
        params = new LinkedList<SystemParam>();
    }

    public int    read(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    SystemParam sp = parseLine(line);
                    if (sp != null) {
                        System.out.println(sp);
                        params.add(sp);
                    }
                }
            }
        }
        catch(IOException e)
        {
            Log.debug(e);
            return (0);
        };
        return (1);
    }

    public  SystemParam    parseLine(String line)
    {
        SystemParam param = null;
        String tofind  = "+title= ";
        int pos = line.indexOf(tofind);
        line = line.substring(pos + tofind.length());
        if (line.startsWith("МСК")) {

            int zonePos = line.indexOf(" ");
            String sk = line.substring(line.indexOf('-') + 1, zonePos);
            String zone = line.substring(zonePos + 1, zonePos + 7);
            int z = Integer.parseInt(zone.split("\\s+")[1]);
            return (new SystemParam(line.substring(line.indexOf('+')), sk,"MSK", z));

        }
        else if (line.startsWith("Московская"))
        {
            return (new SystemParam(line.substring(line.indexOf('+')),"Moscow","MGGT", 1));
        }
        return null;
    }

    public LinkedList<SystemParam> getParams() {
        return params;
    }

    @Override
    public String toString() {
        String res = "";
        for(int i = 0; i < params.size(); i++)
            if (params.get(i) != null)
                res += params.get(i);
        return (res);
    }
}
