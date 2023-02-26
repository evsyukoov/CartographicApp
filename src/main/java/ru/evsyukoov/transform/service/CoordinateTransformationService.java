package ru.evsyukoov.transform.service;

import org.osgeo.proj4j.Proj4jException;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.dto.Pline;

import java.util.List;

public interface CoordinateTransformationService {

    List<Point> transformPointsWgsToMsk(List<Point> points, String targetCoordinateSystemParams) throws Proj4jException;

    List<Point> transformPointsMskToWgs(List<Point> points, String srcCoordinateSystemParams) throws Proj4jException;

    List<Point> transformPointsMskToMsk(List<Point> points, String srcCoordinateSystemParams, String targetCoordinateSystemParams) throws Proj4jException;

    List<Pline> transformLinesWgsToMsk(List<Pline> lines, String targetCoordinateSystemParams) throws Proj4jException;

    List<Pline> transformLinesMskToWgs(List<Pline> lines, String srcCoordinateSystemParams) throws Proj4jException;

    List<Pline> transformLinesMskToMsk(List<Pline> lines, String srcCoordinateSystemParams, String targetCoordinateSystemParams) throws Proj4jException;
}
