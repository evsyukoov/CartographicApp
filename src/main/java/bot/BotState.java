package bot;

import dao.ClientDAO;
import dao.SelectDAO;
import org.json.JSONObject;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
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
        private static final String HELLO = "Что умеет этот бот?\nОтправьте ему csv в формате: \n" +
                "Имя точки, X, Y, Z(опционально)\n" +
                "Выберите систему координат и зону из предложенных\n" +
                "В ответ получите kml файл\n" +
                "Пример форматирования: Rp12, 123111.23, 456343.79";

        @Override
        public int writeToClient(BotContext botContext) {
            if (sendMessage(botContext, HELLO) == 0)
                return (0);
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext) {
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            return FILE;
        }
    },
    //1
    FILE {
        @Override
        public int writeToClient(BotContext botContext) {
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext) {
            if (uploadFile(botContext) == 0)
                return (0);
            try {
                new ClientDAO(botContext.getMessage().getChat().getId()).setState(2);
            } catch (SQLException e) {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }

        @Override
        public BotState next(BotContext botContext) {
            return CHOOSE_TYPE;
        }
    },
    //2
    CHOOSE_TYPE {
        private Boolean isRightAnswer;
        private ArrayList<String> availableTypes;

        @Override
        public int writeToClient(BotContext botContext) {
            try {
                SelectDAO sd = new SelectDAO();
                sd.selectTypes();
                availableTypes = sd.getTypes();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите тип СК");
                setButtons(sm, availableTypes);
                if (sendMessage(botContext, sm) == 0)
                    return (0);
                ClientDAO clientBD = new ClientDAO(botContext.getMessage().getChat().getId());
                clientBD.setState(2);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext) {
            try {
                String recieve = botContext.getMessage().getText();
                if (!availableTypes.contains(recieve))
                    isRightAnswer = false;
                else
                {
                    isRightAnswer = true;
                    ClientDAO clientBD = new ClientDAO(botContext.getMessage().getChat().getId());
                    clientBD.setState(3);
                    clientBD.setType(recieve);
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
            if (isRightAnswer)
                return CHOOSE_SK;
            return ERROR_INPUT;
        }
    },
    //3
    CHOOSE_SK {
        private Boolean isRightAnswer;
        private ArrayList<String> availableSK;

        @Override
        public int writeToClient(BotContext botContext) {
            try {
                SelectDAO sd = new SelectDAO();
                ClientDAO clientBD = new ClientDAO(botContext.getMessage().getChat().getId());
                clientBD.getData();
                sd.selectSK(clientBD.getChoosedType());
                availableSK = sd.getSk();
                SendMessage sm = new SendMessage();
                sm.setText("Выберите регион(район)");
                setButtons(sm, availableSK);
                if (sendMessage(botContext, sm) == 0)
                    return (0);
                clientBD.setState(3);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return (0);
            }
            return (1);
        }

        @Override
        public int readFromClient(BotContext botContext) {
            ClientDAO clientBD = new ClientDAO(botContext.getMessage().getChat().getId());
            try {
                String recieve = botContext.getMessage().getText();
                if (!availableSK.contains(recieve))
                    isRightAnswer = false;
                else
                {
                    isRightAnswer = true;
                    clientBD.setSK(recieve);
                    clientBD.setState(4);
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
        @Override
        public int writeToClient(BotContext botContext) {
            try {
                SelectDAO sd = new SelectDAO();
                ClientDAO clientBD = new ClientDAO(botContext.getMessage().getChat().getId());
                clientBD.getData();
                sd.selectZone(clientBD.getChoosedSK());
                availableZones = sd.getZones();
                for (String s : availableZones) {
                    System.out.println(s);
                }

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
        public int readFromClient(BotContext botContext) {
            SelectDAO sd = new SelectDAO();
            ClientDAO clientBD = new ClientDAO(botContext.getMessage().getChat().getId());
            try {
                String recieve = botContext.getMessage().getText();
                if (!availableZones.contains(recieve))
                    isRightAnswer = false;
                else
                {
                    isRightAnswer = true;
                    //собственно обработка отправленного файла на основе присланной информации

                    clientBD.setState(1);
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
            if (isRightAnswer)
                return FILE;
            return ERROR_INPUT;
        }
    },
    //5
    ERROR_INPUT
            {
                @Override
                public int writeToClient(BotContext botContext) {
                    SendMessage sm = new SendMessage();
                    sm.setText("Неверный выбор");
                    sendMessage(botContext, sm);
                    return (1);
                }

                @Override
                public int readFromClient(BotContext botContext) {
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

    public abstract int writeToClient(BotContext botContext);

    public abstract int readFromClient(BotContext botContext);

    public abstract BotState next(BotContext botContext);


    private void sendFile(BotContext botContext, File file) {
        SendMessage sendMessage = new SendMessage();
        SendDocument sendDocument = new SendDocument();
        sendDocument.setNewDocument(file);
        sendMessage.setChatId(botContext.getMessage().getChat().getId());
        try {
            botContext.getBot().execute(sendMessage);
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

    public int uploadFile(BotContext botContext) {
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
            FileOutputStream fos = new FileOutputStream(uploadFile);
            System.out.println("Start upload");
            ReadableByteChannel rbc = Channels.newChannel(downoload.openStream());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
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
