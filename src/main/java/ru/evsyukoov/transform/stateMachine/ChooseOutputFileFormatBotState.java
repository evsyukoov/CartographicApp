package ru.evsyukoov.transform.stateMachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ChooseOutputFileFormatBotState implements BotState {

    private final KeyboardService keyboardService;

    @Autowired
    public ChooseOutputFileFormatBotState(KeyboardService keyboardService) {
        this.keyboardService = keyboardService;
    }

    @Override
    public String getStateMessage() {
        return Messages.FILE_FORMAT_CHOICE;
    }

    @Override
    public State getState() {
        return State.CHOOSE_OUTPUT_FILE_OPTION;
    }

    @Override
    public List<BotApiMethod<?>> handleMessage(Client client, Update update) {
        log.info("ChooseOutputFile option state");
        if (!TelegramUtils.isCallbackMessage(update)) {
            return Collections.singletonList(
                    TelegramUtils.initSendMessage(client.getId(), Messages.FILE_FORMAT_CHOICE));
        } else {
            return Collections.singletonList(
                    keyboardService.pressButtonsChoiceHandle(update, client.getId()));
        }
    }
}
