package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.service.ParserService;
import ru.evsyukoov.transform.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
public class InputContentHandlerImpl implements InputContentHandler {

    private final Map<Long, InputInfo> clientFileCache;

    private final DataService dataService;

    private final ParserService parserService;

    @Value("${file-storage.upload}")
    private String fileStoragePath;

    @Autowired
    public InputContentHandlerImpl(Map<Long, InputInfo> clientFileCache,
                                   DataService dataService,
                                   ParserService parserService) {
        this.clientFileCache = clientFileCache;
        this.dataService = dataService;
        this.parserService = parserService;
    }

    @Override
    public void removeInfo(Client client) {
        log.info("Successfully remove input content from client {} cache", client.getId());
        clientFileCache.remove(client.getId());
    }

    @Override
    public InputInfo getInfo(Client client) throws IOException {
        if (clientFileCache.isEmpty() || !clientFileCache.containsKey(client.getId())) {
            FileFormat format = dataService.getClientFileFormatChoice(client);
            log.warn("No file info at app cache for client {}", client);
            InputInfo inputInfo = parserService.parseFile(new FileInputStream(Utils.getLocalFilePath(fileStoragePath, client.getId(), format)),
                    format);
            clientFileCache.put(client.getId(), inputInfo);
            return inputInfo;
        }
        log.info("Successfully get input content from client {} cache", client.getId());
        return clientFileCache.get(client.getId());
    }

    @Override
    public InputInfo putInfo(InputStream inputStream, FileFormat format, long clientId) throws IOException {
        InputInfo inputInfo = parserService.parseFile(inputStream, format);
        clientFileCache.put(clientId, inputInfo);
        log.info("Successfully put input client file content {} to cache", clientId);
        return inputInfo;
    }

    @Override
    public InputInfo putInfo(String text, long clientId) {
        InputInfo inputInfo = parserService.parseText(text);
        clientFileCache.put(clientId, inputInfo);
        log.info("Successfully put input client text content {} to cache", clientId);
        return inputInfo;
    }

}
