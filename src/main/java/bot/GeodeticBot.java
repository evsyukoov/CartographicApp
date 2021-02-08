package bot;

import dao.ClientDAO;
import dao.DAO;
import org.json.JSONObject;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import javax.print.Doc;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.util.LinkedList;

public class GeodeticBot extends TelegramLongPollingBot {

    public static DAO dao;
    final String token = "1555019728:AAH3SgshB-qQR4SoW0TwkUWsfwAq-QArlKU";

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
    public void onUpdateReceived(Update update) {
        BotState bs;
        Boolean isInBD;
        Client client;
        if (update.getMessage() != null &&
                (update.getMessage().getDocument() != null || update.getMessage().getText() != null))
        {
            long id = update.getMessage().getChat().getId();
            try {
                BotContext botContext = new BotContext(this, update.getMessage(), token);
                //добавляем клиента в базу или увеличиваем счетчик заходов (для сбора статистики)
                if ((client = getClientFromId(id)) == null)
                {
                    ClientDAO cd = new ClientDAO(id);
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
                bs.writeToClient(botContext, client);
                if (client.getClientReady())
                {
                    clients.remove(client);
                    System.out.println("Client delete!");
                }
            } catch (SQLException throwables) {
                System.out.println("MySQL Exception");
                throwables.printStackTrace();
            }

        }
    }

    @Override
    public String getBotUsername() {
        return "GeodeticBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public static void main(String[] args) {
        ApiContextInitializer.init();
        dao = new DAO();
        clients = new LinkedList<Client>();
        dao.register();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(new GeodeticBot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}
