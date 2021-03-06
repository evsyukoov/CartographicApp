package bot;

import org.telegram.telegrambots.meta.api.objects.Message;

public class BotContext {
    private GeodeticBot bot;
    private Message message;
    private String token;

    public BotContext(GeodeticBot bot, Message message, String token) {
        this.bot = bot;
        this.message = message;
        this.token = token;
    }

    public GeodeticBot getBot() {
        return bot;
    }

    public Message getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }
}
