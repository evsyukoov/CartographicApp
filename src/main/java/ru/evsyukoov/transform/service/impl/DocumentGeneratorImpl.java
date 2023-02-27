package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.kabeja.io.GenerationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DocumentGenerator;
import ru.evsyukoov.transform.service.OutputContentGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DocumentGeneratorImpl implements DocumentGenerator {

    private final OutputContentGenerator contentGenerator;

    private final static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM_dd_HH_mm_ss");

    @Autowired
    public DocumentGeneratorImpl(OutputContentGenerator contentGenerator) {
        this.contentGenerator = contentGenerator;
    }

    @Override
    public List<SendDocument> createDocuments(List<FileFormat> outputFormats, Client client, List<Point> points) throws IOException, GenerationException {
        List<SendDocument> documents = new ArrayList<>();
        for (FileFormat format : outputFormats) {
            ByteArrayOutputStream baos = contentGenerator.generateFile(points, format);
            if (baos != null) {
                InputFile inputFile = new InputFile(new ByteArrayInputStream(baos.toByteArray()),
                        generateOutputFileName(client.getId(), format));
                SendDocument sendDocument = SendDocument.builder()
                        .document(inputFile)
                        .chatId(String.valueOf(client.getId()))
                        .build();
                documents.add(sendDocument);
            }
        }
        return documents;
    }

    @Override
    public List<SendDocument> createDocuments(List<FileFormat> outputFormats, Client client, List<Point> points, List<Pline> lines) throws IOException, GenerationException {
        List<SendDocument> documents = new ArrayList<>();
        for (FileFormat format : outputFormats) {
            ByteArrayOutputStream baos;
            baos = contentGenerator.generateFile(points, lines, format);
            if (baos != null) {
                InputFile inputFile = new InputFile(new ByteArrayInputStream(baos.toByteArray()),
                        generateOutputFileName(client.getId(), format));
                SendDocument sendDocument = SendDocument.builder()
                        .document(inputFile)
                        .chatId(String.valueOf(client.getId()))
                        .build();
                documents.add(sendDocument);
            }
        }
        return documents;
    }

    private String generateOutputFileName(long id, FileFormat format) {
        return String.format("%s_%s.%s",
                id, dtf.format(LocalDateTime.now().plusHours(3)), format.name().toLowerCase());
    }
}
