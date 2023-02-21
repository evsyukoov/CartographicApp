package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.service.KeyboardService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KeyboardServiceImpl implements KeyboardService {

    @Value("${bot.buttons-at-row:2}")
    private Integer buttonsAtRow;

    @Value("${bot.optional-buttons-at-row:3}")
    private Integer optionalButtonsAtRow;

    @Override
    public SendMessage prepareKeyboard(List<String> payloadButtons, long clientId, String text) {
        SendMessage sm = new SendMessage();
        payloadButtons = payloadButtons.stream()
                .map(Messages.EMPTY_SYMBOL::concat)
                .collect(Collectors.toList());
        sm.setReplyMarkup(prepareKeyboard(payloadButtons, buttonsAtRow));
        sm.setChatId(String.valueOf(clientId));
        sm.setText(text);
        return sm;
    }

    @Override
    public SendMessage prepareOptionalKeyboard(List<String> optionalButtons, long clientId, String text) {
        SendMessage sm = new SendMessage();
        sm.setReplyMarkup(prepareKeyboard(optionalButtons, buttonsAtRow));
        sm.setChatId(String.valueOf(clientId));
        sm.setText(text);
        return sm;
    }

    @Override
    public SendMessage prepareKeyboard(List<String> payloadButtons, List<String> optionalButtons,
                                       long clientId, String text) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        SendMessage sendMessage = new SendMessage();
        payloadButtons = payloadButtons.stream()
                .map(Messages.EMPTY_SYMBOL::concat)
                .collect(Collectors.toList());
        List<List<InlineKeyboardButton>> buttons = prepareButtons(payloadButtons, buttonsAtRow);
        buttons.addAll(prepareButtons(optionalButtons, optionalButtonsAtRow));
        inlineKeyboardMarkup.setKeyboard(buttons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setChatId(String.valueOf(clientId));
        sendMessage.setText(text);
        return sendMessage;
    }

    @Override
    public EditMessageReplyMarkup pressButtonsChoiceHandle(Update update, long id) {
        InlineKeyboardMarkup markup = update.getCallbackQuery().getMessage().getReplyMarkup();
        String message = update.getCallbackQuery().getData();
        InlineKeyboardButton pressed = findPressedButton(markup, message);
        if (pressed.getCallbackData().startsWith(Messages.CONFIRM_SYMBOL)) {
            pressed.setText(pressed.getText().replace(Messages.CONFIRM_SYMBOL, Messages.EMPTY_SYMBOL));
            pressed.setCallbackData(pressed.getCallbackData().replace(Messages.CONFIRM_SYMBOL, Messages.EMPTY_SYMBOL));
        } else {
            pressed.setText(pressed.getText().replace(Messages.EMPTY_SYMBOL, Messages.CONFIRM_SYMBOL));
            pressed.setCallbackData(pressed.getCallbackData().replace(Messages.EMPTY_SYMBOL, Messages.CONFIRM_SYMBOL));
        }
        return EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(id))
                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                .replyMarkup(markup)
                .build();
    }

    @Override
    public List<String> getPressedItems(Update update, long id) {
        InlineKeyboardMarkup markup = update.getCallbackQuery().getMessage().getReplyMarkup();
        return markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .filter(str -> str.startsWith(Messages.CONFIRM_SYMBOL))
                .map(str -> str.substring(Messages.CONFIRM_SYMBOL.length()))
                .collect(Collectors.toList());
    }

    private InlineKeyboardButton findPressedButton(InlineKeyboardMarkup markup, String text) {
        return markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .filter(button -> button.getCallbackData().equals(text))
                .findAny()
                .orElse(null);
    }

    private InlineKeyboardButton newButton(String text) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(text)
                .build();
    }

    private InlineKeyboardMarkup prepareKeyboard(List<String> payloadButtons, int buttonsAtRow) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(prepareButtons(payloadButtons, buttonsAtRow));
        return markup;
    }

    private List<List<InlineKeyboardButton>> prepareButtons(List<String> payloadButtons, int buttonsAtRow) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        int i = 0;
        List<InlineKeyboardButton> row = new ArrayList<>();
        buttons.add(row);
        for (String payload : payloadButtons) {
            if (i != 0 && i % buttonsAtRow == 0) {
                row = new ArrayList<>();
                buttons.add(row);
            }
            row.add(newButton(payload));
            i++;
        }
        return buttons;
    }


}
