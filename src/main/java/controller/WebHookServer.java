package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@SpringBootApplication
public class WebHookServer {

    private static final Logger logger = Logger.getLogger(WebHookServer.class.getName());

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().readConfiguration(WebHookServer.class.getResourceAsStream("../logging.properties"));
        logger.log(Level.INFO, "SERVER START");
        SpringApplication.run(WebHookServer.class, args);
    }

}
