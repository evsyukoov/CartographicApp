package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.kabeja.DraftDocument;
import org.kabeja.common.Type;
import org.kabeja.entities.Attrib;
import org.kabeja.entities.Insert;
import org.kabeja.entities.LW2DVertex;
import org.kabeja.entities.LWPolyline;
import org.kabeja.entities.Polyline;
import org.kabeja.entities.Vertex;
import org.kabeja.math.Point3D;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.evsyukoov.transform.convert.Point;
import ru.evsyukoov.transform.dto.AutocadFileInfo;
import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.CoordinatesType;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import ru.evsyukoov.transform.service.FileParser;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.List;

@Service
@Slf4j
public class FileParserImpl implements FileParser {

    @Value("${parser.text-delimetr}")
    private String delimetr;

    private static final String KML_COORDS_DELIMETR = ",";

    private final DocumentBuilder documentBuilder;

    private final ApplicationContext context;

    private static final int CAD_BORDER_RIGHT = 1000;

    private static final int CAD_BORDER_LEFT = -1000;

    @Autowired
    public FileParserImpl(DocumentBuilder documentBuilder, Parser dxfParser, ApplicationContext context) {
        this.documentBuilder = documentBuilder;
        this.context = context;
    }

    @Override
    public FileInfo parseFile(InputStream inputStream, String charset, FileFormat format) throws IOException {
        FileInfo fileInfo;
        switch (format) {
            case CSV:
                fileInfo = this.parseCsv(inputStream, charset);
                break;
            case KML:
                fileInfo = this.parseKml(inputStream);
                break;
            case GPX:
                fileInfo = this.parseGpx(inputStream);
                break;
            case DXF:
                fileInfo = this.parseDxf(inputStream);
                break;
            case TXT:
                fileInfo = this.parseTxt(inputStream, charset);
                break;
            case KMZ:
                fileInfo = this.parseKmz(inputStream);
                break;
            default:
                return null;
        }
        if (fileInfo.getPoints().isEmpty()) {
            return null;
        }
        fileInfo.setCoordinatesType(getPointCoordinatesType(fileInfo.getPoints().get(0)));
        return fileInfo;
    }

    @Override
    public FileInfo parseText(String text) {
        String[] arr = text.split("\n");
        CoordinatesType coordinatesType = null;
        FileInfo fileInfo = new FileInfo();
        for (String line : arr) {
            Point point = parseTextLine(line);
            checkCoordinateSystem(point, coordinatesType, line);
            coordinatesType = getPointCoordinatesType(point);
            fileInfo.getPoints().add(point);
        }
        return fileInfo;
    }

    @Override
    public FileInfo parseCsv(InputStream inputStream, String charset) throws IOException {
        return parseTxt(inputStream, charset);
    }

    @Override
    public FileInfo parseTxt(InputStream inputStream, String charset) throws IOException {
        String line;
        BufferedReader fr = new BufferedReader(new InputStreamReader(inputStream, charset));
        CoordinatesType coordinatesType = null;
        FileInfo fileInfo = new FileInfo();
        while ((line = fr.readLine()) != null) {
            if (!line.isEmpty()) {
                Point point = parseTextLine(line);
                checkCoordinateSystem(point, coordinatesType, line);
                coordinatesType = getPointCoordinatesType(point);
                fileInfo.getPoints().add(point);
            }
        }
        return fileInfo;
    }

