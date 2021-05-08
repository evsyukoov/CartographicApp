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

public class GeodeticBot extends TelegramWebhookBot {

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

//    @Override
//    public BotApiMethod onWebhookUpdateReceived(Update update) {
//        System.out.println(update.getMessage());
//        return new SendMessage(update.getMessage().getChatId(), "Hello from webhook");
//    }

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
                    cd.addToDataBase();
                    cd.closeConnection();
                    clients.add(client);
                    client.setState(1);
                }
                System.out.printf("Client state: %d\n", client.getState());
                bs = BotState.getStatement(client.getState());
                bs.readFromClient(botContext, client);
                bs = bs.next(botContext);
                content = bs.writeToClient(botContext, client);
                if (client.getClientReady())
                {
                    clients.remove(client);
                    System.out.println("Client delete!");
                }
            } catch (SQLException throwables) {
                System.out.println("MySQL Exception");
                throwables.printStackTrace();
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



    public static void preDownload() throws SQLException
    {
        DownloadDAO dao = new DownloadDAO();
        dao.startConnection();
        dao.startDownload();
        dao.closeConnection();
    }

    public static void main(String[] args){
        TelegramBotsApi botsApi = new TelegramBotsApi();
        ApiContextInitializer.init();
        dao = new DAO();
        dao.register();
        clients = new LinkedList<Client>();
        System.out.println("Server start!");
        try {
            botsApi.registerBot(new GeodeticBot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
        //while (true);
    }
}
