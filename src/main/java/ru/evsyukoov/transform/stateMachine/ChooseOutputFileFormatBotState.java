package ru.evsyukoov.transform.stateMachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ChooseOutputFileFormatBotState implements BotState {

    private final KeyboardService keyboardService;

    private final DataService dataService;

    private BotStateFactory stateFactory;

    @Autowired
    public ChooseOutputFileFormatBotState(KeyboardService keyboardService,
                                          DataService dataService) {
        this.keyboardService = keyboardService;
        this.dataService = dataService;
    }

    @Autowired
    public void setStateFactory(@Lazy BotStateFactory stateFactory) {
        this.stateFactory = stateFactory;
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
        log.info("{} state, client {}", getState().name(), client);
        if (!TelegramUtils.isCallbackMessage(update)) {
            return Collections.singletonList(
                    TelegramUtils.initSendMessage(client.getId(), Messages.FILE_FORMAT_CHOICE));
        } else {
            String text = update.getCallbackQuery().getData();
            if (text.equals(Messages.BACK)) {
                List<BotApiMethod<?>> res = Collections.singletonList(TelegramUtils.initSendMessage(client.getId(),
                        stateFactory.initPrevState(client).getStateMessage()));
                dataService.updateClientState(client, State.CHOOSE_TRANSFORMATION_TYPE, State.INPUT);
                return res;
            } else if (text.equals(Messages.APPROVE)) {
                List<String> pressedButtons = keyboardService.getPressedItems(update, client.getId());

            }
            return Collections.singletonList(
                    keyboardService.pressButtonsChoiceHandle(update, client.getId()));
        }
    }
}
