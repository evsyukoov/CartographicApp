package ru.evsyukoov.transform.stateMachine;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.model.Client;

import java.util.List;

@Component
public class InputBotState implements BotState {

    @Override
    public String getStateMessage() {
        return null;
    }

    @Override
    public State getState() {
        return State.INPUT;
    }

    @Override
    public List<BotApiMethod<?>> handleMessage(Client client, Update update) {
        return null;
    }
}