    @Override
    public FileInfo parseKml(InputStream inputStream) throws IOException {
        FileInfo fileInfo = new FileInfo();
        try {
            Document doc = documentBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("Placemark");
            checkPoints(nodeList);
            CoordinatesType coordinatesType = null;
            for(int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element eElement = (Element) node;
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList pointNode = eElement.getElementsByTagName("Point");
                    if (pointNode == null || pointNode.getLength() == 0) {
                        continue;
                    }
                    String coordinates = eElement.getElementsByTagName("coordinates").item(0).getTextContent();
                    String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                    Point point = parseKmlLine(coordinates);
                    point.setName(name);
                    checkCoordinateSystem(point, coordinatesType, coordinates);
                    coordinatesType = getPointCoordinatesType(point);
                    fileInfo.getPoints().add(point);
                }
            }
            return fileInfo;
        } catch (SAXException e) {
            String err = "Невозможно прочитать отправленный KML файл";
            log.error(err);
            throw new WrongFileFormatException(err);
        }
    }

    @Override
    public FileInfo parseKmz(InputStream inputStream) throws IOException {
        FileInfo fileInfo = new FileInfo();
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            String name;
            while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                name = entry.getName();
                if (!name.substring(name.indexOf('.') + 1).equalsIgnoreCase(FileFormat.KML.name()))
                    continue;
                ByteArrayInputStream baos = new ByteArrayInputStream(zis.readAllBytes());
                fileInfo.getPoints().addAll(parseKml(baos).getPoints());
                zis.closeEntry();
            }
        }
        return fileInfo;
    }

    @Override
    public FileInfo parseGpx(InputStream inputStream) throws IOException {
        FileInfo fileInfo = new FileInfo();
        try {
            Document doc = documentBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("wpt");
            checkPoints(nodeList);
            CoordinatesType coordinatesType = null;
            for(int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element eElement = (Element) node;
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String lat = eElement.getAttribute("lat");
                    String lon = eElement.getAttribute("lon");
                    String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                    Point point = new Point(name, Double.parseDouble(lat), Double.parseDouble(lon), 0.0);
                    checkCoordinateSystem(point, coordinatesType, eElement.getTextContent());
                    coordinatesType = getPointCoordinatesType(point);
                    fileInfo.getPoints().add(point);
                }
            }
            return fileInfo;
        } catch (SAXException e) {
            String err = "Невозможно прочитать отправленный GPX файл";
            log.error(err);
            throw new WrongFileFormatException(err);
        }
    }

    @Override
    public FileInfo parseDxf(InputStream inputStream) {
        Parser dxfParser = context.getBean(Parser.class);
        AutocadFileInfo fileInfo = new AutocadFileInfo();
        try {
            DraftDocument draftDocument = new DraftDocument();
            dxfParser.parse(inputStream, draftDocument, Collections.emptyMap());
            fileInfo.getPoints().addAll(parseDxfBlockInsertions(draftDocument));
            fileInfo.getPolylines().addAll(parseDxfPolylines(draftDocument));
            log.info("Successfully parse DXF file");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return fileInfo;
    }

    private List<Point> parseDxfBlockInsertions(DraftDocument doc) {
        List<Point> points = new ArrayList<>();
        doc.getLayers().stream()
                .map(layer -> layer.getEntitiesByType(Type.TYPE_INSERT))
                .flatMap(Collection::stream)
                .filter(insert -> !isBlockAtTheDrawingBorder(insert))
                .forEach(insert -> {
                    Point3D point3D = insert.getInsertPoint();
                    Point point = new Point(point3D.getX(), point3D.getY(), point3D.getZ());

                    Attrib attrib = insert.getAttributes()
                            .stream()
                            .filter(attr -> StringUtils.hasText(attr.getText()))
                            .findFirst()
                            .orElse(null);
                    point.setName(attrib == null ? "UNKNOWN_NAME" : attrib.getText());
                    points.add(point);
                });
        return points;
    }

    private List<AutocadFileInfo.Polyline> parseDxfPolylines(DraftDocument doc) {
        List<AutocadFileInfo.Polyline> polylines = new ArrayList<>();
        List<Polyline> polylinesFromDxf1 = doc.getLayers().stream()
                .map(layer -> layer.getEntitiesByType(Type.TYPE_POLYLINE))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (int i = 0; i < polylinesFromDxf1.size(); i++) {
            List<Vertex> vertexes = polylinesFromDxf1.get(i).getVertices();
            AutocadFileInfo.Polyline polyline = new AutocadFileInfo.Polyline();
            for (int j = 0; j < vertexes.size(); j++) {
                Point p = point3DtoPoint(vertexes.get(j).getPoint());
                p.setName(i + "_" + j);
                polyline.addPoint(p);
            }
            polylines.add(polyline);
        }

        List<LWPolyline> polylinesFromDxf2 = doc.getLayers().stream()
                .map(layer -> layer.getEntitiesByType(Type.TYPE_LWPOLYLINE))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (int i = 0; i < polylinesFromDxf2.size(); i++) {
            List<LW2DVertex> vertexes = polylinesFromDxf2.get(i).getVertices();
            AutocadFileInfo.Polyline polyline = new AutocadFileInfo.Polyline();
            for (int j = 0; j < vertexes.size(); j++) {
                Point p = point3DtoPoint(vertexes.get(j).getPoint());
                p.setName(i + "_" + j);
                polyline.addPoint(p);
            }
            polylines.add(polyline);
        }
        return polylines;
    }

    private Point point3DtoPoint(Point3D p3D) {
        return new Point(p3D.getX(), p3D.getY(), p3D.getZ());
    }

    private boolean isBlockAtTheDrawingBorder(Insert insert) {
        Point3D point = insert.getInsertPoint();
        return point.getX() <= CAD_BORDER_RIGHT && point.getX() >= CAD_BORDER_LEFT &&
               point.getY() <= CAD_BORDER_RIGHT && point.getY() >= CAD_BORDER_LEFT;
    }

    private Point parseTextLine(String line) {
        String[] splitted = line.split("\\s*" + delimetr + "\\s*");
        double h = 0;
        if (splitted.length > 4 || splitted.length < 3) {
            String err = String.format("Неверный формат строки. Строка: %s", line);
            log.error(err);
            throw new WrongFileFormatException(err);
        }
        try {
            return new Point(splitted[0], Double.parseDouble(splitted[1]), Double.parseDouble(splitted[2]),
                        (splitted.length == 4 ? Double.parseDouble(splitted[3]) : h));
        } catch (Exception e) {
            String err = String.format("Ошибочный текст, строка: %s", line);
            log.error(err, e);
            throw new WrongFileFormatException(err, e);
        }
    }

    private Point parseKmlLine(String line) {
        String[] splitted = line.split("\\s*" + KML_COORDS_DELIMETR + "\\s*");
        double h = 0;
        if (splitted.length > 3 || splitted.length < 2) {
            String err = String.format("Неверный формат строки. Строка: %s", line);
            log.error(err);
            throw new WrongFileFormatException(err);
        }
        try {
            h = splitted.length == 3 ? Double.parseDouble(splitted[2]) : h;
            return new Point(Double.parseDouble(splitted[1]), Double.parseDouble(splitted[0]), h);
        } catch (Exception e) {
            String err = String.format("Ошибочный текст, строка: %s", line);
            log.error(err, e);
            throw new WrongFileFormatException(err, e);
        }
    }

    private void checkCoordinateSystem(Point point, CoordinatesType coordinatesType, String line) {
        if (coordinatesType != null && getPointCoordinatesType(point) != coordinatesType) {
            String err = String.format("Различные типы координат в файле. Проверьте строку %s.", line);
            log.error(err);
            throw new WrongFileFormatException(err);
        }
    }

    private void checkPoints(NodeList nodeList) {
        if (nodeList == null) {
            String err = "Не найдены точки в KML файле";
            log.error(err);
            throw new WrongFileFormatException(err);
        }
    }

    private CoordinatesType getPointCoordinatesType(Point point) {
        if (point.getX() >= -90 && point.getX() <= 90 && point.getY() >= 0 && point.getY() <= 360) {
            return CoordinatesType.WGS_84;
        }
        return CoordinatesType.MSK;

    }
}
