package ru.evsyukoov.transform.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.evsyukoov.transform.handlers.MessageHandler;
import ru.evsyukoov.transform.utils.TelegramUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.TimeZone;


@Component
@Slf4j
public class GeodeticBot extends TelegramLongPollingBot {

    private final ThreadPoolTaskExecutor executor;

    private String token;

    private String botName;

    private final MessageHandler messageHandler;

    @Autowired
    public GeodeticBot(ThreadPoolTaskExecutor executor, MessageHandler messageHandler) {
        this.executor = executor;
        this.messageHandler = messageHandler;
    }


    @Value("${bot.token}")
    public void setToken(String token) {
        this.token = token;
    }

    @Value("${bot.name}")
    public void setBotName(String botName) {
        this.botName = botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.execute(() -> {
            try {
                if (TelegramUtils.isInlineMessage(update)) {
                    this.execute(messageHandler.prepareInline(update));
                } else if (TelegramUtils.isTextDocumentOrCallbackMessage(update)) {
                    sendMessages(messageHandler.prepareMessage(update));
                }
            } catch (Exception e) {
                log.error("Error: ", e);
            }
        });
    }

    private void sendMessages(List<PartialBotApiMethod<?>> messages) throws TelegramApiException {
        for (PartialBotApiMethod<?> method : messages) {
            if (method instanceof SendMessage) {
                this.execute((SendMessage) method);
            } else if (method instanceof SendDocument) {
                this.execute((SendDocument) method);
            } else if (method instanceof EditMessageReplyMarkup) {
                this.execute((EditMessageReplyMarkup) method);
            } else if (method instanceof SendVideo) {
                this.execute((SendVideo) method);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @PostConstruct
    private void init() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
        log.info("Bot {} at polling mode successfully started", botName);
    }
}
