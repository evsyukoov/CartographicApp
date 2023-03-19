package ru.evsyukoov.transform.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.service.ParserService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("TEST")
public class ParserTest {

    @Autowired
    ParserService parserService;

    @Test
    public void testKmlParser1() throws IOException {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("doc1.kml");
        InputInfo inputInfo = parserService.parseKml(is);
        assertEquals(1, inputInfo.getPoints().size());
        assertEquals(1, inputInfo.getPolylines().size());
        assertEquals(15, inputInfo.getPolylines().stream()
                .map(Pline::getPolyline)
                .mapToLong(Collection::size).sum());
    }

    @Test
    public void testKmlParser2() throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("doc2.kml");
        InputInfo inputInfo = parserService.parseKml(is);

        assertEquals(2, inputInfo.getPoints().size());
        assertEquals(1, inputInfo.getPolylines().size());
        assertEquals(30, inputInfo.getPolylines().stream()
                .map(Pline::getPolyline)
                .mapToLong(Collection::size).sum());
    }

    @Test
    public void testKmlParser3() throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("doc3.kml");
        InputInfo inputInfo = parserService.parseKml(is);

        assertEquals(3, inputInfo.getPoints().size());
        assertEquals(2, inputInfo.getPolylines().size());
        assertEquals(45, inputInfo.getPolylines().stream()
                .map(Pline::getPolyline)
                .mapToLong(Collection::size).sum());
    }
}
