package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.model.Client;

import java.util.List;

public interface DataService {

    Client createNewClient(long id, String name, String nickName);

    Client moveClientToStart(Client client, boolean incrementCount);

    Client findClientById(long id);

    List<String> findCoordinateSystemsByPattern(String text);

}
