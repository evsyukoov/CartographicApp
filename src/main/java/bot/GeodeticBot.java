package bot;

import dao.ClientDAO;
import dao.DAO;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.sql.SQLException;

public class GeodeticBot extends TelegramLongPollingBot {

    public static DAO dao;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage() != null && update.hasMessage()
                && !update.getMessage().getText().isEmpty())
        {
            long id = update.getMessage().getChat().getId();
            try {
                ClientDAO cd = new ClientDAO(id);
                if (cd.addNewClient())
                {
                    System.out.println("HELLO");
                }
                else
                    System.out.println("No");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
    }

    @Override
    public String getBotUsername() {
        return "Geo";
    }

    @Override
    public String getBotToken() {
        return "1555019728:AAH3SgshB-qQR4SoW0TwkUWsfwAq-";
    }

    public static void main(String[] args) {
        ApiContextInitializer.init();
        dao = new DAO();
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
