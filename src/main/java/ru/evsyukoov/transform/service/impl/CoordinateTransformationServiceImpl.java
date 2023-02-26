package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.Proj4jException;
import org.osgeo.proj4j.ProjCoordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.service.CoordinateTransformationService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CoordinateTransformationServiceImpl implements CoordinateTransformationService {

    CRSFactory crsFactory;

    CoordinateTransformFactory coordinateTransformFactory;

    private final static String WGS84 = "EPSG:4326";

    @Autowired
    public CoordinateTransformationServiceImpl(CRSFactory crsFactory,
                                               CoordinateTransformFactory coordinateTransformFactory) {
        this.crsFactory = crsFactory;
        this.coordinateTransformFactory = coordinateTransformFactory;
    }

    public List<Point> transformPointsWgsToMsk(List<Point> points, String targetCoordinateSystemParams) throws Proj4jException {
        CoordinateReferenceSystem src = crsFactory.createFromName(WGS84);
        CoordinateReferenceSystem target = crsFactory.createFromParameters(null, targetCoordinateSystemParams);
        CoordinateTransform transformator = coordinateTransformFactory.createTransform(src, target);

        return transformPoints(points, transformator);
    }

    public List<Point> transformPointsMskToWgs(List<Point> points, String srcCoordinateSystemParams) throws Proj4jException {
        CoordinateReferenceSystem src = crsFactory.createFromParameters(null, srcCoordinateSystemParams);
        CoordinateReferenceSystem target = crsFactory.createFromName(WGS84);
        CoordinateTransform transformator = coordinateTransformFactory.createTransform(src, target);

        return transformPoints(points, transformator);
    }

    public List<Point> transformPointsMskToMsk(List<Point> points, String srcCoordinateSystemParams, String targetCoordinateSystemParams) throws Proj4jException {
        List<Point> wgsPoints = transformPointsMskToWgs(points, srcCoordinateSystemParams);

        return transformPointsWgsToMsk(wgsPoints, targetCoordinateSystemParams);
    }

    @Override
    public List<Pline> transformLinesWgsToMsk(List<Pline> lines, String targetCoordinateSystemParams) throws Proj4jException {
        lines.forEach(line -> line.setPolyline(transformPointsWgsToMsk(line.getPolyline(), targetCoordinateSystemParams)));
        return lines;
    }

    @Override
    public List<Pline> transformLinesMskToWgs(List<Pline> lines, String srcCoordinateSystemParams) throws Proj4jException {
        lines.forEach(line -> line.setPolyline(transformPointsMskToWgs(line.getPolyline(), srcCoordinateSystemParams)));
        return lines;
    }

    @Override
    public List<Pline> transformLinesMskToMsk(List<Pline> lines, String srcCoordinateSystemParams, String targetCoordinateSystemParams) throws Proj4jException {
        lines.forEach(line -> line.setPolyline(transformPointsMskToMsk(line.getPolyline(), srcCoordinateSystemParams, targetCoordinateSystemParams)));
        return lines;
    }

    private List<Point> transformPoints(List<Point> points, CoordinateTransform transformator) {
        List<Point> transformedPoints = new ArrayList<>();
        for (Point point : points) {
            ProjCoordinate tgtPointProj = new ProjCoordinate();
            ProjCoordinate scrPointProj = new ProjCoordinate(point.y, point.x);
            transformator.transform(scrPointProj, tgtPointProj);
            Point tgt = new Point(point.name, tgtPointProj.y, tgtPointProj.x, point.h);
            transformedPoints.add(tgt);
        }
        return transformedPoints;
    }
}
