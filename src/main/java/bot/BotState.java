package bot;

import bot.enums.InputCoordinatesType;
import bot.enums.TransType;
import bot.outputgenerators.GeneratorManager;
import convert.InfoReader;
import convert.Transformator;
import dao.SelectDAO;
import org.json.JSONObject;
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
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(HELLO);
            setHelper(sm, "Помощь");
            sendMessage(botContext, sm);
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
        public void writeToClient(BotContext botContext, Client client)
        {
            SendMessage msg = new SendMessage();
            msg.setText("Отправьте файл или текст с координатами");
            setHelper(msg, "Помощь");
            sendMessage(botContext, msg);
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
                InfoReader c = new InfoReader(text);
                if (c.readText() == 0) {
                    next = ERROR;
                    client.setErrorMSG("Неверный формат текста\nОтправьте файл или строчку с координатами");
                    return ;
                }
                next = CHOOSE_TYPE;
                client.setInfoReader(c);
                client.setExtension("csv");
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
            InfoReader c = new InfoReader(new File(client.getUploadPath()), client.getId(), client.getExtension());
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
            next = CHOOSE_OUTPUT_FILE_OPTION;
            client.setInfoReader(c);
            client.setState(next.ordinal());
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },

    CHOOSE_OUTPUT_FILE_OPTION {
        private BotState next;
        ArrayList<String> buttons;
        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            buttons = new ArrayList<>();
            sm.setText("Выберите формат выходного файла");
            buttons.add("GPX");
            buttons.add("KML");

            if (client.getExtension().equals("csv") && client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.WGS)
                buttons.add("CSV(плоские)");
            else if (client.getExtension().equals("csv") && client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK) {
                buttons.add("CSV(плоские)");
                buttons.add("CSV(WGS-84)");
            }
            setButtons(sm, buttons);
            sendMessage(botContext, sm);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = readClientChoice(botContext, client, FILE, CHOOSE_TYPE, "Неверный формат выходного файла\n" +
                    "Выберите формат выходного файла", buttons);
            client.analizeTransformationType(botContext.getMessage().getText());
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
        public void writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = new SelectDAO();
                sd.startConnection();
                client.setSd(sd);
                sd.selectTypes();
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите тип СК исходного файла");
                setButtons(sm, sd.getTypes());
                sendMessage(botContext,sm);
                client.setState(CHOOSE_TYPE.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = readClientChoice(botContext, client, CHOOSE_OUTPUT_FILE_OPTION, CHOOSE_SK,
                    "Неверно выбран тип  CК\nВыберите тип СК", client.getSd().getTypes());
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
        public void writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectSK(client.getChoosedType());
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите регион(район) исходного файла");
                setButtons(sm, client.getSd().getSk());
                sendMessage(botContext, sm);
                client.setState(CHOOSE_SK.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
           next = readClientChoice(botContext, client, CHOOSE_TYPE,
                   CHOOSE_ZONE, "Неверно выбран регион(район)\nВыберите регион(район)",
                   client.getSd().getSk());
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
        public void writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectZone(client.getChoosedSK());
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите зону исходного файла");
                setButtons(sm, sd.getZones());
                sendMessage(botContext, sm);
                client.setState(CHOOSE_ZONE.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = readClientChoice(botContext, client, CHOOSE_SK, EXECUTE, "Неверно выбрана зона\nВыберите зону", client.getSd().getZones());
            client.setTransformationParametrs(client.getSd().getParam());
        }


        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },

    // 3 дополнительных стейта для случая если пользователь хочет перейти из плоской СК в другую плоскую СК
    // нужно сначала выбрать СК исходного файла, а затем СК результата

     CHOOSE_TYPE_TGT {
        BotState next;

         @Override
         public void writeToClient(BotContext botContext, Client client) {
             try {
                 SelectDAO sd = new SelectDAO();
                 sd.startConnection();
                 client.setSd(sd);
                 sd.selectTypes();
                 sd.closeConnection();
                 SendMessage sm = new SendMessage();
                 sm.setText("Выберите тип СК результирующего файла");
                 setButtons(sm, sd.getTypes());
                 sendMessage(botContext,sm);
                 client.setState(CHOOSE_TYPE_TGT.ordinal());
             }
             catch (SQLException e)
             {
                 e.printStackTrace();
             }
         }

         @Override
         public void readFromClient(BotContext botContext, Client client) {
             next = readClientChoice(botContext, client, CHOOSE_ZONE, CHOOSE_SK_TARGET,
                     "Неверно выбран тип  CК\nВыберите тип СК", client.getSd().getTypes());
         }

         @Override
         public BotState next(BotContext botContext) {
             return next;
         }
     },

    CHOOSE_SK_TARGET {
        BotState next;

        @Override
        public void writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectSK(client.getChoosedType());
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите регион(район) результирующего файла");
                setButtons(sm, client.getSd().getSk());
                sendMessage(botContext, sm);
                client.setState(CHOOSE_SK_TARGET.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = readClientChoice(botContext, client, CHOOSE_TYPE_TGT, CHOOSE_ZONE_TARGET,
                    "Неверно выбран регион(район)\nВыберите тип СК", client.getSd().getSk());
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },

    CHOOSE_ZONE_TARGET {
        BotState next;

        @Override
        public void writeToClient(BotContext botContext, Client client) {
            try {
                SelectDAO sd = client.getSd();
                sd.startConnection();
                sd.selectZone(client.getChoosedSK());
                sd.closeConnection();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите зону результирующего файла");
                setButtons(sm, sd.getZones());
                sendMessage(botContext, sm);
                client.setState(CHOOSE_ZONE_TARGET.ordinal());
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            next = readClientChoice(botContext, client, CHOOSE_SK_TARGET, EXECUTE,
                    "Неверно выбрана зона\nВыберите зону", client.getSd().getZones());
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },


    //последний  стейт если все хорошо
    EXECUTE {
        BotState next;
        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SelectDAO sd = client.getSd();
            try {
                sd.startConnection();
                sd.selectParam(client.getChoosedType(), client.getChoosedSK(), client.getChoosedZone());
                client.setTransformationParametrs(sd.getParam());
                if (client.getTransType() == TransType.MSK_TO_MSK)
                {
                    sd.selectParam(client.getTargetType(), client.getTargetSk(), client.getTargetZone());
                    client.setSecondTransformationParamters(sd.getParam());
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            GeneratorManager gm = new GeneratorManager(client);
            if (gm.run() == 0) {
                client.setErrorMSG("Ошибка трансформации");
                next = TRANSFORM_ERROR;
                client.setState(FILE.ordinal());
            }
            else {
                sendFile(botContext, gm.getOutput());
                client.setClientReady(true);
                SendMessage sm = new SendMessage();
                sm.setText("Отправьте файл или текст с координатами");
                setHelper(sm, "Помощь");
                sendMessage(botContext, sm);
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) { }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
        },

    ERROR {
        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(client.getErrorMSG());
            sendMessage(botContext, sm);
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
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(client.getErrorMSG());
            sendMessage(botContext, sm);
            sm.setText("Отправьте файл или текст с координатами");
            setHelper(sm, "Помощь");
            sendMessage(botContext, sm);
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
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(HELPER);
            setHelper(sm, "Назад");
            sendMessage(botContext, sm);
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



    public abstract void writeToClient(BotContext botContext, Client client);

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
            client.setExtension(path.substring(pos + 1).toLowerCase());
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

    public  BotState readClientChoice(BotContext botContext, Client client,
                                      BotState back, BotState next, String error, List<String> checkList)
    {
        BotState res;
        if (checkStop(botContext, client)) {
            res = FILE;
            return res;
        }
        String recieve = botContext.getMessage().getText();
        if (recieve == null || (!checkList.contains(recieve)
                && !recieve.equals("Помощь") && !recieve.equals("Назад"))) {
            res = ERROR;
            client.setErrorMSG(error);
        }
        else {
            if (recieve.equals("Помощь"))
            {
                client.setPrevState(CHOOSE_TYPE.ordinal());
                client.setState(HELP.ordinal());
                res = HELP;
            }
            else if (recieve.equals("Назад"))
            {
                res = back;
                client.setState(back.ordinal());
            }
            else
                res = setClientParamOfCurrentState(client, recieve, next);
        }
        return res;
    }

    public     BotState    setClientParamOfCurrentState(Client client, String receive, BotState nxt)
    {
        BotState res = nxt;
        if (client.getState() == CHOOSE_OUTPUT_FILE_OPTION.ordinal())
        {
            // вообще пропускаем выбор системы координат
            if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.WGS && (receive.equals("GPX")
            || receive.equals("KML")))
            {
                 client.setState(EXECUTE.ordinal());
                 res = EXECUTE;
            }
            else if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK &&
                    (receive.equals("KML") || receive.equals("GPX") || receive.equals("CSV(WGS)")))
                client.setState(nxt.ordinal());
        }
        else if (client.getState() == CHOOSE_SK.ordinal()) {
            client.setState(nxt.ordinal());
            client.setChoosedSK(receive);
        }
        else if (client.getState() == CHOOSE_TYPE.ordinal())
        {
            client.setState(nxt.ordinal());
            client.setChoosedType(receive);
        }
        // тут смотрим какой тип преобразования и либо завершаем стейты либо спрашиваем вторую систему координат
        else if (client.getState() == CHOOSE_ZONE.ordinal())
        {
            if (client.getTransType() != TransType.MSK_TO_MSK)
                client.setState(EXECUTE.ordinal());
            else
            {
                client.setState(CHOOSE_TYPE_TGT.ordinal());
                res = CHOOSE_TYPE_TGT;
            }
            client.setChoosedZone(receive);
        }
        else if (client.getState() == CHOOSE_TYPE_TGT.ordinal())
        {
            client.setState(nxt.ordinal());
            client.setTargetType(receive);
        }
        else if (client.getState() == CHOOSE_SK_TARGET.ordinal())
        {
            client.setState(nxt.ordinal());
            client.setTargetSk(receive);
        }
        else if (client.getState() == CHOOSE_ZONE_TARGET.ordinal())
        {
            client.setState(EXECUTE.ordinal());
            client.setTargetZone(receive);
        }
        return res;
    }


};
