import Helper.Helper;

import java.io.File;

public class Main {
    public static void main(String[] args) {

        //Test Library

//        File file = new File("./hz");
//        Transformator transformator = new Transformator("+proj=tmerc +lat_0=0 +lon_0=114.45 +k=1 +x_0=2400000 +y_0=-5912900.566 " +
//                "+ellps=krass +towgs84=23.57,-140.95,-79.8,0,0.35,0.79,-0.22 " +
//                "+units=m +no_defs");
//        Converter rd = new Converter(file, transformator);
//        if (rd.readFile() == 1)
//            System.out.println(rd);

        //Test parsing
        File file = new File("./projections.txt");
        Helper help = new Helper();
        help.read(file);

    }
}
