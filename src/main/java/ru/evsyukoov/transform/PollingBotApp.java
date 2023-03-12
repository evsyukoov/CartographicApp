package ru.evsyukoov.transform;

import lombok.extern.slf4j.Slf4j;
import org.kabeja.io.GenerationException;
import org.kabeja.parser.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import java.io.IOException;

@SpringBootApplication
@Slf4j
public class PollingBotApp {

    public static void main(String[] args) throws IOException, GenerationException, ParseException {
        SpringApplication.run(PollingBotApp.class, args);
        log.info("Transform bot server started");
    }

}
