package webhook;

import bot.GeodeticBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("application.properties")
public class WebHookServer {
    static GeodeticBot bot = new GeodeticBot();

    public static void main(String[] args) {
        bot = new GeodeticBot();
        EntryPointController controller = new EntryPointController(bot);
        SpringApplication.run(WebHookServer.class, args);
    }

}
