package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.stateMachine.State;

import java.util.List;

public interface DataService {

    Client createNewClient(long id, String name, String nickName);

    Client moveClientToStart(Client client, boolean incrementCount);

    Client findClientById(long id);

    List<String> findCoordinateSystemsByPattern(String text);

    void updateClientState(Client client, State next, State previous);

    void updateClientStateAndChosenFormat(Client client, State next, State previous, FileFormat chosenFormat);

    void updateClientStateAndChosenTransformation(Client client, State next, State previous, TransformationType type);

}
