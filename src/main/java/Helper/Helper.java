package Helper;

//класс для загрузки проекций в БД

//import com.oracle.tools.packager.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
    private LinkedList<SystemParam> params;

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
                        //System.out.println(sp);
                        params.add(sp);
                    }
                }
                else {
                    //System.out.println(params.size());
                    break;
                }
            }
        }
        catch(IOException e)
        {
            //Log.debug(e);
            return (0);
        };
        return (1);
    }

    private     Boolean    check1(String s)
    {
        Pattern p = Pattern.compile("\\D*\\d*\\s\\D*");
        Matcher m = p.matcher(s);
        return m.matches();
    }

    public  SystemParam    parseLine(String line)
    {
        String begin  = "+title= ";
        int posBegin = line.indexOf(begin);
        int posEnd = line.indexOf("+proj=tmerc");
        String params = line.substring(posEnd, line.length() - 2);

        line = line.substring(posBegin + begin.length(), posEnd);
        if (line.startsWith("МСК")) {
            if (check1(line))
            {
                String sk = line.substring(line.indexOf('-') + 1, line.indexOf(" "));
                return new SystemParam(params, sk + " Красноярский край", "MSK", "Зона 1");
            }
            int zonePos = line.indexOf(" ");
            int position = line.indexOf('-');
            String sk = line.substring(position + 1, zonePos);
            String zone = line.substring(zonePos + 1, zonePos + 7);
            String reg = line.substring(zonePos + 8).trim();
            if (reg.startsWith("("))
                reg = reg.substring(reg.indexOf(")") + 1).trim();
            String z = zone.split("\\s+")[1];
            return (new SystemParam(params, sk + " " + reg,"MSK", "Зона " + z));

        }
        else if (line.startsWith("Московская"))
        {
            return (new SystemParam(params,"Moscow","MGGT", "Зона 1"));
        }
        else if (line.startsWith("СК-1963"))
        {
            String[] spl = line.split(" "); //СК-1963 район А зона 3
            return new SystemParam(params, "Район " + spl[2], "SK-63", "Зона " + spl[4]);
        }
        else if (line.startsWith("Пулково 1942"))
        {
            String[] spl = line.split(" "); // Пулково 1942 зона 2 ГОСТ 2008
            return new SystemParam(params, "None", "SK-42", "Зона " + spl[3]);
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
                res += params.get(i) + "\n";
        return (res);
    }
}
