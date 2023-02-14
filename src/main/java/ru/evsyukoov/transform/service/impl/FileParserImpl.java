package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.evsyukoov.transform.convert.Point;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class FileParserImpl implements FileParser {

    @Value("${parser.text-delimetr}")
    private String delimetr;

    private static final String KML_COORDS_DELIMETR = ",";

    private final DocumentBuilder documentBuilder;

    private final ApplicationContext context;

    @Autowired
    public FileParserImpl(DocumentBuilder documentBuilder, Parser dxfParser, ApplicationContext context) {
        this.documentBuilder = documentBuilder;
        this.context = context;
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
        try {
            dxfParser.parse(inputStream, DXFParser.DEFAULT_ENCODING);
            DXFDocument document = dxfParser.getDocument();
            log.info("Successfully parse DXF file");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
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
