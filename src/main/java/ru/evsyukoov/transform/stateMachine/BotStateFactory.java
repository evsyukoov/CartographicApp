package ru.evsyukoov.transform.stateMachine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.model.StateHistory;

import java.util.Comparator;
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
        List<StateHistory> history = client.getStateHistory();
        if (CollectionUtils.isEmpty(history)) {
            return states.get(State.INPUT);
        }
        client.getStateHistory().sort(Comparator.comparing(StateHistory::getState));
        return states.get(history.get(history.size() - 1).getState());
    }
}
