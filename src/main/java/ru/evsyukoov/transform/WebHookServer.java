package ru.evsyukoov.transform;

import com.jsevy.jdxf.DXFCircle;
import com.jsevy.jdxf.DXFDocument;
import com.jsevy.jdxf.DXFGraphics;
import com.jsevy.jdxf.DXFStyle;
import com.jsevy.jdxf.DXFText;
import com.jsevy.jdxf.DXFViewport;
import com.jsevy.jdxf.RealPoint;
import lombok.extern.slf4j.Slf4j;
import org.kabeja.DraftDocument;
import org.kabeja.common.Header;
import org.kabeja.common.LineType;
import org.kabeja.dxf.generator.DXFGenerator;
import org.kabeja.dxf.generator.DXFGeneratorFactory;
import org.kabeja.dxf.parser.DXFParserBuilder;
import org.kabeja.entities.Line;
import org.kabeja.entities.Polyline;
import org.kabeja.entities.Vertex;
import org.kabeja.entities.Viewport;
import org.kabeja.io.GenerationException;
import org.kabeja.math.Bounds;
import org.kabeja.math.Point3D;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.AttributedCharacterIterator;
import java.util.Collections;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class WebHookServer {

    public static void main(String[] args) throws IOException, GenerationException, ParseException {
        SpringApplication.run(WebHookServer.class, args);
        log.info("Transform bot server started");

//        paint1();
//        paint2();
    }

    public static void paint1() throws IOException {

        DXFDocument dxfDocument = new
                DXFDocument("Example");
        DXFGraphics graphics =
                dxfDocument.getGraphics();

//        AffineTransform transform = new AffineTransform();
        AffineTransform transform1 = new AffineTransform();
        transform1.scale(1.0D, -1.0D);
        //transform.concatenate(transform1);
        dxfDocument.getGraphics().setTransform(transform1);
        // set pen characteristics
        graphics.setColor(Color.RED);

//         draw stuff - line, rectangles, ovals, ...
//        graphics.drawLine(0, 0, 1000, 500);
//        graphics.drawRect(1000, 500, 150, 150);
//        graphics.drawRoundRect(20, 200, 130, 100, 20,
//                10);
//        graphics.drawOval(200, 800, 200, 400);
//        graphics.drawArc(100, 1900, 400, 200, 60, 150);
        graphics.drawPolyline(new int[]{0,5,10,15}, new int[]{0,5,10,15}, 4);
        DXFCircle circle1 = new DXFCircle(new RealPoint(0,0,0),0.1, graphics);
        DXFText dxfText = new DXFText("Point 1", new RealPoint(0, 0.2, 0), new DXFStyle(new Font(null, Font.PLAIN, 1)), graphics);
        DXFCircle circle2 = new DXFCircle(new RealPoint(100,100,0),0.1, graphics);
        DXFText dxfText2 = new DXFText("Point 1", new RealPoint(0, 100.2, 0), new DXFStyle(graphics.getFont()), graphics);
        java.util.List<DXFCircle> lst = java.util.List.of(circle1, circle2);
        dxfDocument.addEntity(circle1);
        dxfDocument.addEntity(circle2);
        dxfDocument.addEntity(dxfText);
        dxfDocument.addEntity(dxfText2);
        // can draw filled shapes, which get
        // implemented as DXF hatches
//        graphics.setColor(Color.BLUE);
//        graphics.fillRect(100, 100, 100, 50);
//        int[] xPoints = {200, 300, 250};
//        int[] yPoints = {200, 250, 300};
//        graphics.fillPolygon(xPoints, yPoints,
//                xPoints.length);
//
//        // text too
//        graphics.setFont(new Font(Font.MONOSPACED,
//                Font.PLAIN, 38));
//
//        // and even transformations
//        graphics.drawRect(100, 100, 200, 200);
        String text = dxfDocument.toDXFString();
        lst.forEach(DXFCircle::toDXFString);
        String filePath = "./test1.dxf";
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(text);
        fileWriter.flush();
        fileWriter.close();
        log.info("Paint 1 SUCCESS");
    }

    public static void paint2() throws IOException, GenerationException, ParseException {
//        FileInputStream fis1 = new FileInputStream("/Users/19572356/IdeaProjects/CartographicApp/ACAD_SRC_TEST.dxf");
//        Parser parser = DXFParserBuilder.createDefaultParser();
//        DraftDocument document = new DraftDocument();
//        parser.parse(fis1, document, Collections.emptyMap());
        InputStream inputStream = new FileInputStream("/Users/19572356/IdeaProjects/CartographicApp/src/main/resources/kabeja/profiles.xml");
        DXFGenerator generator = (DXFGenerator) DXFGeneratorFactory.createStreamGenerator(inputStream);

        DraftDocument document = new DraftDocument();

        Viewport viewport = new Viewport();

        viewport.setViewportID("*ACTIVE");
        viewport.setHeight(1000);

        LineType lineType = new LineType();
        lineType.setName("ByBlock");

        document.addViewport(viewport);
        viewport.setDocument(document);

        document.addLineType(lineType);


        OutputStream fos = new FileOutputStream("./test2.dxf");
        Polyline polyline = new Polyline();
        Line line = new Line();
        line.setStartPoint(new Point3D(100,100,0));
        line.setEndPoint(new Point3D(120,120,0));
        line.setDocument(document);

        Vertex v1 = new Vertex(new Point3D(0,0,0));
        Vertex v2 = new Vertex(new Point3D(5,5,0));
        Vertex v3 = new Vertex(new Point3D(10,10,0));
        Vertex v4 = new Vertex(new Point3D(15,15,0));
        polyline.addVertex(v1);
        polyline.addVertex(v2);
        polyline.addVertex(v3);
        polyline.addVertex(v4);
        polyline.setDocument(document);
        document.addEntity(polyline);
        document.addEntity(line);


        generator.generate(document, Map.of("dxf.encoding", "UTF-8"), fos);
        fos.close();
        log.info("Paint 2 SUCCESS");
    }

}
