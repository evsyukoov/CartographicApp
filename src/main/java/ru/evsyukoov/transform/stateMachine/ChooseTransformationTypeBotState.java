package ru.evsyukoov.transform.stateMachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.CoordinatesType;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
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

    @Autowired
    public ChooseTransformationTypeBotState(@Lazy BotStateFactory botStateFactory,
                                            DataService dataService,
                                            KeyboardService keyboardService) {
        this.botStateFactory = botStateFactory;
        this.dataService = dataService;
        this.keyboardService = keyboardService;
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
    public List<BotApiMethod<?>> handleMessage(Client client, Update update) {
        log.info("{} state, client {}", getState().name(), client);
        if (!TelegramUtils.isCallbackMessage(update)) {
            return Collections.emptyList();
        } else {
            if (update.getCallbackQuery().getData().equals(Messages.BACK)) {
                List<BotApiMethod<?>> res = Collections.singletonList(
                        botStateFactory.initPrevState(client).getStartMessage(client.getId()));
                dataService.updateClientState(client, State.INPUT, null);
                return res;
            }
            else {
                String payload = update.getCallbackQuery().getData().substring(Messages.CONFIRM_SYMBOL.length() + 1);
                TransformationType choice = TransformationType.getTypeByDescription(payload);
                if (choice == null) {
                    return Collections.emptyList();
                } else {
                    EditMessageReplyMarkup markup = keyboardService.pressButtonsChoiceHandle(update, client.getId());
                    dataService.updateClientState(client, State.CHOOSE_OUTPUT_FILE_OPTION, getState());
                    BotState next = botStateFactory.initState(client);
                    SendMessage msg = TelegramUtils.initSendMessage(client.getId(),
                            next.getStateMessage());
                    return List.of(markup, msg);
                }
            }
        }
    }

    private SendMessage prepareOutputMessage(FileInfo fileInfo, String textMessage, long clientId) {
        List<String> outputFormats = prepareOutputFormats(fileInfo).stream()
                .map(FileFormat::getDescription)
                .collect(Collectors.toList());
        List<String> optional = List.of(Messages.APPROVE, Messages.BACK);
        return keyboardService.prepareKeyboard(outputFormats, optional, clientId, textMessage);
    }

    private List<FileFormat> prepareOutputFormats(FileInfo fileInfo) {
        if (fileInfo.getCoordinatesType() == CoordinatesType.WGS_84) {
            List<FileFormat> outputFormats = Stream.of(FileFormat.CSV_RECTANGULAR, FileFormat.GPX, FileFormat.KML, FileFormat.DXF).collect(Collectors.toList());
            if (fileInfo.getFormat() != FileFormat.CSV) {
                outputFormats.add(FileFormat.CSV);
            }
            outputFormats.removeIf(f -> f == fileInfo.getFormat());
            return outputFormats;
        } else {
            List<FileFormat> outputFormats = Stream.of(FileFormat.CSV, FileFormat.CSV_RECTANGULAR, FileFormat.KML, FileFormat.GPX).collect(Collectors.toList());
            outputFormats.removeIf(f -> f == fileInfo.getFormat());
            return outputFormats;
        }
    }
}
