package ru.evsyukoov.transform.stateMachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.DocumentGenerator;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;
import ru.evsyukoov.transform.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChooseOutputFileFormatBotState implements BotState {

    private final KeyboardService keyboardService;

    private final DataService dataService;

    private final DocumentGenerator documentGenerator;

    private final InputContentHandler inputContentHandler;

    private final ObjectMapper objectMapper;

    @Autowired
    public ChooseOutputFileFormatBotState(KeyboardService keyboardService,
                                          DataService dataService,
                                          DocumentGenerator documentGenerator,
                                          InputContentHandler inputContentHandler,
                                          ObjectMapper objectMapper) {
        this.keyboardService = keyboardService;
        this.dataService = dataService;
        this.documentGenerator = documentGenerator;
        this.inputContentHandler = inputContentHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getStateMessage() {
        return Messages.FILE_FORMAT_CHOICE;
    }

    @Override
    public State getState() {
        return State.CHOOSE_OUTPUT_FILE_OPTION;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleMessage(Client client, Update update) throws Exception {
        log.info("{} state, client {}", getState().name(), client);
        if (!TelegramUtils.isCallbackMessage(update)) {
            return Collections.emptyList();
        } else {
            String text = update.getCallbackQuery().getData();
            if (text.equals(Messages.APPROVE)) {
                List<FileFormat> outputFormats = keyboardService.getPressedItems(update, client.getId()).stream()
                        .map(FileFormat::valueOf)
                        .collect(Collectors.toList());
                return prepareResponse(client, outputFormats);
            }
            return Collections.singletonList(
                    keyboardService.pressButtonsChoiceHandle(update, client.getId()));
        }
    }

    private List<PartialBotApiMethod<?>> prepareResponse(Client client, List<FileFormat> outputFormats) throws Exception {
        TransformationType type = dataService.getClientTransformationTypeChoice(client);
        if (CollectionUtils.isEmpty(outputFormats)) {
            log.warn("Client {} doesn't pressed any button", client);
            return Collections.emptyList();
        }
        // такой тип перевода не требует запроса системы координат, просто конвертируем файлы, отправляем, конец диалога
        List<PartialBotApiMethod<?>> resp = new ArrayList<>();
        if (type == TransformationType.WGS_TO_WGS) {
            InputInfo inputInfo = inputContentHandler.getInfo(client);
            resp.addAll(documentGenerator.createDocuments(Utils.mapToOutputInfo(inputInfo.getPoints(), inputInfo.getPolylines(), outputFormats), client));
            SendMessage startMsg = keyboardService.prepareOptionalKeyboard(Collections.singletonList(Messages.HELP),
                    client.getId(),
                    Messages.INPUT_PROMPT);;
            resp.add(startMsg);
            dataService.moveClientToStart(client, true,
                    objectMapper.writeValueAsString(Collections.singletonList(startMsg)));
            inputContentHandler.removeInfo(client);
        } else if (type == TransformationType.WGS_TO_MSK) {
            resp = Collections.singletonList(
                    keyboardService.preparePromptInlineKeyboard(List.of(Messages.BACK, Messages.HELP), client.getId(), Messages.COORDINATE_SYSTEM_TARGET_CHOICE));
            String clientChoice = outputFormats.stream().map(FileFormat::name).collect(Collectors.joining(","));
            dataService.updateClientState(client, State.CHOOSE_SYSTEM_COORDINATE_TGT,
                    objectMapper.writeValueAsString(resp), clientChoice);
            return resp;
        } else if (type == TransformationType.MSK_TO_WGS || type == TransformationType.MSK_TO_MSK) {
            resp = Collections.singletonList(
                    keyboardService.preparePromptInlineKeyboard(List.of(Messages.BACK, Messages.HELP), client.getId(), Messages.COORDINATE_SYSTEM_SRC_CHOICE));
            String clientChoice = outputFormats.stream().map(FileFormat::name).collect(Collectors.joining(","));
            dataService.updateClientState(client, State.CHOOSE_SYSTEM_COORDINATE_SRC,
                    objectMapper.writeValueAsString(resp), clientChoice);
            return resp;
        }
        return resp;
    }
}
