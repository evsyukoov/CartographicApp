package convert;

import Helper.Helper;
import com.github.fracpete.gpsformats4j.Convert;
import com.github.fracpete.gpsformats4j.formats.CSV;
import com.github.fracpete.gpsformats4j.formats.GPX;
import com.github.fracpete.gpsformats4j.formats.KML;
import com.github.fracpete.gpsformats4j.formats.TCX;
import dao.DAO;
import dao.DownloadDAO;
import dao.ClientDAO;
import de.micromata.opengis.kml.v_2_2_0.Kml;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;


public class Main {
    public static void main(String[] args) {

//        Converter c = new Converter(new File("./src/main/java/convert/kmzTest.kmz"), 123, ".kmz");
//        c.run();
//        c.print();
        //System.out.println(c.isWGS("6453.54", "2363.53"));
        //System.out.printf("res = %d\n", c.readFile());

        //System.out.printf("Result = %d",a );
          //Archivator zipArch = new Archivator(new File("./src/main/java/convert/kmzTest.kmz"),
//                  "./src/main/java/convert/kmz/");
          //zipArch.extractFile();
        Convert convert = new Convert();
        convert.setInputFile(new File("./src/main/java/convert/example.kml"));
        convert.setInputFormat(KML.class);
        convert.setOutputFile(new File("./src/main/java/convert/out2.csv"));
        convert.setOutputFormat(CSV.class);
        String msg = convert.execute();
 //successful if null returned:
        if (msg != null)
            System.err.println(msg);
//        //Test Library


//        File file = new File("./hz");
//        Transformator transformator = new Transformator("+proj=tmerc +ellps=krass +towgs84=24,-123,-94,0.02,-0.25,-0.13,1.1 +units=m +lon_0=114.45 +lat_0=0 +k_0=1 +x_0=2400000 +y_0=-5912900.566");
//        Converter rd = new Converter(file, transformator);
//        if (rd.readFile() == 1)
//            System.out.println(rd);


        //Test parsing
//        File file = new File("./projections.txt");
//        Helper help = new Helper();
//        help.read(file);
//        DownloadDAO dao = new DownloadDAO();
//        try {
//            dao.register();
//            dao.startConnection();
//            dao.startDownload();
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }

//        ClientDAO dao = new ClientDAO(1);
//        dao.register();
//        try {
//            dao.startConnection();
//            dao.getData();
//            System.out.println(String.format("%s %s", dao.getChoosedType(), dao.getChoosedSK()));
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//
//    }
    }
}
