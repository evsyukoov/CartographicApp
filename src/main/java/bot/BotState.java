package bot;

import convert.Converter;
import convert.Transformator;
import dao.SelectDAO;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public enum BotState {
    //0
    WELCOME {
        private static final String HELLO = "Отправьте файл или текст с координатами";

        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            sm.setText(HELLO);
            setHelper(sm, "Помощь");
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext,  Client client) { }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
    },
    //1
    FILE {

        private BotState next;

        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client)
        {
            SendMessage msg = new SendMessage();
            msg.setChatId(client.getId());
            msg.setText("Отправьте файл или текст с координатами");
            setHelper(msg, "Помощь");
            return msg;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = null;
            if (checkStop(botContext, client)) {
                next = FILE;
                return ;
            }
            String text;
            //получили от клиента строку
            if ((text = botContext.getMessage().getText()) != null)
            {
                if (text.equals("Помощь"))
                {
                    client.setPrevState(FILE.ordinal());
                    client.setState(HELP.ordinal());
                    next = HELP;
                    return ;
                }
                Converter c = new Converter(text);
                if (c.readText() == 0) {
                    next = ERROR;
                    client.setErrorMSG("Неверный формат текста\nОтправьте файл или строчку с координатами");
                    return ;
                }
                next = CHOOSE_TYPE;
                client.setPointsFromFile(c.getReadedPoints());
                client.setTransformType(c.getTransformType());
                client.setExtension(".csv");
                client.setState(CHOOSE_TYPE.ordinal());
                return ;
            }
            //получили от клиента файл
            if (botContext.getMessage().getDocument() != null && uploadFile(botContext, client) == 0) {
                next = ERROR;
                client.setErrorMSG("Проблемы на сервере. Попробуйте чуть позднее");
                client.setState(FILE.ordinal());
                return ;
            }
            Converter c = new Converter(new File(client.getUploadPath()), client.getId(), client.getExtension());
            int ret = c.run();
            if (ret == 0) {
                client.setErrorMSG("Неверный формат файла\nОтправьте файл или строчку с координатами");
                next = ERROR;
                return ;
            }
            else if (ret == 2)
            {
                client.setErrorMSG("В dxf файле не найдено замкнутых полилиний и блоков\nОтправьте файл или строчку с координатами");
                next = ERROR;
                return ;
            }
            else if (ret == -1) {
                next = ERROR;
                client.setErrorMSG("Проблемы на сервере. Попробуйте чуть позднее");
                return ;
            }
            next = CHOOSE_TYPE;
            if (c.isDxf())
                client.setDxf(c.getFromDXF());
            else
                client.setPointsFromFile(c.getReadedPoints());
            client.setTransformType(c.getTransformType());
            client.setState(CHOOSE_TYPE.ordinal());
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },
    //2
    CHOOSE_TYPE {
        private BotState next;

        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            try {
                SelectDAO sd = new SelectDAO();
                sd.startConnection();
                client.setSd(sd);
                sd.selectTypes();
                sd.closeConnection();
                sm.setText("Выберите тип СК");
                setButtons(sm, sd.getTypes());
                //sendMessage(botContext,sm);
                client.setState(CHOOSE_TYPE.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return sm.setText("Проблемы на сервере");
            }
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            if (checkStop(botContext, client)) {
                next = FILE;
            }
            String recieve = botContext.getMessage().getText();
            if (recieve == null || (!client.getSd().getTypes().contains(recieve)
                    && !recieve.equals("Помощь") && !recieve.equals("Назад"))) {
                next = ERROR;
                client.setErrorMSG("Неверно выбран тип СК\nВыберите тип СК");
            }
            else {
                if (recieve.equals("Помощь"))
                {
                    client.setPrevState(CHOOSE_TYPE.ordinal());
                    client.setState(HELP.ordinal());
                    next = HELP;
                }
                else if (recieve.equals("Назад"))
                {
                    next = FILE;
                    client.setState(FILE.ordinal());
                }
                else {
                    next = CHOOSE_SK;
                    client.setState(CHOOSE_SK.ordinal());
                    client.setChoosedType(recieve);
                }
            }
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },
    //3
    CHOOSE_SK {
        private BotState next;

        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectSK(client.getChoosedType());
                sd.closeConnection();
                sm.setText("Выберите регион(район)");
                setButtons(sm, client.getSd().getSk());
                //sendMessage(botContext, sm);
                client.setState(CHOOSE_SK.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return sm.setText("Проблемы на сервере, попробуйте позднее");
            }
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = null;
            if (checkStop(botContext, client))
            {
                next = FILE;
                return;
            }
            String recieve = botContext.getMessage().getText();
            if (recieve == null || (!client.getSd().getSk().contains(recieve) &&
                    !recieve.equals("Помощь") && !recieve.equals("Назад"))) {
                next = ERROR;
                client.setErrorMSG("Неверно выбран регион(район)\nВыберите регион(район)");
            }
            else {
                if (recieve.equals("Помощь"))
                {
                    next = HELP;
                    client.setPrevState(CHOOSE_SK.ordinal());
                    client.setState(HELP.ordinal());
                    next = HELP;
                }
                else if (recieve.equals("Назад"))
                {
                    next = CHOOSE_TYPE;
                    client.setState(CHOOSE_TYPE.ordinal());
                }
                else {
                    System.out.printf("recieve: %s\n", recieve);
                    next = CHOOSE_ZONE;
                    client.setChoosedSK(recieve);
                    client.setState(CHOOSE_ZONE.ordinal());
                }
            }
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },
    //4
    CHOOSE_ZONE
    {
        BotState next;

        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectZone(client.getChoosedSK());
                sd.closeConnection();
                sm.setText("Выберите зону");
                setButtons(sm, sd.getZones());
                //sendMessage(botContext, sm);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return sm.setText("Проблемы на сервере, попробуйте позднее");
            }
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = null;
            if (checkStop(botContext, client)) {
                next = FILE;
                return;
            }
            try {
                SelectDAO sd = client.getSd();
                System.out.printf("Type: %s, SK: %s \n", client.getChoosedType(), client.getChoosedSK());
                String recieve = botContext.getMessage().getText();
                System.out.println(recieve);
                if (recieve == null || (!sd.getZones().contains(recieve) &&
                        !recieve.equals("Помощь") && !recieve.equals("Назад"))) {
                    next = ERROR;
                    client.setErrorMSG("Неверно выбрана зона\nВыберите зону");
                 }
                 else
                 {
                     if (recieve.equals("Помощь"))
                     {
                         client.setPrevState(CHOOSE_ZONE.ordinal());
                         client.setState(HELP.ordinal());
                         next = HELP;
                     }
                     else if (recieve.equals("Назад"))
                     {
                         next = CHOOSE_SK;
                         client.setState(CHOOSE_SK.ordinal());
                     }
                     else
                     {
                         next = EXECUTE;
                         sd.startConnection();
                         sd.selectParam(client.getChoosedType(), client.getChoosedSK(), recieve);
                         sd.closeConnection();
                         Transformator transformator;
                         if (client.getDxf() != null)
                             transformator = new Transformator(sd.getParam(), client.getDxf(),client.getSavePath(), client.getTransformType());
                         else
                            transformator = new Transformator(sd.getParam(), client.getPointsFromFile(),client.getSavePath(), client.getTransformType());
                         if (transformator.transform() == 0) {
                             client.setErrorMSG("Ошибка трансформации");
                             next = TRANSFORM_ERROR;
                             client.setState(FILE.ordinal());
                         }
                         client.setFiles(transformator.getFiles());
                     }
                 }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }


        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },

    //последний  стейт если все хорошо
    EXECUTE {
        @Override public BotApiMethod writeToClient(BotContext botContext, Client client) {
            for(int i = 0;i < client.getFiles().size(); i++)
                sendFile(botContext, client.getFiles().get(i));
            client.setClientReady(true);
            SendMessage sm = new SendMessage();
            sm.setText("Отправьте файл или текст с координатами");
            setHelper(sm, "Помощь");
            sendMessage(botContext, sm);
            return null;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) { }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
        },

    //6
    ERROR {
        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            sm.setText(client.getErrorMSG());
           // sendMessage(botContext, sm);
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
            }
        },
    //7
    TRANSFORM_ERROR {
        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            sm.setText(client.getErrorMSG());
            sendMessage(botContext, sm);
            sm.setText("Отправьте файл или текст с координатами");
            setHelper(sm, "Помощь");
            //sendMessage(botContext, sm);
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {}

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    },
    //8
    HELP {
        private BotState next;
        private static final String HELPER = "Принимаемые типы файлов: kml, kmz, csv, txt, текст\n" +
                                            "Формат  текста: Point; North; East; (Elevation)\n" +
                                            "Все преобразования выполняются на Transverse Mercator\n" +
                                            "Разделитель целой и дробной части - точка\n" +
                                            "Формат WGS-координат Lat(Long) = DD.DDDDD(градусы и десятичные доли градуса)\n" +
                                            "Для возврата в начало разговора введите /stop\n" +
                                            "Добавлена поддержка dxf. Из чертежа вытягиваются все блоки и полилилинии\n" +
                                            "В качестве имени точки берется первое непустое значение атрибута блока";
        @Override
        public BotApiMethod writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setChatId(client.getId());
            sm.setText(HELPER);
            setHelper(sm, "Назад");
            sendMessage(botContext, sm);
            return sm;
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            String recieve = botContext.getMessage().getText();
            next = null;
            if (recieve != null && recieve.equals("Назад"))
            {
                int prevState = client.getPrevState();
                client.setState(prevState);
                next = BotState.getStatement(prevState);
            }
            else if (recieve != null && recieve.equals("/stop"))
            {
                client.setState(FILE.ordinal());
                next = FILE;
            }
        }

        @Override
        public BotState next(BotContext botContext) {
            if (next != null)
                return next;
            return HELP;
        }
    };



    private static BotState[] statements;

    public static BotState getStatement(int state) {
        if (statements == null)
            statements = BotState.values();
        return statements[state];
    }



    public abstract BotApiMethod writeToClient(BotContext botContext, Client client);

    public abstract void readFromClient(BotContext botContext, Client client);

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

    public void sendFile(BotContext botContext, File file) {
        SendDocument doc = new SendDocument();
        doc.setDocument(file);
        doc.setChatId(botContext.getMessage().getChat().getId());
        try {
            botContext.getBot().execute(doc);
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

    private void    findExtension(String path, Client client)
    {
        int pos = path.indexOf('.');
        if (pos == -1)
            client.setExtension("csv");
        else {
            client.setExtension(path.substring(pos + 1));
        }
    }

    public int uploadFile(BotContext botContext, Client client) {
        Document doc = botContext.getMessage().getDocument();
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
            if (client.getExtension().equalsIgnoreCase("csv"))
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

    public synchronized void setHelper(SendMessage sendMessage, String action)
    {
        ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
        rkm.setResizeKeyboard(true);
        rkm.setSelective(true);
        //rkm.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow helpers = new KeyboardRow();
        helpers.add(new KeyboardButton(action));
        keyboard.add(helpers);
        rkm.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(rkm);
    }

    public synchronized void setButtons(SendMessage sendMessage, ArrayList<String> buttons) {
        ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
        rkm.setSelective(true);
        rkm.setResizeKeyboard(true);
        //rkm.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow helpers = new KeyboardRow();
        helpers.add(new KeyboardButton("Помощь"));
        helpers.add(new KeyboardButton("Назад"));
        keyboard.add(helpers);
        for (int i = 0; i < buttons.size(); i++) {
            KeyboardRow kr = new KeyboardRow();
            kr.add(new KeyboardButton(buttons.get(i)));
            keyboard.add(kr);
        }
        rkm.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(rkm);
    }

};
