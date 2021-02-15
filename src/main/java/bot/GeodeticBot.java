package bot;

import dao.ClientDAO;
import dao.DAO;
import dao.DownloadDAO;

import org.kabeja.dxf.*;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.dxf.helpers.Vector;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.ParserBuilder;
import org.kabeja.parser.entities.DXF3DFaceHandler;
import org.kabeja.xml.SAXGenerator;
import org.kabeja.xml.SAXSerializer;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.parser.Parser;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;

public class GeodeticBot extends TelegramLongPollingBot {

    private static DAO dao;
    final String token = "1418694554:AAE-RAWPAq8R6Z50k4uqu4RVhBXHyxYqu3I";

    private boolean isUpdateSend = false;

    public static LinkedList<Client> clients;

    //функция для рассылки сообщений об обновлениях

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
        return "SurveyGeoBot";
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

    //отправить сообщение об обновлении
    public void   sendUpdateInfo()
    {
        ClientDAO cd = new ClientDAO();
        try {
            cd.startConnection();
            List<Long> lst = cd.getAllClients();
            cd.closeConnection();
            for (Long aLong : lst) {
                SendMessage sm = new SendMessage();
                sm.setText("Обноление 1.1. Улучшена навигация по меню");
                sm.setChatId((long)349939502);
                execute(sm);
            }

        }
        catch (TelegramApiException | SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        ApiContextInitializer.init();
        dao = new DAO();
        dao.register();
        clients = new LinkedList<Client>();
        System.out.println("Server start!");
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(new GeodeticBot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}
