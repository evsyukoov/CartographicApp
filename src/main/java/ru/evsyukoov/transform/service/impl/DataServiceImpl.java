package ru.evsyukoov.transform.service.impl;

import com.ibm.icu.text.Transliterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.model.CoordinateSystem;
import ru.evsyukoov.transform.repository.ClientRepository;
import ru.evsyukoov.transform.repository.CoordinateSystemRepository;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.stateMachine.State;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataServiceImpl implements DataService {

    private final ClientRepository clientRepository;

    private final CoordinateSystemRepository coordinateSystemRepository;

    private final Transliterator latinToCyrillic;

    private final Transliterator cyrillicToLatin;

    @Value("${bot.max-result}")
    private Integer maxResult;

    @Autowired
    public DataServiceImpl(ClientRepository clientRepository,
                           CoordinateSystemRepository coordinateSystemRepository,
                           Transliterator latinToCyrillic,
                           Transliterator cyrillicToLatin) {
        this.clientRepository = clientRepository;
        this.coordinateSystemRepository = coordinateSystemRepository;
        this.latinToCyrillic = latinToCyrillic;
        this.cyrillicToLatin = cyrillicToLatin;
    }

    @Override
    public Client createNewClient(long id, String name, String nickName) {
        Client client = new Client();
        client.setId(id);
        client.setUserName(name);
        client.setNickName(nickName);
        client.setState(State.INPUT);
        client.setCount(0);
        log.info("Successfully create new client {}", client);
        return client;
    }

    @Override
    public Client moveClientToStart(Client client, boolean incrementCount) {
        client.setPreviousState(null);
        client.setState(State.values()[0]);
        client.setCount(incrementCount ? client.getCount() + 1 : client.getCount());
        log.info("Successfully move client {} to start", client);
        clientRepository.save(client);
        return client;
    }

    @Override
    public Client findClientById(long id) {
        return clientRepository.findClientById(id);
    }

    public List<String> findSystemDescription(String text) {
        return coordinateSystemRepository
                .findCoordinateSystemByDescriptionLikeIgnoreCase("%" + text + "%")
                .stream()
                .limit(maxResult)
                .map(CoordinateSystem::getDescription)
                .collect(Collectors.toList());
    }

    @Override
    public void updateClientState(Client client, State next, State previous) {
        client.setState(next);
        client.setPreviousState(previous);
        clientRepository.save(client);
    }

    @Override
    public List<String> findCoordinateSystemsByPattern(String text) {
        List<String> projects = findSystemDescription(text);
        if (CollectionUtils.isEmpty(projects)) {
            projects = findSystemDescription(transliterateString(text));
        }
        return projects;
    }

    private String transliterateString(String text) {
        if (isCyrillic(text)) {
            return cyrillicToLatin.transliterate(text);
        } else {
            return latinToCyrillic.transliterate(text);
        }
    }

    private boolean isCyrillic(String text) {
        return text.chars()
                .mapToObj(Character.UnicodeBlock::of)
                .anyMatch(Character.UnicodeBlock.CYRILLIC::equals);
    }

}
