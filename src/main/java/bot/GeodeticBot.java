package bot;

import dao.ClientDataAccessObject;
import dao.DataAccessObject;;
import logging.LogUtil;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class GeodeticBot extends TelegramLongPollingBot {
    final String token = "tokenToReplace";
    private final String botName = "botNameToReplace";

    public static LinkedList<Client> clients;

    public Client getClientFromId(long id) {
        for (Client c : clients) {
            if (id == c.getId())
                return (c);
        }
        return null;
    }

    public void inlineAction(Update update) {
        InlineMod inlineMod = new InlineMod(update, this);
//        Thread thread = new Thread(() -> {
            try {
                inlineMod.answerInline();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
       // });
        //thread.start();
        //thread.join();
    }

    public void botAction(Update update) {
        BotState bs;
        Client client = null;
        try {
            if (update.getInlineQuery() != null) {
                inlineAction(update);
                return;
            }
            Chat chat;
            if (update.getCallbackQuery() != null) {
                chat = update.getCallbackQuery().getMessage().getChat();
            } else {
                chat = update.getMessage().getChat();
            }
            long id = chat.getId();
            BotContext botContext = new BotContext(this, update.getMessage(), token, update, chat);
            if ((client = getClientFromId(id)) == null) {
                ClientDataAccessObject.addToDataBase
                        (id, chat.getFirstName(), chat.getLastName(), chat.getUserName());
                client = new Client(id, chat);
                clients.add(client);
                client.setState(1);
            }
            LogUtil.log(GeodeticBot.class.getName(), client);
            bs = BotState.getStatement(client.getState());
            bs.readFromClient(botContext, client);
            bs = bs.next(botContext);
            bs.writeToClient(botContext, client);
            if (client.getClientReady()) {
                LogUtil.log(GeodeticBot.class.getName(), client,
                        "Was succesfully cancelled full circle");
            }
        } catch (SQLException throwables) {
            LogUtil.log(Level.SEVERE, GeodeticBot.class.getName(), client, throwables);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage() != null &&
                (update.getMessage().getDocument() != null || update.getMessage().getText() != null)
                || update.getInlineQuery() != null || update.getCallbackQuery() != null) {
                botAction(update);
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

    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(new FileInputStream("./src/main/resources/logging.properties"));
        TelegramBotsApi botsApi = new TelegramBotsApi();
        ApiContextInitializer.init();
        DataAccessObject.register();
        clients = new LinkedList<Client>();
        try {
            botsApi.registerBot(new GeodeticBot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}
