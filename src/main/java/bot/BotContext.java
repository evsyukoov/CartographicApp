package bot;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class BotContext {
    private GeodeticBot bot;
    private Message message;
    private String token;
    private Update update;
    private Chat chat;

    public BotContext(GeodeticBot bot, Message message, String token, Update update, Chat chat) {
        this.bot = bot;
        this.message = message;
        this.token = token;
        this.update = update;
        this.chat = chat;
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

    public Chat getChat() {
        return chat;
    }

    public Update getUpdate() {
        return update;
    }
}
