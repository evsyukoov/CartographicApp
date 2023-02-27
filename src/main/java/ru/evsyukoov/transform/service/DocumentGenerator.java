package ru.evsyukoov.transform.service;

import org.kabeja.io.GenerationException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.model.Client;

import java.io.IOException;
import java.util.List;

public interface DocumentGenerator {

    List<SendDocument> createDocuments(List<FileFormat> outputFormats, Client client, List<Point> points) throws IOException, GenerationException;

    List<SendDocument> createDocuments(List<FileFormat> outputFormats, Client client, List<Point> points, List<Pline> lines) throws IOException, GenerationException;
}
