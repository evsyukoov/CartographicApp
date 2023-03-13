package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.model.CoordinateSystem;
import ru.evsyukoov.transform.model.StateHistory;
import ru.evsyukoov.transform.stateMachine.State;

import java.util.List;

public interface DataService {

    Client createNewClient(long id, String name, String nickName, String response);

    Client moveClientToStart(Client client, boolean incrementCount, String stateResponse);

    Client findClientById(long id);

    List<String> findCoordinateSystemsByPattern(String text);

    void updateClientState(Client client, State next, String response, String clientChoice);

    void removeLastClientStateInfo(Client client);

    StateHistory removeLastStateAndGet(Client client);

    FileFormat getClientFileFormatChoice(Client client);

    List<FileFormat> getOutputFileFormatChoice(Client client);

    TransformationType getClientTransformationTypeChoice(Client client);

    String getSrcCoordinateSystemChoice(Client client);

    String getCoordinateSystemParams(String coordinateSystemDescription);

    String getTgtCoordinateSystemChoice(Client client);

    List<String> getCoordinateSystemsDescription();

}
