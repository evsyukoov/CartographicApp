package convert;

import bot.Client;
import bot.enums.InputCoordinatesType;
import org.osgeo.proj4j.*;


public class Transformator {
    private CoordinateTransform         transformation;
    private CoordinateTransform         extraTransformation;

    Client client;

    private final static String WGS84 = "EPSG:4326";

    public Transformator(Client client) {
        this.client = client;
    }

    public void       initTransformation() throws Proj4jException
    {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem src;
        CoordinateReferenceSystem target;
        if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK) {
            src = factory.createFromParameters(null, client.getSrcSystem());
            target = factory.createFromName(WGS84);
        }
        else {
            src = factory.createFromName(WGS84);
            target = factory.createFromParameters(null, client.getSrcSystem());
        }
        transformation = new CoordinateTransformFactory().createTransform(src, target);
    }

    public void     initExtraTransformation() throws Proj4jException
    {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem src;
        CoordinateReferenceSystem target;
        src = factory.createFromName(WGS84);
        target = factory.createFromParameters(null, client.getTgtSystem());
        extraTransformation = new CoordinateTransformFactory().createTransform(src, target);
    }

    public     Point    transformOnePoint(Point point) throws Proj4jException
    {
        ProjCoordinate result = new ProjCoordinate();
        Point tgt;
        ProjCoordinate src;
        src = new ProjCoordinate(point.y, point.x);
        transformation.transform(src, result);
        tgt = new Point(point.name, result.y, result.x, point.h);
        return (tgt);
    }

    public  Point       twoStepTransform(Point point) throws Proj4jException, IllegalStateException
    {
        ProjCoordinate middwareResult = new ProjCoordinate();
        ProjCoordinate result = new ProjCoordinate();
        ProjCoordinate src = new ProjCoordinate(point.y, point.x);;
        transformation.transform(src, middwareResult);
        extraTransformation.transform(middwareResult, result);
        return new Point(point.name, result.x, result.y, point.h);
    }
}

