package bot;

import exceptions.UploadFileException;
import exceptions.WrongFileFormatException;
import bot.enums.InputCoordinatesType;
import bot.enums.OutputFileType;
import bot.enums.TransType;
import convert.InfoReader;
import dao.SelectDataAccessObject;
import logging.LogUtil;
import org.json.JSONObject;
import org.osgeo.proj4j.Proj4jException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.sound.sampled.LineEvent;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public enum BotState {

    //0
    WELCOME {
        private static final String HELLO = "Отправьте файл или текст с координатами";

        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText(HELLO);
            sendMessage(botContext, sm);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
        }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
    },
    //1
    FILE {
        private BotState next;

        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage msg = new SendMessage();
            msg.setText("Отправьте файл или текст с координатами");
            setInlineKeyboard(msg, "Помощь");
            sendMessage(botContext, msg);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            client.setPrevState(FILE.ordinal());
            if ((next = helpersCasesAnalize(botContext, client, FILE)) != null)
                return;
            String text;
            if (botContext.getMessage() == null) {
                return;
            }
            if ((text = botContext.getMessage().getText()) != null) {
                InfoReader c = new InfoReader(text);
                try {
                    c.readText();
                } catch (Exception e) {
                    if (e instanceof WrongFileFormatException) {
                        client.setErrorMSG(e.getMessage());
                    } else {
                        client.setErrorMSG("Неизвестная ошибка. Обратитесь в техподдержку.");
                    }
                    next = FILE;
                    LogUtil.log(Level.SEVERE, BotState.FILE.name(), client, e);
                    ERROR.writeToClient(botContext, client);
                    return;
                }
                next = CHOOSE_OUTPUT_FILE_OPTION;
                client.setInfoReader(c);
                client.setExtension("csv");
                client.setState(next.ordinal());
                return;
            }
            //получили от клиента файл
            if (botContext.getMessage().getDocument() != null) {
                try {
                    uploadFile(botContext, client);
                } catch (Exception e) {
                    if (e instanceof UploadFileException) {
                        client.setErrorMSG(e.getMessage());
                    } else {
                        client.setErrorMSG("Неизвестная ошибка. Обратитесь в техподдержку.");
                    }
                    next = FILE;
                    client.setState(FILE.ordinal());
                    ERROR.writeToClient(botContext, client);
                    LogUtil.log(Level.SEVERE, BotState.FILE.name(), client, e);
                    return;
                }
            }
            InfoReader reader = new InfoReader(new File(client.getUploadPath()), client.getId(), client.getExtension());
            try {
                reader.run();
            } catch (Exception e) {
                if (e instanceof WrongFileFormatException) {
                    client.setErrorMSG(e.getMessage());
                } else {
                    client.setErrorMSG("Неизвестная ошибка. Обратитесь в техподдержку.");
                }
                next = FILE;
                ERROR.writeToClient(botContext, client);
                LogUtil.log(Level.SEVERE, BotState.FILE.name(), client, e);
                return;
            }
            next = CHOOSE_OUTPUT_FILE_OPTION;
            client.setInfoReader(reader);
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
            if (client.getExtension().equals("csv")
                    && client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.WGS) {
                buttons.add("GPX");
                buttons.add("KML");
                buttons.add("CSV(плоские)");
            } else if (client.getExtension().equals("csv")
                    && client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK) {
                buttons.add("GPX");
                buttons.add("KML");
                buttons.add("CSV(плоские)");
                buttons.add("CSV(WGS-84)");
            } else if (client.getExtension().equals("dxf")) {
                buttons.add("GPX");
                buttons.add("KML");
                buttons.add("CSV(WGS-84)");
                buttons.add("CSV(плоские)");
            } else if (client.getExtension().equals("kml")) {
                buttons.add("CSV(плоские)");
                buttons.add("CSV(WGS-84)");
                buttons.add("GPX");
            } else if (client.getExtension().equals("gpx")) {
                buttons.add("CSV(плоские)");
                buttons.add("CSV(WGS-84)");
                buttons.add("KML");
            }
            setInlineKeyboard(sm, buttons, "Помощь", "Назад");
            sendMessage(botContext, sm);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            client.setPrevState(CHOOSE_OUTPUT_FILE_OPTION.ordinal() - 1);
            if ((next = helpersCasesAnalize(botContext, client, CHOOSE_OUTPUT_FILE_OPTION)) != null) {
                return;
            }
            if (!isCallbackData(botContext)) {
                client.setErrorMSG("Неизвестная команда\nВыберите формат выходного файла");
                next = CHOOSE_OUTPUT_FILE_OPTION;
                ERROR.writeToClient(botContext, client);
            } else {
                String recv = botContext.getUpdate().getCallbackQuery().getData();
                if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.WGS && (recv.equals("GPX")
                        || recv.equals("KML") || recv.equals("CSV(WGS-84)"))) {
                    client.setState(EXECUTE.ordinal());
                    client.setTransType(TransType.WGS_TO_WGS);
                    next = EXECUTE;
                } else if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.WGS &&
                        recv.equals("CSV(плоские)")) {
                    next = CHOOSE_SYSTEM_COORDINATE_SRC;
                    client.setState(next.ordinal());
                    client.setTransType(TransType.WGS_TO_MSK);
                } else if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK &&
                        (recv.equals("KML") || recv.equals("GPX") || recv.equals("CSV(WGS-84)"))) {
                    next = CHOOSE_SYSTEM_COORDINATE_SRC;
                    client.setState(next.ordinal());
                    client.setTransType(TransType.MSK_TO_WGS);
                } else if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK &&
                        recv.equals("DXF") || recv.equals("CSV(плоские)")) {
                    client.setTransType(TransType.MSK_TO_MSK);
                    next = CHOOSE_SYSTEM_COORDINATE_SRC;
                    client.setState(next.ordinal());
                }
                setOutputFileType(client, recv);
            }
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },

    CHOOSE_SYSTEM_COORDINATE_SRC {
        private BotState next;

        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.WGS) {
                sm.setText("Выберите систему координат в которую хотите перевести WGS-84\n" +
                        "Для выбора введите @SurveyGeoBot ...");
            } else if (client.getInfoReader().getInputCoordinatesType() == InputCoordinatesType.MSK) {
                sm.setText("Выберите систему координат исходного файла\n" +
                        "Для выбора введите @SurveyGeoBot ...");
            }
            setInlineKeyboard(sm, "Помощь", "Назад");
            sendMessage(botContext, sm);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) throws SQLException {
            client.setPrevState(CHOOSE_SYSTEM_COORDINATE_SRC.ordinal() - 1);
            if ((next = helpersCasesAnalize(botContext, client, CHOOSE_SYSTEM_COORDINATE_SRC)) != null)
                return;
            if (botContext.getMessage() == null) {
                return;
            }
            String param = SelectDataAccessObject.
                    findCoordinateSystemParam(botContext.getMessage().getText());
            if (param == null) {
                client.setErrorMSG("Неверный выбор системы координат");
                next = CHOOSE_SYSTEM_COORDINATE_SRC;
                ERROR.writeToClient(botContext, client);
            } else {
                client.setSrcSystem(param);
                if (client.getTransType() == TransType.MSK_TO_WGS || client.getTransType() == TransType.WGS_TO_MSK) {
                    next = EXECUTE;
                    client.setState(next.ordinal());
                } else if (client.getTransType() == TransType.MSK_TO_MSK) {
                    next = CHOOSE_SYSTEM_COORDINATE_TGT;
                    client.setState(next.ordinal());
                }
            }
        }


        @Override
        public BotState next(BotContext botContext) {
            return next;
        }
    },

    CHOOSE_SYSTEM_COORDINATE_TGT {

        private BotState next;

        @Override
        public void writeToClient(BotContext botContext, Client client) {
            SendMessage sm = new SendMessage();
            sm.setText("Выберите систему координат результирующего файла\n" +
                    "Для выбора введите @SurveyGeoBot ...");
            setInlineKeyboard(sm, "Помощь", "Назад");
            sendMessage(botContext, sm);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) throws SQLException {
            client.setPrevState(CHOOSE_SYSTEM_COORDINATE_TGT.ordinal() - 1);
            if ((next = helpersCasesAnalize(botContext, client, CHOOSE_SYSTEM_COORDINATE_TGT)) != null)
                return;
            if (botContext.getMessage() == null) {
                return;
            }
            String param = SelectDataAccessObject.
                    findCoordinateSystemParam(botContext.getMessage().getText());
            if (param == null) {
                client.setErrorMSG("Неверный выбор системы координат");
                next = CHOOSE_SYSTEM_COORDINATE_TGT;
                ERROR.writeToClient(botContext, client);
            } else {
                client.setTgtSystem(param);
                next = EXECUTE;
                client.setState(next.ordinal());
            }
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
            GeneratorManager gm = new GeneratorManager(client);
            try {
                gm.run();
                sendFile(botContext, gm.getOutput());
                client.setClientReady(true);
                SendMessage sm = new SendMessage();
                sm.setText("Отправьте файл или текст с координатами");
                setInlineKeyboard(sm, "Помощь");
                sendMessage(botContext, sm);
                next = FILE;
                client.setState(next.ordinal());
            } catch (Exception e) {
                if (e instanceof Proj4jException || e instanceof IllegalStateException) {
                    client.setErrorMSG("Ошибка трансформации");
                    TRANSFORM_ERROR.writeToClient(botContext, client);
                } else {
                    client.setErrorMSG("Неизвестная ошибка. Обратитесь в техподдержку.");
                    ERROR.writeToClient(botContext, client);
                }
                next = FILE;
                client.setState(next.ordinal());
                LogUtil.log(Level.SEVERE, BotState.EXECUTE.name(), client, e);
            }
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
        }

        @Override
        public BotState next(BotContext botContext) {
            return next;
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
            setInlineKeyboard(sm, "Помощь");
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
            setInlineKeyboard(sm, "Назад");
            sendMessage(botContext, sm);
        }

        @Override
        public void readFromClient(BotContext botContext, Client client) {
            if ((next = helpersCasesAnalize(botContext, client,
                    BotState.getStatement(client.getState()))) != null)
                return;

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

    public abstract void readFromClient(BotContext botContext, Client client) throws SQLException;

    public abstract BotState next(BotContext botContext);

    public Boolean checkStop(BotContext botContext, Client client) {
        if (botContext.getMessage() == null)
            return false;
        String msg = botContext.getMessage().getText();
        if (msg != null && (msg.equals("/stop") || botContext.getMessage().getText().equals("/start"))) {
            client.setState(FILE.ordinal());
            return true;
        }
        return false;
    }

    public void sendFile(BotContext botContext, File file) {
        SendDocument doc = new SendDocument();
        doc.setDocument(file);
        doc.setChatId(botContext.getChat().getId());
        try {
            botContext.getBot().execute(doc);
        } catch (TelegramApiException e) {
            LogUtil.log(Level.SEVERE, "SendFile", e);
        }
    }

    public void sendMessage(BotContext botContext, SendMessage sm) {
        sm.setChatId(botContext.getChat().getId());
        try {
            botContext.getBot().execute(sm);
        } catch (TelegramApiException e) {
            LogUtil.log(Level.SEVERE, "SendMessage", e);
        }
    }

    private void findExtension(String path, Client client) {
        int pos = path.indexOf('.');
        if (pos == -1)
            client.setExtension("csv");
        else {
            client.setExtension(path.substring(pos + 1).toLowerCase());
        }
    }

    public void uploadFile(BotContext botContext, Client client) throws Exception {
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
                uploadZIP(downoload, client);
            Writer fw = new OutputStreamWriter(new FileOutputStream(client.getUploadPath()), StandardCharsets.UTF_8);
            String charset;
            if (client.getExtension().equalsIgnoreCase("csv"))
                charset = "windows-1251";
            else
                charset = "UTF-8";
            BufferedReader uploadIn = new BufferedReader(new InputStreamReader(downoload.openStream(), charset));
            String s;
            while ((s = uploadIn.readLine()) != null)
                fw.write(String.format("%s\n", s));
            fw.close();
            uploadIn.close();
            in.close();
            System.out.println("Uploaded!");
        } catch (IOException e) {
            throw new UploadFileException("Проблема с загрузкой файла с сервера Телеграм", e);
        }
    }

    public void uploadZIP(URL download, Client client) throws UploadFileException {
        try (
                ReadableByteChannel rbc = Channels.newChannel(download.openStream());
                FileOutputStream fos = new FileOutputStream(client.getUploadPath())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new UploadFileException("Проблема с загрузкой kmz-архива с серверa Telegram", e);
        }
    }

    public synchronized void setInlineKeyboard(SendMessage sm, ArrayList<String> buttons, String... helpers) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = null;
        for (int i = 0; i < buttons.size(); i++) {
            if (i % 2 == 0) {
                row = new ArrayList<>();
                rows.add(row);
            }
            row.add(new InlineKeyboardButton().setText(buttons.get(i))
                    .setCallbackData(buttons.get(i)));
        }
        setHelpers(rows, helpers);
        inlineKeyboard.setKeyboard(rows);
        sm.setReplyMarkup(inlineKeyboard);
    }

    public synchronized void setInlineKeyboard(SendMessage sm, String... helpers) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        setHelpers(rows, helpers);
        inlineKeyboard.setKeyboard(rows);
        sm.setReplyMarkup(inlineKeyboard);
    }

    private synchronized void setHelpers(List<List<InlineKeyboardButton>> rows, String... helpers) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (String s : helpers) {
            row.add(new InlineKeyboardButton().setText(s).setCallbackData(s));
        }
        rows.add(row);
    }

    public BotState helpersCasesAnalize(BotContext botContext, Client client, BotState current) {
        if (checkStop(botContext, client)) {
            return FILE;
        }
        if (botContext.getUpdate().getCallbackQuery() == null) {
            return null;
        }

        String recv = botContext.getUpdate().getCallbackQuery().getData();
        if (recv.equals("Помощь")) {
            client.setPrevState(current.ordinal());
            client.setState(HELP.ordinal());
            return HELP;
        } else if (recv.equals("Назад")) {
            client.setState(client.getPrevState());
            return BotState.getStatement(client.getPrevState());
        }
        return null;
    }

    public boolean isCallbackData(BotContext botContext) {
        return botContext.getUpdate().getCallbackQuery() != null;
    }

    public void setOutputFileType(Client client, String recv) {
        if (recv.equals("DXF")) {
            client.setOutputFileType(OutputFileType.DXF);
        } else if (recv.equals("KML")) {
            client.setOutputFileType(OutputFileType.KML);
        } else if (recv.equals("CSV") || recv.equals("CSV(плоские)") || recv.equals("CSV(WGS-84)")) {
            client.setOutputFileType(OutputFileType.CSV);
        } else if (recv.equals("GPX")) {
            client.setOutputFileType(OutputFileType.GPX);
        }
    }


};
