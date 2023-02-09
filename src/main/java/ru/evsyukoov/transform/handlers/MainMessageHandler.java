package ru.evsyukoov.transform.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.bot.BotState;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.repository.ClientRepository;
import ru.evsyukoov.transform.utils.TelegramUtils;

@Service
@Slf4j
public class MainMessageHandler {

    private final ClientRepository clientRepository;

    @Autowired
    public MainMessageHandler(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public PartialBotApiMethod<?> prepareMessage(Update update) {
        long clientId;
        if (TelegramUtils.isCallbackMessage(update)) {
            clientId = update.getCallbackQuery().getMessage().getChatId();
        } else {
            clientId = update.getMessage().getChatId();
        }
        Client client = clientRepository.findClientById(clientId);
        if (client == null) {
            //Clien
        }
        return null;
    }

    private Client createNewClient(long id) {
        Client client = new Client();
        client.setId(id);
        client.setState(BotState.WELCOME);
        client.setCount(0);
        //client.set
        return null;
    }
}
