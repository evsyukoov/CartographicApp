import Helper.Helper;
import dao.DAO;
import dao.DownloadDAO;
import dao.ClientDAO;

import java.io.File;
import java.io.IOException;


//public class Main {
//    public static void main(String[] args) {
//        //Test Library
//
//
////        File file = new File("./hz");
////        Transformator transformator = new Transformator("+proj=tmerc +ellps=krass +towgs84=24,-123,-94,0.02,-0.25,-0.13,1.1 +units=m +lon_0=114.45 +lat_0=0 +k_0=1 +x_0=2400000 +y_0=-5912900.566");
////        Converter rd = new Converter(file, transformator);
////        if (rd.readFile() == 1)
////            System.out.println(rd);
//
//
//        //Test parsing
////        File file = new File("./projections.txt");
////        Helper help = new Helper();
////        help.read(file);
//
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
//}
