package ru.evsyukoov.transform.stateMachine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.kabeja.io.GenerationException;
import org.osgeo.proj4j.Proj4jException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.OutputInfo;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.CoordinateTransformationService;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.DocumentGenerator;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;
import ru.evsyukoov.transform.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ChooseSrcCoordinateSystemBotState implements BotState {

    private final DataService dataService;

    private final DocumentGenerator documentGenerator;

    private final InputContentHandler inputContentHandler;

    private final CoordinateTransformationService coordinateTransformationService;

    private final ObjectMapper objectMapper;

    private final KeyboardService keyboardService;

    @Autowired
    public ChooseSrcCoordinateSystemBotState(DataService dataService,
                                             DocumentGenerator documentGenerator,
                                             InputContentHandler inputContentHandler,
                                             CoordinateTransformationService coordinateTransformationService,
                                             ObjectMapper objectMapper,
                                             KeyboardService keyboardService) {
        this.dataService = dataService;
        this.documentGenerator = documentGenerator;
        this.inputContentHandler = inputContentHandler;
        this.coordinateTransformationService = coordinateTransformationService;
        this.objectMapper = objectMapper;
        this.keyboardService = keyboardService;
    }

    @Override
    public String getStateMessage() {
        return null;
    }

    @Override
    public State getState() {
        return State.CHOOSE_SYSTEM_COORDINATE_SRC;
    }

    @Override
    public List<PartialBotApiMethod<?>> handleMessage(Client client, Update update) throws JsonProcessingException {
        try {
            if (!TelegramUtils.isTextMessage(update)) {
                return Collections.emptyList();
            } else {
                String clientChoice = update.getMessage().getText();
                if (!dataService.getCoordinateSystemsDescription().contains(clientChoice)) {
                    return Collections.singletonList(
                            TelegramUtils.initSendMessage(client.getId(), Messages.NO_SUCH_COORDINATE_SYSTEM));
                }
                String coordinateSystemParams = dataService.getCoordinateSystemParams(clientChoice);
                return prepareResponse(client, coordinateSystemParams);
            }
        } catch (Proj4jException | IllegalStateException e) {
            String err = "Не удалось трансформировать точки";
            log.error("Error: {}, ex: ", err, e);
            SendMessage startMsg = TelegramUtils.initSendMessage(client.getId(), Messages.INPUT_PROMPT);
            List<PartialBotApiMethod<?>> resp = Collections.singletonList(TelegramUtils.initSendMessage(client.getId(),
                    List.of(err, Messages.INPUT_PROMPT)));
            dataService.moveClientToStart(client, true,
                    objectMapper.writeValueAsString(Collections.singletonList(startMsg)));
            return resp;
        } catch (Exception e) {
            log.error("FATAL ERROR: ", e);
            return Collections.emptyList();
        }
    }

    private List<PartialBotApiMethod<?>> prepareResponse(Client client, String srcSystem) throws Exception {
        TransformationType type = dataService.getClientTransformationTypeChoice(client);
        InputInfo inputInfo = inputContentHandler.getInfo(client);
        List<Point> srcPoints = inputInfo.getPoints();
        List<PartialBotApiMethod<?>> resp;
        OutputInfo outputInfo;
        if (type == TransformationType.MSK_TO_WGS) {
            outputInfo = Utils.mapToOutputInfo(coordinateTransformationService.transformPointsMskToWgs(srcPoints, srcSystem),
                    coordinateTransformationService.transformLinesMskToWgs(inputInfo.getPolylines(), srcSystem), dataService.getOutputFileFormatChoice(client));
        } else if (type == TransformationType.MSK_TO_MSK) {
            //перекидываем на следуюший стейт
            resp = Collections.singletonList(
                    keyboardService.preparePromptInlineKeyboard(List.of(Messages.BACK), client.getId(), Messages.COORDINATE_SYSTEM_TARGET_CHOICE));
            dataService.updateClientState(client, State.CHOOSE_SYSTEM_COORDINATE_TGT,
                    objectMapper.writeValueAsString(resp), srcSystem);
            return resp;
        } else {
            throw new RuntimeException();
        }

        resp = new ArrayList<>(documentGenerator.createDocuments(outputInfo, client));
        SendMessage startMsg = TelegramUtils.initSendMessage(client.getId(), Messages.INPUT_PROMPT);
        resp.add(startMsg);
        dataService.moveClientToStart(client, true, objectMapper.writeValueAsString(Collections.singletonList(startMsg)));
        return resp;
    }
}
