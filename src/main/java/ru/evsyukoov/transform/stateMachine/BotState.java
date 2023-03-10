package ru.evsyukoov.transform.stateMachine;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.model.Client;

import java.util.List;

//TODO библиотека для state-machine, вынести интерфейс и factory
public interface BotState {

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
    List<PartialBotApiMethod<?>> handleMessage(Client client, Update update) throws Exception;

}
