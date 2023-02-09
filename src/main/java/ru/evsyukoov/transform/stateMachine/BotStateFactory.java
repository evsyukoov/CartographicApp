package ru.evsyukoov.transform.stateMachine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.evsyukoov.transform.model.Client;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BotStateFactory {

    private final Map<State, BotState> states = new EnumMap<>(State.class);

    @Autowired
    public BotStateFactory(List<BotState> botStateList) {
        for (BotState botState : botStateList) {
            states.put(botState.getState(), botState);
        }
    }

    public BotState initState(Client client) {
        return states.get(client.getState());
    }
}
