package ru.evsyukoov.transform.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.evsyukoov.transform.handlers.InlineMessageHandler;
import ru.evsyukoov.transform.utils.TelegramUtils;

import javax.annotation.PostConstruct;
import java.util.TimeZone;


@Component
@Slf4j
public class GeodeticBot extends TelegramLongPollingBot {

    private final ThreadPoolTaskExecutor executor;

    private String token;

    private String botName;

    private final InlineMessageHandler inlineHandler;

    @Autowired
    public GeodeticBot(ThreadPoolTaskExecutor executor, InlineMessageHandler inlineHandler) {
        this.executor = executor;
        this.inlineHandler = inlineHandler;
    }


    @Value("${bot.token}")
    public void setToken(String token) {
        this.token = token;
    }

    @Value("${bot.name}")
    public void setBotName(String botName) {
        this.botName = botName;
    }

//    public void botAction(Update update) {
//        BotState bs;
//        Client client = null;
//        try {
//            if (update.getInlineQuery() != null) {
//                inlineAction(update);
//                return;
//            }
//            Chat chat;
//            if (update.getCallbackQuery() != null) {
//                chat = update.getCallbackQuery().getMessage().getChat();
//            } else {
//                chat = update.getMessage().getChat();
//            }
//            long id = chat.getId();
//            BotContext botContext = new BotContext(this, update.getMessage(), token, update, chat);
//            if ((client = getClientFromId(id)) == null) {
//                ClientDataAccessObject.addToDataBase
//                        (id, chat.getFirstName(), chat.getLastName(), chat.getUserName());
//                client = new Client(id, chat);
//                clients.add(client);
//                client.setState(1);
//            }
//            LogUtil.log(GeodeticBot.class.getName(), client);
//            bs = BotState.getStatement(client.getState());
//            bs.readFromClient(botContext, client);
//            bs = bs.next(botContext);
//            if (bs == null) {
//                return;
//            }
//            bs.writeToClient(botContext, client);
//            if (client.getClientReady()) {
//                LogUtil.log(GeodeticBot.class.getName(), client,
//                        "Was succesfully cancelled full circle");
//            }
//        } catch (SQLException throwables) {
//            LogUtil.log(Level.SEVERE, GeodeticBot.class.getName(), client, throwables);
//        }
//    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.execute(() -> {
            try {
                if (TelegramUtils.isInlineMessage(update)) {
                    this.execute(inlineHandler.getInlineAnswer(update));
                    return;
                }
                if (TelegramUtils.isTextDocumentOrCallbackMessage(update)) {
                    log.info("Implementation");
                }
            } catch (Exception e) {
                log.error("Error: ", e);
            }
        });
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
