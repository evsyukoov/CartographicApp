package ru.evsyukoov.transform.utils;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static ru.evsyukoov.transform.constants.Messages.START;
import static ru.evsyukoov.transform.constants.Messages.STOP;

public class TelegramUtils {

    public static boolean isInlineMessage(Update update) {
        return update != null && update.getInlineQuery() != null;
    }

    public static boolean isTextMessage(Update update) {
        return update.getMessage() != null && update.getMessage().getText() != null;
    }

    public static boolean isDocumentMessage(Update update) {
        return update.getMessage() != null && update.getMessage().getDocument() != null;
    }

    public static boolean isCallbackMessage(Update update) {
        return update.getCallbackQuery() != null;
    }

    public static boolean isTextDocumentOrCallbackMessage(Update update) {
        return isTextMessage(update) || isDocumentMessage(update) || isCallbackMessage(update);
    }

    public static boolean isStartMessage(Update update) {
        if (isTextMessage(update)) {
            String message = update.getMessage().getText();
            return message.equals(START) || message.equals(STOP);
        }
        return false;
    }

    public static SendMessage initSendMessage(long id, List<String> messages) {
        return SendMessage.builder()
                .text(String.join("\n", messages))
                .chatId(String.valueOf(id))
                .build();
    }

    public static SendMessage initSendMessage(long id, String message) {
        return SendMessage.builder()
                .text( message)
                .chatId(String.valueOf(id))
                .build();
    }

}
