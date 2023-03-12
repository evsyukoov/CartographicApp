package ru.evsyukoov.transform.update;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.evsyukoov.transform.bot.GeodeticBot;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.constants.UpdateNotification;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.repository.ClientRepository;
import ru.evsyukoov.transform.service.KeyboardService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "update", name = "notification", havingValue = "true")
@Slf4j
public class UpdateNotificationService {

    private final ClientRepository clientRepository;

    private final KeyboardService keyboardService;

    private final GeodeticBot geodeticBot;

    @Autowired
    public UpdateNotificationService(ClientRepository clientRepository,
                                     KeyboardService keyboardService,
                                     GeodeticBot geodeticBot) {
        this.clientRepository = clientRepository;
        this.keyboardService = keyboardService;
        this.geodeticBot = geodeticBot;
    }

    @PostConstruct
    private void notificateClients() {
        int i = 1;
        for (Client client : clientRepository.findAll()) {
            // обход лимита телеграма на отправку сообщений
            if (i % 10 == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("ERROR: ", e);
                }
            }
            log.info("{} Start sending update to {}", i, client.getUserName());
            try {
                List<SendMessage> messages = new ArrayList<>();
                SendMessage updateInfo = SendMessage.builder()
                        .text(UpdateNotification.VERSION_2_0)
                        .chatId(String.valueOf(client.getId()))
                        .build();

                messages.add(updateInfo);
                messages.add(initStartMessage(client));
                for (SendMessage msg : messages) {
                    geodeticBot.execute(msg);
                }
            } catch (TelegramApiException e) {
                log.error("{} Failed sending update to {}, ex: ", i, client.getUserName(), e);
                continue;
            }
            log.info("{} Sucessfully sending update to {}", i, client.getUserName());
            i++;
        }
    }

    private SendMessage initStartMessage(Client client) {
        return keyboardService.prepareOptionalKeyboard(Collections.singletonList(Messages.HELP),
                        client.getId(),
                        Messages.INPUT_PROMPT);
    }
}
