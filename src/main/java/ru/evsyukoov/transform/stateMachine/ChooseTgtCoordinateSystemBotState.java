package ru.evsyukoov.transform.stateMachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.dto.OutputInfo;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.CoordinateTransformationService;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.DocumentGenerator;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;
import ru.evsyukoov.transform.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ChooseTgtCoordinateSystemBotState implements BotState {

    private final DataService dataService;

    private final CoordinateTransformationService coordinateTransformationService;

    private final InputContentHandler inputContentHandler;

    private final DocumentGenerator documentGenerator;

    private final ObjectMapper objectMapper;

    private final KeyboardService keyboardService;

    @Autowired
    public ChooseTgtCoordinateSystemBotState(DataService dataService,
                                             CoordinateTransformationService coordinateTransformationService,
                                             InputContentHandler inputContentHandler,
                                             DocumentGenerator documentGenerator,
                                             ObjectMapper objectMapper,
                                             KeyboardService keyboardService) {
        this.dataService = dataService;
        this.coordinateTransformationService = coordinateTransformationService;
        this.inputContentHandler = inputContentHandler;
        this.documentGenerator = documentGenerator;
        this.objectMapper = objectMapper;
        this.keyboardService = keyboardService;
    }

    @Override
    public String getStateMessage() {
        return null;
    }

    @Override
    public State getState() {
        return State.CHOOSE_SYSTEM_COORDINATE_TGT;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleMessage(Client client, Update update) throws Exception {
        log.info("{} state, client {}", getState().name(), client);
        if (!TelegramUtils.isTextMessage(update)) {
            log.warn("Client {} doesn't send valid message", client.getId());
            return Collections.emptyList();
        } else {
            String clientChoice = update.getMessage().getText();
            log.info("Client {} system coordinate choice - {}", client.getId(), clientChoice);
            if (!dataService.getCoordinateSystemsDescription().contains(clientChoice)) {
                log.warn("Client {} doesn't send valid coordinate system", client.getId());
                return Collections.singletonList(
                        TelegramUtils.initSendMessage(client.getId(), Messages.NO_SUCH_COORDINATE_SYSTEM));
            }
            String coordinateSystemParams = dataService.getCoordinateSystemParams(clientChoice);
            return prepareResponse(client, coordinateSystemParams);
        }
    }

    private List<PartialBotApiMethod<?>> prepareResponse(Client client, String tgtSystem) throws Exception {
        TransformationType type = dataService.getClientTransformationTypeChoice(client);
        InputInfo inputInfo = inputContentHandler.getInfo(client);
        List<Point> srcPoints = inputInfo.getPoints();
        OutputInfo outputInfo;
        if (type == TransformationType.WGS_TO_MSK) {
            outputInfo = Utils.mapToOutputInfo(coordinateTransformationService.transformPointsWgsToMsk(srcPoints, tgtSystem),
                    coordinateTransformationService.transformLinesWgsToMsk(inputInfo.getPolylines(), tgtSystem), dataService.getOutputFileFormatChoice(client));
        } else if (type == TransformationType.MSK_TO_MSK) {
            String srcSystem = dataService.getSrcCoordinateSystemChoice(client);
            outputInfo = Utils.mapToOutputInfo(coordinateTransformationService.transformPointsMskToMsk(srcPoints, srcSystem, tgtSystem),
                    coordinateTransformationService.transformLinesMskToMsk(inputInfo.getPolylines(), srcSystem, tgtSystem), dataService.getOutputFileFormatChoice(client));
        } else {
            log.error("Something wrong with chosen client {} transformation type {}", client.getId(), type);
            throw new RuntimeException();
        }

        List<PartialBotApiMethod<?>> resp = new ArrayList<>(documentGenerator.createDocuments(outputInfo, client));
        SendMessage startMsg = keyboardService.prepareOptionalKeyboard(Collections.singletonList(Messages.HELP),
                client.getId(),
                Messages.INPUT_PROMPT);
        resp.add(startMsg);
        dataService.moveClientToStart(client, true,
                objectMapper.writeValueAsString(Collections.singletonList(startMsg)));
        inputContentHandler.removeInfo(client);
        return resp;
    }
}
