package ru.evsyukoov.transform.utils;

import org.telegram.telegrambots.meta.api.objects.Update;

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
}
