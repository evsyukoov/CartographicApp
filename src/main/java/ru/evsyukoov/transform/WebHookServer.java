package ru.evsyukoov.transform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@Slf4j
public class WebHookServer {

    public static void main(String[] args) {
        SpringApplication.run(WebHookServer.class, args);
        log.info("Transform bot server started");
    }

}
