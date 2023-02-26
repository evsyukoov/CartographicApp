package ru.evsyukoov.transform.stateMachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class ChooseTransformationTypeBotState implements BotState {

    private final BotStateFactory botStateFactory;

    private final DataService dataService;

    private final KeyboardService keyboardService;

    private final InputContentHandler inputContentHandler;

    private final ObjectMapper objectMapper;

    @Autowired
    public ChooseTransformationTypeBotState(@Lazy BotStateFactory botStateFactory,
                                            DataService dataService,
                                            KeyboardService keyboardService,
                                            InputContentHandler inputContentHandler,
                                            ObjectMapper objectMapper) {
        this.botStateFactory = botStateFactory;
        this.dataService = dataService;
        this.keyboardService = keyboardService;
        this.inputContentHandler = inputContentHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStateMessage() {
        return Messages.TRANSFORMATION_TYPE_CHOICE;
    }

    @Override
    public State getState() {
        return State.CHOOSE_TRANSFORMATION_TYPE;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleMessage(Client client, Update update) {
        log.info("{} state, client {}", getState().name(), client);
        try {
            if (!TelegramUtils.isCallbackMessage(update)) {
                return Collections.emptyList();
            } else {
                String payload = update.getCallbackQuery().getData().substring(Messages.CONFIRM_SYMBOL.length() + 1);
                TransformationType choice = TransformationType.getTypeByDescription(payload);
                if (choice == null) {
                    return Collections.emptyList();
                } else {
                    EditMessageReplyMarkup markup = keyboardService.pressButtonsChoiceHandle(update, client.getId());
                    FileInfo fileInfo = inputContentHandler.getInfo(client);
                    List<PartialBotApiMethod<?>> resp = List.of(markup, prepareOutputMessage(fileInfo, Messages.FILE_FORMAT_CHOICE, client.getId(), choice));
                    dataService.updateClientState(client, State.CHOOSE_OUTPUT_FILE_OPTION,
                            objectMapper.writeValueAsString(resp), choice.name());
                    return resp;
                }
            }
        } catch (Exception e) {
            log.error("FATAL ERROR: ", e);
            return Collections.singletonList(
                    TelegramUtils.initSendMessage(client.getId(), List.of(Messages.FATAL_ERROR, getStateMessage())));
        }
    }

    private SendMessage prepareOutputMessage(FileInfo fileInfo, String textMessage, long clientId, TransformationType type) {
        List<String> outputFormats = prepareOutputFormats(fileInfo, type).stream()
                .map(FileFormat::getDescription)
                .collect(Collectors.toList());
        List<String> optional = List.of(Messages.APPROVE, Messages.BACK);
        return keyboardService.prepareKeyboard(outputFormats, optional, clientId, textMessage);
    }

    private List<FileFormat> prepareOutputFormats(FileInfo fileInfo, TransformationType type) {
        List<FileFormat> outputFormats = null;
        switch (type) {
            case WGS_TO_WGS:
                outputFormats = Stream.of(FileFormat.TXT, FileFormat.GPX, FileFormat.KML, FileFormat.CSV).collect(Collectors.toList());
                outputFormats.removeIf(f -> f == fileInfo.getFormat());
                break;
            case WGS_TO_MSK:
            case MSK_TO_MSK:
                outputFormats = List.of(FileFormat.TXT, FileFormat.CSV, FileFormat.DXF);
                break;
            case MSK_TO_WGS:
                outputFormats = List.of(FileFormat.TXT, FileFormat.CSV, FileFormat.GPX, FileFormat.KML);
                break;
        }
        return outputFormats;
    }
}
