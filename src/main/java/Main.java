import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.datum.Datum;
import org.osgeo.proj4j.datum.Ellipsoid;
import org.osgeo.proj4j.proj.MercatorProjection;
import org.osgeo.proj4j.proj.Projection;
import org.osgeo.proj4j.proj.TransverseMercatorProjection;
import org.osgeo.proj4j.units.Unit;

import java.awt.geom.Point2D;

public class Main {
    public static void main(String[] args) {
        //Check library logic:  FROM MSK-14 z2(Yakutia) to WGS-84
        //FROM

//        PROJCS["MSK МСК-14 зона 2 (6 градусная) Республика Саха (Якутия)",
//                GEOGCS["Krassovsky, 1942",DATUM["unknown",SPHEROID["krass",6378245,298.3],
//        TOWGS84[23.57,-140.95,-79.8,0,0.35,0.79,-0.22]],
//        PRIMEM["Greenwich",0],                                                                    ----------------->>           TO WGS-84 (Lat, Long)
//        UNIT["degree",0.0174532925199433]],
//        PROJECTION["Transverse_Mercator"],
//                PARAMETER["latitude_of_origin",0],
//        PARAMETER["central_meridian",114.45],
//        PARAMETER["scale_factor",1],
//        PARAMETER["false_easting",2400000],
//        PARAMETER["false_northing",-5912900.566],
//        UNIT["Meter",1]]


        Datum sk95 = new Datum("SK-95", 23.57, -140.95, -79.8, 0.0, 0.0000035,0.0000079, 0.99999978, Ellipsoid.KRASSOVSKY, "My datum");
        Projection src = new TransverseMercatorProjection(Ellipsoid.KRASSOVSKY, 114.45 * Math.PI / 180 , 0, 1, 2400000, -5912900.566);

        CoordinateReferenceSystem msk14z2 = new CoordinateReferenceSystem("MSK-14z2 Yakutia", null, sk95, src); //2 пара метр, видимо служебная инфа
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem wgs84 = factory.createFromName("EPSG:4326");
        CoordinateReferenceSystem my = factory.createFromParameters(null,
                "+proj=tmerc +lat_0=0 +lon_0=114.45 +k=1 +x_0=2400000 +y_0=-5912900.566 +ellps=krass +towgs84=23.57,-140.95,-79.8,0,0.35,0.79,-0.22 +units=m +no_defs");
        //System.out.println(my.getDatum());
        //первый аргумент - восток, второй - север
        ProjCoordinate point = new ProjCoordinate(2303248.01, 703919.89);
        ProjCoordinate result = new ProjCoordinate();

        BasicCoordinateTransform transformator = new BasicCoordinateTransform(msk14z2, wgs84);

        transformator.transform(point, result);
        System.out.println(String.format("Lat - %f, Long - %f", result.x, result.y));
        System.out.println("Expected - 112.73456353163765, 59.65413798772589");


    }
}
