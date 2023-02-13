package ru.evsyukoov.transform.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.stateMachine.BotState;
import ru.evsyukoov.transform.stateMachine.BotStateFactory;
import ru.evsyukoov.transform.stateMachine.State;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class MessageHandler {

    private final DataService dataService;

    private final BotStateFactory stateFactory;

    @Autowired
    public MessageHandler(DataService dataService,
                          BotStateFactory stateFactory) {
        this.stateFactory = stateFactory;
        this.dataService = dataService;
    }

    public List<BotApiMethod<?>> prepareMessage(Update update) {
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
        if (client.getState() == null) {
            client.setState(State.INPUT);
        }
        BotState currentState = stateFactory.initState(client);
        List<BotApiMethod<?>> start = currentState.handleStartMessage(client, update);
        if (start != null) {
            dataService.moveClientToStart(client, false);
        }
        return start == null ? currentState.handleMessage(client, update) : start;
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
