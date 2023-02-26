package ru.evsyukoov.transform.service.impl;

import com.ibm.icu.text.Transliterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.model.CoordinateSystem;
import ru.evsyukoov.transform.model.StateHistory;
import ru.evsyukoov.transform.repository.ClientRepository;
import ru.evsyukoov.transform.repository.CoordinateSystemRepository;
import ru.evsyukoov.transform.repository.StateHistoryRepository;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.stateMachine.State;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataServiceImpl implements DataService {

    private final ClientRepository clientRepository;

    private final CoordinateSystemRepository coordinateSystemRepository;

    private final StateHistoryRepository stateHistoryRepository;

    private final Transliterator latinToCyrillic;

    private final Transliterator cyrillicToLatin;

    @Value("${bot.max-result}")
    private Integer maxResult;

    @Autowired
    public DataServiceImpl(ClientRepository clientRepository,
                           CoordinateSystemRepository coordinateSystemRepository,
                           StateHistoryRepository stateHistoryRepository,
                           Transliterator latinToCyrillic,
                           Transliterator cyrillicToLatin) {
        this.clientRepository = clientRepository;
        this.coordinateSystemRepository = coordinateSystemRepository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.latinToCyrillic = latinToCyrillic;
        this.cyrillicToLatin = cyrillicToLatin;
    }

    @Override
    public Client createNewClient(long id, String name, String nickName) {
        Client client = new Client();
        client.setId(id);
        client.setUserName(name);
        client.setNickName(nickName);

        setStartStateHistory(client);
        client.setCount(0);
        log.info("Successfully create new client {}", client);
        return client;
    }

    private void setStartStateHistory(Client client) {
        StateHistory history = new StateHistory();
        history.setState(State.INPUT);
        client.setStateHistory(List.of(history));
        history.setClient(client);
    }

    @Override
    public Client moveClientToStart(Client client, boolean incrementCount, String stateResponse) {
        //rm old states chain
        setStartStateHistory(client, stateResponse);
        client.setCount(incrementCount ? client.getCount() + 1 : client.getCount());
        log.info("Successfully move client {} to start", client);
        clientRepository.save(client);
        return client;
    }

    private void setStartStateHistory(Client client, String response) {
        StateHistory history = new StateHistory();
        history.setState(State.INPUT);
        history.setResponse(response);
        client.setStateHistory(List.of(history));
        history.setClient(client);
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
    public void updateClientState(Client client, State next, String response, String clientChoice) {
        List<StateHistory> history = client.getStateHistory();
        StateHistory lastState = getLastState(client);
        lastState.setClientChoice(clientChoice);

        StateHistory state = new StateHistory();
        state.setClient(client);
        state.setState(next);
        state.setResponse(response);

        history.add(state);
        client.setStateHistory(history);
        clientRepository.save(client);
    }

    private StateHistory getLastState(Client client) {
        client.getStateHistory().sort(Comparator.comparing(StateHistory::getState));
        int lastIndex = client.getStateHistory().size() - 1;
        return client.getStateHistory().get(lastIndex);
    }

//    public void updateClientStateAndChosenFormat(Client client, State next, State previous, FileFormat chosenFormat) {
//        client.setState(next);
//        client.setPreviousState(previous);
//        client.setFormat(chosenFormat);
//        clientRepository.save(client);
//    }
//
//    @Override
//    public void updateClientStateAndChosenTransformation(Client client, State next, State previous, TransformationType type) {
//        client.setState(next);
//        client.setPreviousState(previous);
//        client.setTransformationType(type);
//        clientRepository.save(client);
//    }

    @Override
    public void removeLastClientStateInfo(Client client) {
        List<StateHistory> history = client.getStateHistory();
        int index = history.size() - 1;
        history.remove(index);
        client.setStateHistory(history);
        clientRepository.save(client);
    }

    @Override
    public StateHistory removeLastStateAndGet(Client client) {
        client.getStateHistory().sort(Comparator.comparing(StateHistory::getState));
        int lastIndex = client.getStateHistory().size() - 1;
        client.getStateHistory().remove(lastIndex);
        clientRepository.save(client);
        return client.getStateHistory().get(client.getStateHistory().size() - 1);
    }

    @Override
    public List<String> findCoordinateSystemsByPattern(String text) {
        List<String> projects = findSystemDescription(text);
        if (CollectionUtils.isEmpty(projects)) {
            projects = findSystemDescription(transliterateString(text));
        }
        return projects;
    }

    @Override
    public FileFormat getClientFileFormatChoice(Client client) {
        StateHistory stateWithFormatChoice = client.getStateHistory()
                .stream()
                .filter(state -> state.getState() == State.INPUT)
                .findFirst()
                .orElse(null);
        if (stateWithFormatChoice == null) {
            throw new RuntimeException();
        }
        return FileFormat.valueOf(stateWithFormatChoice.getClientChoice());
    }

    @Override
    public List<FileFormat> getOutputFileFormatChoice(Client client) {
        StateHistory st = client.getStateHistory()
                .stream()
                .filter(state -> state.getState() == State.CHOOSE_OUTPUT_FILE_OPTION)
                .findFirst()
                .orElse(null);
        if (st == null || st.getClientChoice() == null) {
            throw new RuntimeException();
        }
        return Arrays.stream(st.getClientChoice()
                .split(Messages.DELIMETR)).map(FileFormat::valueOf).collect(Collectors.toList());
    }

    @Override
    public TransformationType getClientTransformationTypeChoice(Client client) {
        StateHistory stateWithTransformationChoice = client.getStateHistory()
                .stream()
                .filter(state -> state.getState() == State.CHOOSE_TRANSFORMATION_TYPE)
                .findFirst()
                .orElse(null);
        if (stateWithTransformationChoice == null) {
            throw new RuntimeException();
        }
        return TransformationType.valueOf(stateWithTransformationChoice.getClientChoice());
    }

    @Override
    public String getSrcCoordinateSystemChoice(Client client) {
        StateHistory st = client.getStateHistory()
                .stream()
                .filter(state -> state.getState() == State.CHOOSE_SYSTEM_COORDINATE_SRC)
                .findFirst()
                .orElse(null);
        if (st == null) {
            throw new RuntimeException();
        }
        return st.getClientChoice();
    }

    @Override
    public String getCoordinateSystemParams(String coordinateSystemDescription) {
        return coordinateSystemRepository.findFirstByDescription(coordinateSystemDescription).getParams();
    }

    @Override
    public String getTgtCoordinateSystemChoice(Client client) {
        StateHistory st = client.getStateHistory()
                .stream()
                .filter(state -> state.getState() == State.CHOOSE_SYSTEM_COORDINATE_TGT)
                .findFirst()
                .orElse(null);
        if (st == null) {
            throw new RuntimeException();
        }
        return st.getClientChoice();
    }

    @Override
    public List<String> getCoordinateSystemsDescription() {
        return coordinateSystemRepository.findAll().stream()
                .map(CoordinateSystem::getDescription)
                .collect(Collectors.toList());
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
