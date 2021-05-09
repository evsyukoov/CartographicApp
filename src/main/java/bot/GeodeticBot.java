package bot;

import dao.ClientDAO;
import dao.DAO;
import dao.DownloadDAO;

import org.apache.tomcat.util.net.AprEndpoint;
import org.telegram.telegrambots.ApiContextInitializer;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.WebhookBot;
import org.telegram.telegrambots.util.WebhookUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeodeticBot extends TelegramWebhookBot {

    private static final java.util.logging.Logger logger = Logger.getLogger(BotState.class.getName());

    private static DAO dao;
    final String token = "1418694554:AAE-RAWPAq8R6Z50k4uqu4RVhBXHyxYqu3I";
    private DefaultBotOptions options;
    private final String botName = "SurveyGeoBot";

    public GeodeticBot(DefaultBotOptions options) {
        super(options);
    }

    public GeodeticBot() {
        dao = new DAO();
        dao.register();
        clients = new LinkedList<Client>();
    }

    public static LinkedList<Client> clients;

    public Client   getClientFromId(long id)
    {
        for (Client c : clients) {
            if (id == c.getId())
                return (c);
        }
        return null;
    }

    @Override
    public String getBotPath() {
        return null;
    }

        @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        BotState bs;
        Client client = null;
        BotApiMethod content = null;
        if (update.getMessage() != null &&
                (update.getMessage().getDocument() != null || update.getMessage().getText() != null))
        {
            Chat chat = update.getMessage().getChat();
            long id = update.getMessage().getChat().getId();
            try {
                BotContext botContext = new BotContext(this, update.getMessage(), token);
                //добавляем клиента в базу или увеличиваем счетчик заходов (для сбора статистики)
                if ((client = getClientFromId(id)) == null)
                {
                    ClientDAO cd = new ClientDAO(id, chat.getFirstName(), chat.getLastName(), chat.getUserName());
                    cd.startConnection();
                    client = new Client(id);
                    client.setName(chat.getFirstName() + (chat.getLastName() == null ? "" : chat.getLastName()));
                    cd.addToDataBase();
                    cd.closeConnection();
                    clients.add(client);
                    client.setState(1);
                }
                logger.log(Level.INFO, String.format("Client %s's state is %d\n",
                        client.getName(), client.getState()));
                bs = BotState.getStatement(client.getState());
                bs.readFromClient(botContext, client);
                bs = bs.next(botContext);
                content = bs.writeToClient(botContext, client);
                if (client.getClientReady())
                {
                    clients.remove(client);
                    logger.log(Level.INFO, String.format("Client %s's delete from Active List\n",
                          client.getName()));
                }
            } catch (SQLException throwables) {
                logger.log(Level.SEVERE, "MySQL Exception");
                SendMessage error = new SendMessage();
                error.setChatId(client.getId());
                error.setText("Проблемы на сервере, попробуйте позднее");
                return error;
            }

        }
        return content;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

}
