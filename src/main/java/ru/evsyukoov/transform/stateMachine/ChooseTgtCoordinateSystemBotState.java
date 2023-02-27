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
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.dto.AutocadFileInfo;
import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.CoordinateTransformationService;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.DocumentGenerator;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.io.IOException;
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

    @Autowired
    public ChooseTgtCoordinateSystemBotState(DataService dataService,
                                             CoordinateTransformationService coordinateTransformationService,
                                             InputContentHandler inputContentHandler,
                                             DocumentGenerator documentGenerator,
                                             ObjectMapper objectMapper) {
        this.dataService = dataService;
        this.coordinateTransformationService = coordinateTransformationService;
        this.inputContentHandler = inputContentHandler;
        this.documentGenerator = documentGenerator;
        this.objectMapper = objectMapper;
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
            dataService.moveClientToStart(client, true, objectMapper.writeValueAsString(Collections.singletonList(startMsg)));
            return Collections.singletonList(TelegramUtils.initSendMessage(client.getId(),
                    List.of(err, Messages.INPUT_PROMPT)));
        } catch (Exception e) {
            log.error("FATAL ERROR: ", e);
            return Collections.emptyList();
        }
    }

    private List<PartialBotApiMethod<?>> prepareResponse(Client client, String tgtSystem) throws IOException, GenerationException {
        TransformationType type = dataService.getClientTransformationTypeChoice(client);
        List<Point> points;
        FileInfo fileInfo = inputContentHandler.getInfo(client);
        List<Point> srcPoints = fileInfo.getPoints();
        List<Pline> lines = null;
        boolean needAutocadLines = fileInfo instanceof AutocadFileInfo;
        if (type == TransformationType.WGS_TO_MSK) {
            points = coordinateTransformationService.transformPointsWgsToMsk(srcPoints, tgtSystem);
            if (needAutocadLines) {
                lines = coordinateTransformationService.transformLinesWgsToMsk(((AutocadFileInfo) fileInfo).getPolylines(), tgtSystem);
            }
        } else if (type == TransformationType.MSK_TO_MSK) {
            String srcSystem = dataService.getSrcCoordinateSystemChoice(client);
            points = coordinateTransformationService.transformPointsMskToMsk(srcPoints, srcSystem, tgtSystem);
            if (needAutocadLines) {
                lines = coordinateTransformationService.transformLinesMskToMsk(((AutocadFileInfo) fileInfo).getPolylines(),srcSystem, tgtSystem);
            }
        } else {
            throw new RuntimeException();
        }

        List<FileFormat> clientFileFormatChoice = dataService.getOutputFileFormatChoice(client);
        List<PartialBotApiMethod<?>> resp;
        if (lines == null) {
            resp = new ArrayList<>(documentGenerator.createDocuments(clientFileFormatChoice, client, points));
        } else {
            resp = new ArrayList<>(documentGenerator.createDocuments(clientFileFormatChoice, client, points, lines));
        }
        SendMessage startMsg = TelegramUtils.initSendMessage(client.getId(), Messages.INPUT_PROMPT);
        resp.add(startMsg);
        dataService.moveClientToStart(client, true,
                objectMapper.writeValueAsString(Collections.singletonList(startMsg)));
        return resp;
    }
}
