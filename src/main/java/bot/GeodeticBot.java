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
    final String token = "";

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
                (update.getMessage().getDocument() != null || !update.getMessage().getText().isEmpty()))
        {
            long id = update.getMessage().getChat().getId();
            try {
                ClientDAO clientBD = new ClientDAO(id);
                BotContext botContext = new BotContext(this, update.getMessage(), token);
                //первый заход клиента в бот
                if (!(isInBD = clientBD.addNewClient())) {
                    client = new Client(id);
                    clients.add(client);
                    bs = BotState.getStatement(0);
                    bs.writeToClient(botContext, client);
                    client.setState(1);
                }
                //для отладки
                //остальные
                else
                {
                    //клиент будет удвлен из списка при прохождении полного круга вопрос-ответ
                    //чтобы не забивать оперативку, поэтому создаем заново
                    if ((client = getClientFromId(id)) == null)
                    {
                        client = new Client(id);
                        clients.add(client);
                        client.setState(1);
                    }
                    System.out.printf("Client state: %d", client.getState());
                    bs = BotState.getStatement(client.getState());
                    bs.readFromClient(botContext, client);
                    bs = bs.next(botContext);
                    bs.writeToClient(botContext, client);
                    if (client.getClientReady())
                    {
                        clients.remove(client);
                        System.out.println("Client delete!");
                    }
                }
            } catch (Exception throwables) {
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
        try {
            dao.startConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(new GeodeticBot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}
