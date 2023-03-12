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
import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.CoordinatesType;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.utils.Utils;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class InputContentHandlerImpl implements InputContentHandler {

    @Value("${text.delimetr}")
    private String delimetr;

    private static final String KML_COORDS_DELIMETR = ",";

    private final DocumentBuilder documentBuilder;

    private final ApplicationContext context;

    private final DataService dataService;

    private static final int CAD_BORDER_RIGHT = 1000;

    private static final int CAD_BORDER_LEFT = -1000;

    private final Map<Long, InputInfo> clientFileCache;

    @Value("${file-storage.upload}")
    private String fileStoragePath;

    @Autowired
    public InputContentHandlerImpl(DocumentBuilder documentBuilder,
                                   ApplicationContext context,
                                   DataService dataService,
                                   Map<Long, InputInfo> clientFileCache) {
        this.documentBuilder = documentBuilder;
        this.context = context;
        this.dataService = dataService;
        this.clientFileCache = clientFileCache;
    }

    @Override
    public void removeInfo(Client client) {
        clientFileCache.remove(client.getId());
    }

    @Override
    public InputInfo getInfo(Client client) throws IOException {
        if (clientFileCache.isEmpty() || !clientFileCache.containsKey(client.getId())) {
            FileFormat format = dataService.getClientFileFormatChoice(client);
            log.info("No file info at app cache for client {}", client);
            String charset = (format == FileFormat.CSV || format == FileFormat.TXT) ? "windows-1251" : "UTF-8";
            InputInfo inputInfo = parseFile(new FileInputStream(Utils.getLocalFilePath(fileStoragePath, client.getId(), format)),
                    charset, format);
            clientFileCache.put(client.getId(), inputInfo);
            return inputInfo;
        }
        return clientFileCache.get(client.getId());
    }

    @Override
    public InputInfo putInfo(InputStream inputStream, String charset, FileFormat format, long clientId) throws IOException {
        InputInfo inputInfo = parseFile(inputStream, charset, format);
        clientFileCache.put(clientId, inputInfo);
        return inputInfo;
    }

    @Override
    public InputInfo putInfo(String text, long clientId) throws IOException {
        InputInfo inputInfo = parseText(text);
        clientFileCache.put(clientId, inputInfo);
        return inputInfo;
    }

    @Override
    public InputInfo parseFile(InputStream inputStream, String charset, FileFormat format) throws IOException {
        InputInfo inputInfo;
        switch (format) {
            case CSV:
                inputInfo = this.parseCsv(inputStream, charset);
                break;
            case KML:
                inputInfo = this.parseKml(inputStream);
                break;
            case GPX:
                inputInfo = this.parseGpx(inputStream);
                break;
            case DXF:
                inputInfo = this.parseDxf(inputStream);
                break;
            case CONSOLE_IN:
            case TXT:
                inputInfo = this.parseTxt(inputStream, charset);
                break;
            case KMZ:
                inputInfo = this.parseKmz(inputStream);
                break;
            default:
                return null;
        }
        inputInfo.setFormat(format);
        if (inputInfo.getPoints().isEmpty()) {
            if (inputInfo.getPolylines().isEmpty()
                    || inputInfo.getPolylines().get(0).getPolyline().isEmpty()) {
                return null;
            }
            Point p = inputInfo.getPolylines().get(0).getPolyline().get(0);
            inputInfo.setCoordinatesType(getPointCoordinatesType(p));
            return inputInfo;
        }
        inputInfo.setCoordinatesType(getPointCoordinatesType(inputInfo.getPoints().get(0)));
        return inputInfo;
    }

    @Override
    public InputInfo parseText(String text) {
        String[] arr = text.split("\n");
        CoordinatesType coordinatesType = null;
        InputInfo inputInfo = new InputInfo();
        for (String line : arr) {
            Point point = parseTextLine(line);
            checkCoordinateSystem(point, coordinatesType, line);
            coordinatesType = getPointCoordinatesType(point);
            inputInfo.getPoints().add(point);
        }
        if (inputInfo.getPoints().isEmpty()) {
            return null;
        }
        inputInfo.setFormat(FileFormat.CONSOLE_IN);
        inputInfo.setCoordinatesType(getPointCoordinatesType(inputInfo.getPoints().get(0)));
        return inputInfo;
    }

    @Override
    public InputInfo parseCsv(InputStream inputStream, String charset) throws IOException {
        return parseTxt(inputStream, charset);
    }

    @Override
    public InputInfo parseTxt(InputStream inputStream, String charset) throws IOException {
        String line;
        BufferedReader fr = new BufferedReader(new InputStreamReader(inputStream, charset));
        CoordinatesType coordinatesType = null;
        InputInfo inputInfo = new InputInfo();
        while ((line = fr.readLine()) != null) {
            if (!line.isEmpty()) {
                Point point = parseTextLine(line);
                checkCoordinateSystem(point, coordinatesType, line);
                coordinatesType = getPointCoordinatesType(point);
                inputInfo.getPoints().add(point);
            }
        }
        return inputInfo;
    }

    @Override
    public InputInfo parseKml(InputStream inputStream) throws IOException {
        InputInfo inputInfo = new InputInfo();
        try {
            Document doc = documentBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("Placemark");
            checkPoints(nodeList);
            CoordinatesType coordinatesType = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Element eElement = (Element) node;
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList pointNode = eElement.getElementsByTagName("Point");
                    NodeList lineNode = eElement.getElementsByTagName("LineString");
                    if (pointNode != null && pointNode.getLength() != 0) {
                        String coordinates = eElement.getElementsByTagName("coordinates").item(0).getTextContent();
                        String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                        Point point = parseKmlLine(coordinates);
                        point.setName(name);
                        checkCoordinateSystem(point, coordinatesType, coordinates);
                        coordinatesType = getPointCoordinatesType(point);
                        inputInfo.getPoints().add(point);
                    } else if (lineNode != null && lineNode.getLength() != 0) {
                        String coordinates = eElement.getElementsByTagName("coordinates").item(0).getTextContent();
                        Pline pline = new Pline();
                        String[] pointsCoords = coordinates.split("\\s+");
                        int j = 1;
                        for (String line : pointsCoords) {
                            Point p = parseKmlLine(line);
                            checkCoordinateSystem(p, coordinatesType, line);
                            pline.addPoint(p);
                            p.setName(String.valueOf(j++));
                        }
                        inputInfo.getPolylines().add(pline);
                    }
                }
            }
            return inputInfo;
        } catch (SAXException e) {
            String err = "Невозможно прочитать отправленный KML файл";
            log.error(err);
            throw new WrongFileFormatException(err);
        }
    }

    @Override
    public InputInfo parseKmz(InputStream inputStream) throws IOException {
        InputInfo inputInfo = new InputInfo();
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            String name;
            while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                name = entry.getName();
                if (!name.substring(name.indexOf('.') + 1).equalsIgnoreCase(FileFormat.KML.name()))
                    continue;
                ByteArrayInputStream baos = new ByteArrayInputStream(zis.readAllBytes());
                inputInfo.getPoints().addAll(parseKml(baos).getPoints());
                zis.closeEntry();
            }
        }
        return inputInfo;
    }

    @Override
    public InputInfo parseGpx(InputStream inputStream) throws IOException {
        InputInfo inputInfo = new InputInfo();
        try {
            Document doc = documentBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList nodeListPoints = doc.getElementsByTagName("wpt");
            checkPoints(nodeListPoints);
            CoordinatesType coordinatesType = null;
            for (int i = 0; i < nodeListPoints.getLength(); i++) {
                Node node = nodeListPoints.item(i);
                Element eElement = (Element) node;
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String lat = eElement.getAttribute("lat");
                    String lon = eElement.getAttribute("lon");
                    String name = eElement.getElementsByTagName("name").item(0).getTextContent();
                    Point point = new Point(name, Double.parseDouble(lat), Double.parseDouble(lon), 0.0);
                    checkCoordinateSystem(point, coordinatesType, eElement.getTextContent());
                    coordinatesType = getPointCoordinatesType(point);
                    inputInfo.getPoints().add(point);
                }
            }
            NodeList nodeListLines = doc.getElementsByTagName("trkseg");
            List<Pline> parsedLines = new ArrayList<>();
            for (int i = 0; i < nodeListLines.getLength(); i++) {
                Node segment = nodeListLines.item(i);
                Pline line = new Pline();
                NodeList lines = segment.getChildNodes();
                for (int j = 0; j < lines.getLength(); j++) {
                    Node nodeCoord = lines.item(j);
                    String lat = null;
                    String lon = null;
                    if (nodeCoord.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nodeCoord;
                        lat = eElement.getAttribute("lat");
                        lon = eElement.getAttribute("lon");
                    }
                    if (lat != null && lon != null) {
                        Point point = new Point(String.valueOf(j++), Double.parseDouble(lat), Double.parseDouble(lon), 0.0);
                        line.addPoint(point);
                    }

                }
                if (!line.getPolyline().isEmpty()) {
                    parsedLines.add(line);
                }
            }
            inputInfo.getPolylines().addAll(parsedLines);
            return inputInfo;
        } catch (SAXException e) {
            String err = "Невозможно прочитать отправленный GPX файл";
            log.error(err);
            throw new WrongFileFormatException(err);
        }
    }

    @Override
    public InputInfo parseDxf(InputStream inputStream) {
        Parser dxfParser = context.getBean(Parser.class);
        InputInfo inputInfo = new InputInfo();
        DraftDocument draftDocument = null;
        try {
            draftDocument = new DraftDocument();
            dxfParser.parse(inputStream, draftDocument, Collections.emptyMap());
            inputInfo.getPoints().addAll(parseDxfBlockInsertions(draftDocument));
            inputInfo.getPolylines().addAll(parseDxfPolylines(draftDocument));
            log.info("Successfully parse DXF file");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return inputInfo;
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

                    Attrib attribName = insert.getAttributes()
                            .stream()
                            .filter(attr -> (StringUtils.hasText(attr.getTag()) && attr.getTag().equalsIgnoreCase("NAME")))
                            .findFirst()
                            .orElse(null);
                    if (attribName == null) {
                        attribName = insert.getAttributes()
                                .stream()
                                .filter(attr -> (StringUtils.hasText(attr.getText()) && !attr.getText().startsWith("\\")))
                                .findFirst()
                                .orElse(null);
                    }
                    Attrib attribHeight = insert.getAttributes()
                            .stream()
                            .filter(attr -> (StringUtils.hasText(attr.getTag())
                                    && (attr.getTag().equalsIgnoreCase("HEIGHT")) ||
                                    attr.getTag().equalsIgnoreCase("OTMETKA") ||
                                    attr.getTag().equalsIgnoreCase("ОТМЕТКА") ||
                                    attr.getTag().equalsIgnoreCase("ВЫСОТА")))
                            .findFirst()
                            .orElse(null);
                    point.setName(attribName == null ? "UNKNOWN_NAME" : attribName.getText() +
                            (attribHeight != null && StringUtils.hasText(attribHeight.getText()) ? "" : "(h = " + attribHeight.getText() + ")"));
                    points.add(point);
                });
        return points;
    }

    private List<Pline> parseDxfPolylines(DraftDocument doc) {
        List<Pline> polylines = new ArrayList<>();
        List<Polyline> polylinesFromDxf1 = doc.getLayers().stream()
                .map(layer -> layer.getEntitiesByType(Type.TYPE_POLYLINE))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (int i = 0; i < polylinesFromDxf1.size(); i++) {
            List<Vertex> vertexes = polylinesFromDxf1.get(i).getVertices();
            Pline line = new Pline();
            for (int j = 0; j < vertexes.size(); j++) {
                Point p = point3DtoPoint(vertexes.get(j).getPoint());
                p.setName(String.valueOf(j + 1));
                line.addPoint(p);
            }
            polylines.add(line);
        }

        List<LWPolyline> polylinesFromDxf2 = doc.getLayers().stream()
                .map(layer -> layer.getEntitiesByType(Type.TYPE_LWPOLYLINE))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (int i = 0; i < polylinesFromDxf2.size(); i++) {
            List<LW2DVertex> vertexes = polylinesFromDxf2.get(i).getVertices();
            Pline line = new Pline();
            for (int j = 0; j < vertexes.size(); j++) {
                Point p = point3DtoPoint(vertexes.get(j).getPoint());
                p.setName(String.valueOf(j + 1));
                line.addPoint(p);
            }
            polylines.add(line);
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
