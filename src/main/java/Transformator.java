import com.sun.deploy.security.SelectableSecurityManager;
import com.sun.javafx.tools.packager.Log;
import org.osgeo.proj4j.*;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class Transformator {
    private String params;
    private CoordinateTransform         transformation;
    private final static String WGS84 = "EPSG:4326";


    public Transformator(String params) {
        this.params = params;
    }

    public int       initTransformation()
    {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem src;
        CoordinateReferenceSystem target;

        try {
            src = factory.createFromParameters(null, params);
            target = factory.createFromName(WGS84);
        }
        catch (Proj4jException e)
        {
            Log.debug(e);
            return (0);
        }
        transformation = new CoordinateTransformFactory().createTransform(src, target);
        return (1);
    }

    public     Point    transformOnePoint(Point point)
    {
        ProjCoordinate result = new ProjCoordinate();
        Point tgt;
        ProjCoordinate src;
        try {
            src = new ProjCoordinate(point.y, point.x);
            transformation.transform(src, result);
            tgt = new Point(point.name, result.y, result.x, result.z);
        }
        catch (Proj4jException e)
        {
            Log.debug(e);
            return (null);
        }
        return (tgt);
    }

}
