package bot;

import convert.Converter;
import convert.Transformator;
import dao.ClientDAO;
import dao.SelectDAO;
import org.json.JSONObject;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public enum BotState {
    //0
    WELCOME {
        private static final String HELLO = "Что умеет этот бот?\nОтправьте ему csv или строчку в формате: \n" +
                "Имя точки, X, Y, Z(опционально)\n" +
                "Выберите систему координат и зону из предложенных\n" +
                "В ответ получите kml файл\n" +
                "Пример форматирования: Rp12, 123111.23, 456343.79";

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            if (sendMessage(botContext, HELLO) == 0)
                return (0);
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext,  Client client) {
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
    },
    //1
    FILE {
        private Boolean isFileFormattedWell;
        private Boolean isStopped;

        @Override
        public int writeToClient(BotContext botContext, Client client)
        {
            sendMessage(botContext, "Отправьте файл или строчку с координатами");
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            isFileFormattedWell = true;
            if ((isStopped = checkStop(botContext, client)))
                return (0);
            String text;
            //получили от клиента строку
            if ((text = botContext.getMessage().getText()) != null)
            {
                Converter c = new Converter(text);
                if (c.readLine() == 0) {
                    isFileFormattedWell = false;
                    return (0);
                }
                client.setPointsFromFile(c.getReadedPoints());
                client.setState(2);
                return (1);
            }
            //получили от клиента файл
            if (botContext.getMessage().getDocument() != null && uploadFile(botContext, client) == 0)
                return (0);
            Converter c = new Converter(new File(client.getUploadPath()));
            if (c.readFile() == 0) {
                isFileFormattedWell = false;
                return (0);
            }
            client.setPointsFromFile(c.getReadedPoints());
            client.setState(2);
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            if (isStopped)
                return WELCOME;
            if (isFileFormattedWell)
                return CHOOSE_TYPE;
            return ERROR_PARSING;
        }
    },
    //2
    CHOOSE_TYPE {
        private Boolean isRightAnswer;
        private ArrayList<String> availableTypes;
        private Boolean isStopped;
        private Boolean skip;

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = new SelectDAO();
                sd.selectTypes();
                availableTypes = sd.getTypes();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите тип СК");
                setButtons(sm, availableTypes);
                if (sendMessage(botContext, sm) == 0)
                    return (0);
                client.setState(2);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            skip = false;
            isRightAnswer = false;
            if ((isStopped = checkStop(botContext, client)))
                return (0);
            String recieve = botContext.getMessage().getText();
            if (recieve == null || !availableTypes.contains(recieve))
                isRightAnswer = false;
            else {
                if (recieve.equals("SK-42")) {
                    skip = true;
                    client.setState(4);
                    client.setChoosedType(recieve);
                    client.setChoosedSK("None");
                    return (1);
                }
                isRightAnswer = true;
                client.setState(3);
                client.setChoosedType(recieve);
            }
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            if (isStopped)
                return WELCOME;
            if (isRightAnswer)
                return CHOOSE_SK;
            if (skip)
                return CHOOSE_ZONE;
            return ERROR_INPUT;
        }
    },
    //3
    CHOOSE_SK {
        private Boolean isRightAnswer;
        private ArrayList<String> availableSK;
        private Boolean isStopped;

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = new SelectDAO();
                sd.selectSK(client.getChoosedType());
                availableSK = sd.getSk();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите регион(район)");
                setButtons(sm, availableSK);
                if (sendMessage(botContext, sm) == 0)
                    return (0);
                client.setState(3);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            if ((isStopped = checkStop(botContext, client)))
                return (0);
            String recieve = botContext.getMessage().getText();
            if (recieve == null || !availableSK.contains(recieve))
                isRightAnswer = false;
            else {
                isRightAnswer = true;
                client.setChoosedSK(recieve);
                client.setState(4);
            }
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            if (isStopped)
                return WELCOME;
            if (isRightAnswer)
                return CHOOSE_ZONE;
            return ERROR_INPUT;
        }
    },
    //4
    CHOOSE_ZONE
    {
        ArrayList<String> availableZones;
        private Boolean isRightAnswer;
        private Boolean badTransform ;
        private Boolean isStopped;

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = new SelectDAO();
                sd.selectZone(client.getChoosedSK());
                availableZones = sd.getZones();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите зону");
                setButtons(sm, availableZones);
                if (sendMessage(botContext, sm) == 0)
                    return (0);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            isRightAnswer = false;
            badTransform = false;
            if ((isStopped = checkStop(botContext, client)))
                return (0);
            try {
                SelectDAO sd = new SelectDAO();
                String recieve = botContext.getMessage().getText();
                 if (recieve == null || availableZones.contains(recieve)){
                    isRightAnswer = true;
                    sd.selectParam(client.getChoosedType(), client.getChoosedSK(), recieve);
                    Transformator transformator = new Transformator(sd.getParam(), client.getPointsFromFile(),client.getSavePath());
                    if (transformator.transform() == 0) {
                        sendMessage(botContext, "Ошибка транcформации");
                        badTransform = true;
                        client.setState(1);
                    }
                    client.setFiles(transformator.getFiles());
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }


        @Override
        public BotState next(BotContext botContext) {
            if (isStopped)
                return WELCOME;
            if (badTransform)
                return FILE;
            if (isRightAnswer)
                return EXECUTE;
            return ERROR_INPUT;
        }
    },

    //последний  стейт если все хорошо
    EXECUTE {
        @Override public int writeToClient(BotContext botContext, Client client) {
            deleteButtons(botContext);
            for(int i = 0;i < client.getFiles().size(); i++)
                sendFile(botContext, client.getFiles().get(i));
            client.setClientReady(true);
            sendMessage(botContext, "Отправьте файл или строчку с координатами");
            System.out.printf("FULL CIRCLE client id = %d\n", client.getId());
            return 0;
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            return 0;
        }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
        },

    //6
    ERROR_INPUT {
        @Override
        public int writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText("Неверный выбор");
            sendMessage(botContext, sm);
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
            }
        },

    //7
    ERROR_PARSING
    {
        @Override
        public int writeToClient(BotContext botContext, Client client) {
        SendMessage sm = new SendMessage();
        sm.setText("Ошибка парсинга файла");
        sendMessage(botContext, sm);
        return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
        return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
        return null;
        }
    },
    //8
    ERROR_TRANSFORMATION
            {
                @Override
                public int writeToClient(BotContext botContext, Client client) {
                    SendMessage sm = new SendMessage();
                    sm.setText("Ошибка трансформации");
                    sendMessage(botContext, sm);
                    return (1);
                }

                @Override
                public int readFromClient(BotContext botContext, Client client) {
                    return (1);
                }

                @Override
                public BotState next(BotContext botContext) {
                    return null;
                }
            };


    private static BotState[] statements;

    public static BotState getStatement(int state) {
        if (statements == null)
            statements = BotState.values();
        return statements[state];
    }


    private String uploadFile;

    public abstract int writeToClient(BotContext botContext, Client client);

    public abstract int readFromClient(BotContext botContext, Client client);

    public abstract BotState next(BotContext botContext);

    public  Boolean checkStop(BotContext botContext, Client client)
    {
        String msg = botContext.getMessage().getText();
        if (msg != null && (msg.equals("/stop") || botContext.getMessage().getText().equals("/start")))
        {
            client.setState(1);
            return true;
        }
        return false;
    }

    public void     deleteButtons(BotContext botContext)
    {
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        SendMessage msg = new SendMessage();
        msg.setReplyMarkup(replyKeyboardRemove);
        msg.setText("Готово!");
        sendMessage(botContext, msg);
    }


    public void sendFile(BotContext botContext, File file) {
        SendMessage sendMessage = new SendMessage();
        SendDocument doc = new SendDocument();
        doc.setNewDocument(file);
        doc.setChatId(botContext.getMessage().getChat().getId());
        try {
            botContext.getBot().sendDocument(doc);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public int sendMessage(BotContext botContext, SendMessage sm)
    {
        sm.setChatId(botContext.getMessage().getChat().getId());
        try {
            botContext.getBot().execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return (0);
        }
        return (1);
    }

    public int sendMessage(BotContext botContext, String text) {
        SendMessage sm = new SendMessage();
        sm.setText(text);
        sm.setChatId(botContext.getMessage().getChat().getId());
        try {
            botContext.getBot().execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return (0);
        }
        return (1);
    }

    public int uploadFile(BotContext botContext, Client client) {
        Document doc = botContext.getMessage().getDocument();
        uploadFile = "./resources/uploaded/file_" + botContext.getMessage().getChat().getId().toString();
        try {
            URL url = new URL("https://api.telegram.org/bot"
                    + botContext.getToken() + "/getFile?file_id=" + doc.getFileId());
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String res = in.readLine();
            System.out.println(res);
            JSONObject jresult = new JSONObject(res);
            JSONObject path = jresult.getJSONObject("result");
            String file_path = path.getString("file_path");
            URL downoload = new URL("https://api.telegram.org/file/bot" + botContext.getToken() + "/" + file_path);
            Writer fw  = new OutputStreamWriter(new FileOutputStream(client.getUploadPath()), "utf-8");
            System.out.println("Start upload");
            BufferedReader uploadIn = new BufferedReader(new InputStreamReader(downoload.openStream()));
            String s;
            while ((s = uploadIn.readLine()) != null)
                fw.write(s);
            fw.close();
            uploadIn.close();
            in.close();
            System.out.println("Uploaded!");
        } catch (MalformedURLException e) {
            System.out.println("URL error");
            return (0);
        } catch (IOException e) {
            System.out.println("openStream error");
            return (0);
        }
        return (1);
    }

    public synchronized void setButtons(SendMessage sendMessage, ArrayList<String> buttons) {
        ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
        rkm.setSelective(true);
        rkm.setResizeKeyboard(true);
        rkm.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        for (int i = 0; i < buttons.size(); i++) {
            KeyboardRow kr = new KeyboardRow();
            kr.add(new KeyboardButton(buttons.get(i)));
            keyboard.add(kr);
        }
        rkm.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(rkm);
    }

};
