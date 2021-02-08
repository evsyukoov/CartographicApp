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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public enum BotState {
    //0
    WELCOME {
        private static final String HELLO = "Отправьте файл или строчку с координатами";

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
        private Boolean isStopped;

        private Boolean error;

        @Override
        public int writeToClient(BotContext botContext, Client client)
        {
            deleteButtons(botContext);
            //sendMessage(botContext, "Отправьте файл или строчку с координатами");
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext, Client client) {
            error = false;
            if ((isStopped = checkStop(botContext, client)))
                return (0);
            String text;
            //получили от клиента строку
            if ((text = botContext.getMessage().getText()) != null)
            {
                Converter c = new Converter(text);
                if (c.readLine() == 0) {
                    error = true;
                    client.setErrorMSG("Неверный формат текста\nОтправьте файл или строчку с координатами");
                    return (0);
                }
                client.setPointsFromFile(c.getReadedPoints());
                client.setTransformType(c.getTransformType());
                client.setExtension(".csv");
                client.setState(2);
                return (1);
            }
            //получили от клиента файл
            if (botContext.getMessage().getDocument() != null && uploadFile(botContext, client) == 0) {
                error = true;
                client.setErrorMSG("Проблемы на сервере. Попробуйте чуть позднее");
                client.setState(1);
                return (0);
            }
            Converter c = new Converter(new File(client.getUploadPath()), client.getId(), client.getExtension());
            int ret = c.run();
            if (ret == 0) {
                client.setErrorMSG("Неверный формат файла\nОтправьте файл или строчку с координатами");
                error = true;
                return (0);
            }
            else if (ret == -1) {
                error = true;
                client.setErrorMSG("Проблемы на сервере. Попробуйте чуть позднее");
                return (0);
            }
            client.setPointsFromFile(c.getReadedPoints());
            client.setTransformType(c.getTransformType());
            client.setState(2);
            c.print();
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            if (error)
                return ERROR;
            if (isStopped)
                return FILE;
            return CHOOSE_TYPE;
        }
    },
    //2
    CHOOSE_TYPE {
        private Boolean isRightAnswer;
        private Boolean isStopped;
        private Boolean skip;

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = new SelectDAO();
                sd.startConnection();
                client.setSd(sd);
                sd.selectTypes();
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите тип СК");
                setButtons(sm, sd.getTypes());
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
            isRightAnswer = true;
            if ((isStopped = checkStop(botContext, client)))
                return (0);
            String recieve = botContext.getMessage().getText();
            if (recieve == null || !client.getSd().getTypes().contains(recieve)) {
                isRightAnswer = false;
                client.setErrorMSG("Неверно выбран тип СК\nВыберите тип СК");
            }
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
                return FILE;
            if (isRightAnswer)
                return CHOOSE_SK;
            if (skip)
                return CHOOSE_ZONE;
            return ERROR;
        }
    },
    //3
    CHOOSE_SK {
        private Boolean isRightAnswer;
        private Boolean isStopped;

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            try {
                client.getSd().startConnection();
                client.getSd().selectSK(client.getChoosedType());
                client.getSd().closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите регион(район)");
                setButtons(sm, client.getSd().getSk());
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
            if (recieve == null || !client.getSd().getSk().contains(recieve)) {
                isRightAnswer = false;
                client.setErrorMSG("Неверно выбран регион(район)\nВыберите регион(район)");
            }
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
                return FILE;
            if (isRightAnswer)
                return CHOOSE_ZONE;
            return ERROR;
        }
    },
    //4
    CHOOSE_ZONE
    {
        private Boolean isRightAnswer;
        private Boolean badTransform ;
        private Boolean isStopped;

        @Override
        public int writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectZone(client.getChoosedSK());
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите зону");
                setButtons(sm, client.getSd().getZones());
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
                SelectDAO sd = client.getSd();
                String recieve = botContext.getMessage().getText();
                 if (recieve == null || client.getSd().getZones().contains(recieve)){
                    isRightAnswer = true;
                    sd.startConnection();
                    sd.selectParam(client.getChoosedType(), client.getChoosedSK(), recieve);
                    sd.closeConnection();
                    Transformator transformator = new Transformator(sd.getParam(), client.getPointsFromFile(),client.getSavePath(), client.getTransformType());
                    if (transformator.transform() == 0) {
                        client.setErrorMSG("Ошибка трансформации");
                        badTransform = true;
                        client.setState(1);
                    }
                          client.setFiles(transformator.getFiles());
                }
                 else  {
                     client.setErrorMSG("Неверно выбрана зона\nВыберите зону");
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
                return  FILE;
            if (badTransform)
                return TRANSFORM_ERROR;
            if (isRightAnswer)
                return EXECUTE;
            return ERROR;
        }
    },

    //последний  стейт если все хорошо
    EXECUTE {
        @Override public int writeToClient(BotContext botContext, Client client) {
            for(int i = 0;i < client.getFiles().size(); i++)
                sendFile(botContext, client.getFiles().get(i));
            client.setClientReady(true);
            deleteButtons(botContext);
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
    ERROR {
        @Override
        public int writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(client.getErrorMSG());
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

    TRANSFORM_ERROR {
        @Override
        public int writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(client.getErrorMSG());
            sendMessage(botContext, sm);
            deleteButtons(botContext);
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
        msg.setText("Отправьте файл или строчку с координатами");
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

    private void    findExtension(String path, Client client)
    {
        int pos = path.indexOf('.');
        if (pos == -1)
            client.setExtension(".csv");
        else {
            client.setExtension(path.substring(pos + 1));
        }
    }

    public int uploadFile(BotContext botContext, Client client) {
        Document doc = botContext.getMessage().getDocument();
        uploadFile = "./src/main/resources/uploaded/file_" + botContext.getMessage().getChat().getId().toString();
        try {
            URL url = new URL("https://api.telegram.org/bot"
                    + botContext.getToken() + "/getFile?file_id=" + doc.getFileId());
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String res = in.readLine();
            System.out.println(res);
            JSONObject jresult = new JSONObject(res);
            JSONObject path = jresult.getJSONObject("result");
            String file_path = path.getString("file_path");
            findExtension(file_path, client);
            URL downoload = new URL("https://api.telegram.org/file/bot" + botContext.getToken() + "/" + file_path);
            if (client.getExtension().equals("kmz"))
                return (uploadZIP(downoload, client));
            Writer fw  = new OutputStreamWriter(new FileOutputStream(client.getUploadPath()), StandardCharsets.UTF_8);
            String charset;
            if (client.getExtension().equals("csv"))
                charset = "windows-1251";
            else
                charset = "UTF-8";
            BufferedReader uploadIn = new BufferedReader(new InputStreamReader(downoload.openStream(), charset));
            String s;
            while ((s = uploadIn.readLine()) != null)
                fw.write(String.format("%s\n",s ));
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

    public int uploadZIP(URL download, Client client)
    {
        try {
            ReadableByteChannel rbc = Channels.newChannel(download.openStream());
            FileOutputStream fos = new FileOutputStream(client.getUploadPath());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
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
