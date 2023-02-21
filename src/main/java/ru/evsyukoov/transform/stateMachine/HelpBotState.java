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

import java.util.List;

@Component
@Slf4j
public class HelpBotState implements BotState {

    private final KeyboardService keyboardService;

    private final DataService dataService;

    private BotStateFactory factory;

    @Autowired
    public HelpBotState(DataService dataService,
                        KeyboardService keyboardService) {
        this.keyboardService  = keyboardService;
        this.dataService = dataService;
    }

    @Autowired
    public void setFactory(@Lazy BotStateFactory factory) {
        this.factory = factory;
    }

    @Override
    public String getStateMessage() {
        return Messages.HELP_PROMPT;
    }

    @Override
    public State getState() {
        return State.HELP;
    }

    @Override
    public List<BotApiMethod<?>> handleMessage(Client client, Update update) {
        if (TelegramUtils.isCallbackMessage(update)) {
            if (update.getCallbackQuery().getData().equals(Messages.BACK))  {
              BotState prev = factory.initPrevState(client);
              dataService.updateClientState(client, State.INPUT, null);
              return List.of(
                      prev.getStartMessage(client.getId()));
            }
        }
        return null;
    }
}
