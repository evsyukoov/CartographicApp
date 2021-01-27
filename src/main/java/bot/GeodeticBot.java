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

public class GeodeticBot extends TelegramLongPollingBot {

    public static DAO dao;
    final String token = "";

    public int uploadFile(Update update)
    {
        Document doc = update.getMessage().getDocument();
        try {
            URL url = new URL("https://api.telegram.org/bot"
                    + token + "/getFile?file_id=" + doc.getFileId());
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String res = in.readLine();
            System.out.println(res);
            JSONObject jresult = new JSONObject(res);
            JSONObject path = jresult.getJSONObject("result");
            String file_path = path.getString("file_path");
            URL downoload = new URL("https://api.telegram.org/file/bot" + token + "/" + file_path);
            FileOutputStream fos = new FileOutputStream("./tmp");
            System.out.println("Start upload");
            ReadableByteChannel rbc = Channels.newChannel(downoload.openStream());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
            System.out.println("Uploaded!");
        }
        catch (MalformedURLException e)
        {
            System.out.println("URL error");
            return (0);
        }
        catch (IOException e)
        {
            System.out.println("openStream error");
            return (0);
        }
        return (1);
    }

    @Override
    public void onUpdateReceived(Update update) {
        BotState bs;
        int state;
        if (update.getMessage() != null &&
                (update.getMessage().getDocument() != null || !update.getMessage().getText().isEmpty()))
        {
            long id = update.getMessage().getChat().getId();
            try {
                ClientDAO clientBD = new ClientDAO(id);
                BotContext botContext = new BotContext(this, update.getMessage(), token);
                //первый заход клиента в бот
                if (!clientBD.addNewClient()) {
                    System.out.println("Hello new");
                    bs = BotState.getStatement(0);
                    bs.writeToClient(botContext);
                    clientBD.setState(1);
                }
                //остальные
                else
                {
                    state = clientBD.getClientState();
                    bs = BotState.getStatement(state);
                    bs.readFromClient(botContext);
                    bs = bs.next(botContext);
                    bs.writeToClient(botContext);
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
