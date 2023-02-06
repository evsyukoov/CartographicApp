package ru.evsyukoov.transform.bot;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class BotContext {
    private final GeodeticBot bot;
    private final Message message;
    private final String token;
    private final Update update;
    private final Chat chat;

    public BotContext(GeodeticBot bot, Message message,
                      String token, Update update, Chat chat) {
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
