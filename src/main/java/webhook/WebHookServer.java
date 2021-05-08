package webhook;

import bot.GeodeticBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("application.properties")
public class WebHookServer {

    public static void main(String[] args) {
        SpringApplication.run(WebHookServer.class, args);
    }

}
