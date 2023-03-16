package ru.evsyukoov.transform.context;

import com.ibm.icu.text.Transliterator;
import com.jsevy.jdxf.DXFStyle;
import org.kabeja.dxf.generator.DXFGenerator;
import org.kabeja.dxf.generator.DXFGeneratorFactory;
import org.kabeja.dxf.parser.DXFParserBuilder;
import org.kabeja.parser.Parser;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.evsyukoov.transform.constants.Const;
import ru.evsyukoov.transform.dto.InputInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MainContext {

    @Bean("executor")
    ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor threadPoolExecutor =  new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(10);
        threadPoolExecutor.setMaxPoolSize(100);
        threadPoolExecutor.setKeepAliveSeconds(120);
        threadPoolExecutor.setQueueCapacity(100);
        return threadPoolExecutor;
    }

    @Bean
    Transliterator latinToCyrillic() {
        return Transliterator.getInstance("Latin-Russian/BGN");
    }

    @Bean
    Transliterator cyrillicToLatin() {
        return Transliterator.getInstance("Russian-Latin/BGN");
    }

    /**
     * @return - мапа для хранения распаршенного файла, который отправил клиент
     * На диск файл также кладем, чтобы в случае ненахода в кеше считать с диска
     */
    @Bean
    Map<Long, InputInfo> clientFileCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        return factory.newDocumentBuilder();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    Parser dxfParser() {
        return DXFParserBuilder.createDefaultParser();
    }

    @Bean
    CRSFactory crsFactory() {
        return new CRSFactory();
    }

    @Bean
    CoordinateTransformFactory coordinateTransformFactory() {
        return new CoordinateTransformFactory();
    }

    @Bean
    DXFGenerator dxfGenerator() throws IOException {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("./kabeja/profiles.xml")) {
            return (DXFGenerator) DXFGeneratorFactory.createStreamGenerator(inputStream);
        }
    }

    @Bean
    AffineTransform transformationRotate() {
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.scale(1.0D, -1.0D);
        return affineTransform;
    }

    @Bean
    DXFStyle dxfStyle() {
        return new DXFStyle(new Font(null, Font.PLAIN, Const.FONT_SIZE));
    }

    @Bean
    public Transformer xmlPrettyPrint() throws TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }

}
