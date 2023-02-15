package ru.evsyukoov.transform.stateMachine;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.util.List;

import static ru.evsyukoov.transform.constants.Messages.INPUT_PROMPT;

//TODO библиотека для state-machine, вынести интерфейс и factory
public interface BotState {

    /**
     * @return - Сообщение, отправляемое в самом начале взаимодействия бота с клиентом
     */
    default String getStartMessage() {
        return INPUT_PROMPT;
    }

    /**
     * @return - Сообщение, отправляемое при взаимодействии бота с клиентом на конкретном шаге
     */
    String getStateMessage();

    /**
     * @return - Текущий шаг взаимодействия
     */
    State getState();

    /**
     *
     * @param client - Клиент, c которым общается бот
     * @param update - Сообщение клиента
     * @return - Контент, который будет отправлен клиенту
     */
    List<BotApiMethod<?>> handleMessage(Client client, Update update);

    default List<BotApiMethod<?>> handleStartMessage(Client client, Update update) {
        if (TelegramUtils.isStartMessage(update)) {
            return List.of(SendMessage.builder()
                    .chatId(String.valueOf(client.getId()))
                    .text(getStartMessage())
                    .build());
        }
        return null;
    }

}