package ru.evsyukoov.transform.handlers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.osgeo.proj4j.Proj4jException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.exceptions.UploadFileException;
import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.model.StateHistory;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.stateMachine.BotState;
import ru.evsyukoov.transform.stateMachine.BotStateFactory;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class MessageHandler {

    private final DataService dataService;

    private final BotStateFactory stateFactory;

    private final ObjectMapper objectMapper;

    private final KeyboardService keyboardService;

    @Autowired
    public MessageHandler(DataService dataService,
                          BotStateFactory stateFactory,
                          ObjectMapper objectMapper,
                          KeyboardService keyboardService) {
        this.stateFactory = stateFactory;
        this.dataService = dataService;
        this.objectMapper = objectMapper;
        this.keyboardService = keyboardService;
    }

    public List<PartialBotApiMethod<?>> prepareMessage(Update update) throws IOException {
        long clientId;
        if (TelegramUtils.isCallbackMessage(update)) {
            clientId = update.getCallbackQuery().getMessage().getChatId();
        } else {
            clientId = update.getMessage().getChatId();
        }
        Client client = dataService.findClientById(clientId);
        if (client == null) {
            client = dataService.createNewClient(clientId, getName(update), getNickName(update));
        }
        List<PartialBotApiMethod<?>> commonResponse = handleStartMessage(client, update);
        if (commonResponse != null) {
            dataService.moveClientToStart(client, false,
                    objectMapper.writeValueAsString(commonResponse));
            return commonResponse;
        }
        commonResponse = handleHelpMessage(client, update);
        if (commonResponse != null) {
            return commonResponse;
        }
        commonResponse = handleBackMessage(client, update);
        if (commonResponse != null) {
            return commonResponse;
        }
        try {
            BotState currentState = stateFactory.initState(client);
            commonResponse = currentState.handleMessage(client, update);
        } catch (Exception e) {
            dataService.moveClientToStart(client, false,
                    objectMapper.writeValueAsString(initStartMessage(client)));
            if (e instanceof WrongFileFormatException) {
                log.error("Error with message format: ", e);
                return initStartMessage(client, e.getMessage());
            } else if (e instanceof UploadFileException) {
                log.error("Error while upload file: ", e);
                return initStartMessage(client, e.getMessage());
            } else if (e instanceof Proj4jException || e instanceof IllegalStateException) {
                String err = "Не удалось трансформировать точки";
                log.error("Error: {}, ex: ", err, e);
                return initStartMessage(client, err);
            } else {
                log.error("FATAL ERROR: ", e);
                return initStartMessage(client, Messages.FATAL_ERROR);
            }
        }
        return commonResponse;
    }

    private List<PartialBotApiMethod<?>> handleBackMessage(Client client, Update update) throws IOException {
        if (TelegramUtils.isBackMessage(update)) {
            List<StateHistory> history = client.getStateHistory();
            if (!CollectionUtils.isEmpty(history)) {
                StateHistory lastState = dataService.removeLastStateAndGet(client);
                List<Serializable> lastResp = objectMapper.readValue(lastState.getResponse(),
                        new TypeReference<List<Serializable>>() {
                        });
                List<PartialBotApiMethod<?>> response = new ArrayList<>();
                for (Serializable respElem : lastResp) {
                    String messageType = JsonPath.read(respElem, "$.method");
                    if (messageType.equalsIgnoreCase("SendMessage")) {
                        SendMessage sendMessage = objectMapper.convertValue(respElem, SendMessage.class);
                        response.add(sendMessage);
                    }
                }
                return response;
            }
        }
        return null;
    }

    private List<PartialBotApiMethod<?>> initStartMessage(Client client, String message) {
        return Collections.singletonList(
                keyboardService.prepareOptionalKeyboard(Collections.singletonList(Messages.HELP),
                        client.getId(),
                        message + "\n" + Messages.INPUT_PROMPT));
    }

    private List<PartialBotApiMethod<?>> initStartMessage(Client client) {
        return Collections.singletonList(
                keyboardService.prepareOptionalKeyboard(Collections.singletonList(Messages.HELP),
                        client.getId(),
                        Messages.INPUT_PROMPT));
    }

    private List<PartialBotApiMethod<?>> handleStartMessage(Client client, Update update) {
        if (TelegramUtils.isStartMessage(update)) {
            return initStartMessage(client);
        }
        return null;
    }

    private List<PartialBotApiMethod<?>> handleHelpMessage(Client client, Update update) {
        if (TelegramUtils.isHelpMessage(update)) {
            return Collections.singletonList(keyboardService.helpButtonHandle(update, client.getId()));
        }
        return null;
    }

    private String getName(Update update) {
        Chat chat = update.getMessage().getChat();
        return chat.getFirstName()
                + (chat.getLastName() == null ? "" : (" " + chat.getLastName()));
    }

    private String getNickName(Update update) {
        return update.getMessage().getChat().getUserName();
    }

    public AnswerInlineQuery prepareInline(Update update) {
        AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
        answerInlineQuery.setInlineQueryId(update.getInlineQuery().getId());
        if (update.getInlineQuery().getQuery().length() == 0) {
            answerInlineQuery.setResults(preparePrompt(update));
        } else {
            List<InlineQueryResult> result = prepareQueryAnswer(update.getInlineQuery().getQuery());
            answerInlineQuery.setResults(result);
        }
        return answerInlineQuery;
    }

    private List<InlineQueryResult> preparePrompt(Update update) {
        InlineQueryResultArticle inlineQueryResultArticle = new InlineQueryResultArticle();
        InputTextMessageContent itmc = new InputTextMessageContent();
        inlineQueryResultArticle.setId(update.getInlineQuery().getId());
        itmc.setMessageText(Messages.INLINE_QUERY_PROMPT);
        inlineQueryResultArticle.setInputMessageContent(itmc);
        inlineQueryResultArticle.setTitle(Messages.INLINE_QUERY_PROMPT);
        return Collections.singletonList(inlineQueryResultArticle);
    }

    private List<InlineQueryResult> prepareQueryAnswer(String receive) {
        List<InlineQueryResult> result = new ArrayList<>();
        List<String> projects = dataService.findCoordinateSystemsByPattern(receive);
        int i = 0;
        for (String project : projects) {
            InlineQueryResultArticle inlineQueryResultArticle = new InlineQueryResultArticle();
            inlineQueryResultArticle.setId(String.valueOf(++i));
            InputTextMessageContent itmc = new InputTextMessageContent();
            itmc.setMessageText(project);
            inlineQueryResultArticle.setInputMessageContent(itmc);
            inlineQueryResultArticle.setTitle(project);
            result.add(inlineQueryResultArticle);
        }
        return result;
    }
}
