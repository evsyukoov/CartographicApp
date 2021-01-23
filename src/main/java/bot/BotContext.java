package bot;

import org.telegram.telegrambots.api.objects.Message;

public class BotContext {
    private GeodeticBot bot;
    private Message message;

    public BotContext(GeodeticBot bot, Message message) {
        this.bot = bot;
        this.message = message;
    }

    public GeodeticBot getBot() {
        return bot;
    }

    public Message getMessage() {
        return message;
    }
}
