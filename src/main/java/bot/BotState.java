package bot;

import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.File;

public enum BotState {
    //state = 0
    CHOOSE_TYPE {
        @Override
        public void writeToClient(BotContext botContext) {

        }

        @Override
        public void readFromClient(BotContext botContext) {

        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    },

    CHOOSE_SK {
        @Override
        public void writeToClient(BotContext botContext) {

        }

        @Override
        public void readFromClient(BotContext botContext) {

        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    },

    CHOOSE_ZONE {
        @Override
        public void writeToClient(BotContext botContext) {

        }

        @Override
        public void readFromClient(BotContext botContext) {

        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    };

    BotState[] statements;

    public BotState getStatement(int state)
    {
        if (statements == null)
            statements = BotState.values();
        return statements[state];
    }

    public abstract void writeToClient(BotContext botContext);

    public abstract void readFromClient(BotContext botContext);

    public abstract BotState next(BotContext botContext);

    public void receiveFile(BotContext botContext)
    {
        Document doc = botContext.getMessage().getDocument();
        File file = new File("./" + doc.getFileName());
    }

    public void sendFile(BotContext botContext, File file) {
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
}
