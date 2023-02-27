package ru.evsyukoov.transform.context;

import com.ibm.icu.text.Transliterator;
import org.kabeja.common.Block;
import org.kabeja.dxf.generator.DXFGenerator;
import org.kabeja.dxf.generator.DXFGeneratorFactory;
import org.kabeja.dxf.parser.DXFParserBuilder;
import org.kabeja.entities.AttribDefinition;
import org.kabeja.entities.Circle;
import org.kabeja.math.Point3D;
import org.kabeja.parser.Parser;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.evsyukoov.transform.dto.FileInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
    Map<Long, FileInfo> clientFileCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        return dbFactory.newDocumentBuilder();
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
        try (InputStream inputStream = MainContext.class.getClassLoader().getResourceAsStream("kabeja/profiles.xml")) {
            return (DXFGenerator) DXFGeneratorFactory.createStreamGenerator(inputStream);
        }
    }
    @Bean
    Block defaultAutocadBlock() {
        Block block = new Block();
        Circle circle = new Circle();
        circle.setBlockEntity(true);
        circle.setCenterPoint(new Point3D(0, 0, 0));
        circle.setRadius(1);
        block.addEntity(circle);

        AttribDefinition attribDefinition = new AttribDefinition();
        attribDefinition.setBlockEntity(true);
        attribDefinition.setTag("NAME");
        attribDefinition.setTextFieldLength(100);
        attribDefinition.setBlockAttribute(false);

        block.addEntity(new AttribDefinition());
        block.setName("INGGEO_BLOCK");
        return block;
    }

}
