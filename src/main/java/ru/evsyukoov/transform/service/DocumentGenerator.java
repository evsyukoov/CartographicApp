package ru.evsyukoov.transform.service;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import ru.evsyukoov.transform.dto.OutputInfo;
import ru.evsyukoov.transform.model.Client;

import java.io.IOException;
import java.util.List;

public interface DocumentGenerator {

    List<SendDocument> createDocuments(OutputInfo outputInfo, Client client) throws IOException, Exception;

}
