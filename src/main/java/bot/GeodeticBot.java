package bot;

import dao.ClientDAO;
import dao.DAO;
import dao.DownloadDAO;
import dao.InlineDAO;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.sql.SQLException;
import java.util.LinkedList;

public class GeodeticBot extends TelegramLongPollingBot {

    private static DAO dao;
    final String token = "1418694554:AAE-RAWPAq8R6Z50k4uqu4RVhBXHyxYqu3I";
    private DefaultBotOptions options;
    private final String botName = "SurveyGeoBot";

    private InlineDAO inlineDataAccess;

    public GeodeticBot(DefaultBotOptions options) {
        super(options);
    }

    public GeodeticBot() {
        inlineDataAccess = new InlineDAO();
    }

    public static LinkedList<Client> clients;

    public Client getClientFromId(long id) {
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
                (update.getMessage().getDocument() != null || update.getMessage().getText() != null)
                || update.getInlineQuery() != null || update.getCallbackQuery() != null) {
            try {
                if (update.getInlineQuery() != null) {
                    InlineMod inlineMod = new InlineMod(update, this, inlineDataAccess);
                    inlineMod.answerInline();
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
                //добавляем клиента в базу или увеличиваем счетчик заходов (для сбора статистики)
                if ((client = getClientFromId(id)) == null) {
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
                bs.writeToClient(botContext, client);
                if (client.getClientReady()) {
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
        return botName;
    }

    @Override
    public String getBotToken() {
        return token;
    }


    public static void preDownload() throws SQLException {
        DownloadDAO dao = new DownloadDAO();
        dao.startConnection();
        dao.startDownload();
        dao.closeConnection();
    }

    public static void main(String[] args) {
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
    }
}
