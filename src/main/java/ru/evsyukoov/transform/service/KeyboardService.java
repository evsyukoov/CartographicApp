package ru.evsyukoov.transform.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

/**
 * Генератор клавиатуры/обработка нажатия кнопок
 */
public interface KeyboardService {
    /**
     *
     * @param payloadButtons - Кнопки, которые нужно будет вывести клиенту
     * @param buttonsAtRow - Количество кнопок в ряду
     * @param clientId - Telegram ID клиента
     * @return
     */
    SendMessage prepareKeyboard(List<String> payloadButtons, int buttonsAtRow, long clientId, String text);

    /**
     *
     * @param payloadButtons - Кнопки, которые нужно будет вывести клиенту
     * @param optionalButtons - Опциональные кнопки (Помощь, Назад, Подтвердить)
     * @param payloadButtonsAtRow - Количество кнопок в ряду
     * @param helpersButtonsAtRow
     * @param clientId  - Telegram ID клиента
     * @return
     */
    SendMessage prepareKeyboard(List<String> payloadButtons, List<String> optionalButtons,
                                int payloadButtonsAtRow, int helpersButtonsAtRow,
                                long clientId, String text);

    /**
     * @param update - Входящее сообщение
     * @param id - ID клиента
     * @return
     */
    EditMessageReplyMarkup pressButtonsChoiceHandle(Update update, long id);

}
