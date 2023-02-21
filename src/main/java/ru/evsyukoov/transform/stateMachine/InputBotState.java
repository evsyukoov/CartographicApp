package ru.evsyukoov.transform.stateMachine;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.CoordinatesType;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.exceptions.UploadFileException;
import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.DataService;
import ru.evsyukoov.transform.service.InputContentHandler;
import ru.evsyukoov.transform.service.KeyboardService;
import ru.evsyukoov.transform.utils.TelegramUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class InputBotState implements BotState {

    private final InputContentHandler fileParser;

    private final Map<Long, FileInfo> clientFileCache;

    private final KeyboardService keyboardService;

    private final DataService dataService;

    private BotStateFactory botStateFactory;

    @Autowired
    public InputBotState(InputContentHandler fileParser,
                         Map<Long, FileInfo> clientFileCache,
                         KeyboardService keyboardService,
                         DataService dataService) {
        this.fileParser = fileParser;
        this.clientFileCache = clientFileCache;
        this.keyboardService = keyboardService;
        this.dataService = dataService;
    }

    @Autowired
    public void setBotStateFactory(@Lazy BotStateFactory botStateFactory) {
        this.botStateFactory = botStateFactory;
    }

    @PostConstruct
    public void initLocalFileStorage() throws IOException {
        Path path = Paths.get(fileStoragePath);
        if (Files.notExists(path)) {
            Files.createDirectory(path);
            log.info("Create directory for saving files from clients");
        }
    }

    @Value("${bot.token}")
    private String token;

    @Value("${file-storage.upload}")
    private String fileStoragePath;

    @Override
    public SendMessage getStartMessage(long clientId) {
        return keyboardService.prepareOptionalKeyboard(
                Collections.singletonList(Messages.HELP), clientId, getStateMessage()
        );
    }

    @Override
    public String getStateMessage() {
        return Messages.INPUT_PROMPT;
    }

    @Override
    public State getState() {
        return State.INPUT;
    }

    @Override
    public List<BotApiMethod<?>> handleMessage(Client client, Update update) {
        try {
            FileInfo fileInfo;
            if (!TelegramUtils.isCallbackMessage(update)) {
                if (TelegramUtils.isTextMessage(update)) {
                    fileInfo = fileParser.parseText(update.getMessage().getText());
                    clientFileCache.put(client.getId(), fileInfo);
                    dataService.updateClientState(client, State.CHOOSE_TRANSFORMATION_TYPE, State.INPUT);
                    return Collections.singletonList(prepareOutputMessage(fileInfo, client.getId()));
                } else if (TelegramUtils.isDocumentMessage(update)) {
                    FileAbout about  = downloadFile(update, client.getId());
                    fileInfo = fileParser.parseFile(about.contentStream, about.charset, about.fileFormat);
                    clientFileCache.put(client.getId(), fileInfo);
                    dataService.updateClientState(client, State.CHOOSE_TRANSFORMATION_TYPE, State.INPUT);
                    return Collections.singletonList(prepareOutputMessage(fileInfo, client.getId()));
                }
            } else {
                if (TelegramUtils.isHelpMessage(update)) {
                    dataService.updateClientState(client, State.HELP, State.INPUT);
                    BotState next = botStateFactory.initState(client);
                    return List.of(
                            keyboardService.prepareOptionalKeyboard(List.of(Messages.BACK), client.getId(), next.getStateMessage()));
                }
                return Collections.singletonList(
                        TelegramUtils.initSendMessage(client.getId(), getStateMessage()));
            }
            return List.of(
                    TelegramUtils.initSendMessage(client.getId(), Messages.WRONG_FORMAT_MESSAGE), getStartMessage(client.getId()));
        } catch (WrongFileFormatException | UploadFileException e) {
            return List.of(
                    TelegramUtils.initSendMessage(client.getId(), List.of(e.getMessage(), getStateMessage())));
        } catch (Exception e) {
            return Collections.singletonList(
                    TelegramUtils.initSendMessage(client.getId(), List.of(Messages.FATAL_ERROR, getStateMessage())));
        }
    }

    private SendMessage prepareOutputMessage(FileInfo fileInfo, long clientId) {
        Stream<TransformationType> types;
        if (fileInfo.getCoordinatesType() == CoordinatesType.WGS_84) {
            types = Stream.of(TransformationType.WGS_TO_WGS, TransformationType.WGS_TO_MSK);
        } else {
            types = Stream.of(TransformationType.MSK_TO_WGS, TransformationType.MSK_TO_MSK);
        }
        return keyboardService.prepareKeyboard(types.map(TransformationType::getDescription).collect(Collectors.toList()),
                List.of(Messages.BACK), clientId, Messages.TRANSFORMATION_TYPE_CHOICE);
    }

    private String getFileServerPath(Document document) throws IOException {
        URL url = new URL("https://api.telegram.org/bot"
                + token + "/getFile?file_id=" + document.getFileId());
        String json;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            json = in.readLine();
        }
        return JsonPath.read(json, "$.result.file_path");
    }

    private FileAbout downloadFile(Update update, long id) {
        Document doc = update.getMessage().getDocument();
        FileAbout inputInfo;
        try {
            String fileServerPath = getFileServerPath(doc);
            FileFormat format = findExtension(fileServerPath);
            URL downloadLink = new URL("https://api.telegram.org/file/bot" + token + "/" + fileServerPath);
            String charset = (format == FileFormat.CSV || format == FileFormat.TXT) ? "windows-1251" : "UTF-8";
            String localFileName = String.format("%s/%d.%s", fileStoragePath, id, format.name());
            inputInfo = new FileAbout(downloadLink.openStream(), charset, format);
            if (format != FileFormat.KMZ) {
                downloadTextFileAndSave(downloadLink.openStream(), new File(localFileName), charset);
            } else {
                uploadZipFileAndSave(downloadLink.openStream(), new File(localFileName));
            }
            log.info("Successfully download file {}", localFileName);
        } catch (IOException e) {
            String err = "Проблема с загрузкой файла с сервера телеграм";
            log.error(err, e);
            throw new UploadFileException(err, e);
        }
        return inputInfo;
    }

    private void downloadTextFileAndSave(InputStream serverInputStream, File localFile, String charset) throws IOException{
        try (FileWriter fw = new FileWriter(localFile);
             BufferedReader uploadIn = new BufferedReader(new InputStreamReader(serverInputStream, charset))) {
            String s;
            while ((s = uploadIn.readLine()) != null) {
                fw.write(String.format("%s\n", s));
            }
        }
    }

    public void uploadZipFileAndSave(InputStream serverInputStream, File localFile) throws IOException {
        try (ReadableByteChannel rbc = Channels.newChannel(serverInputStream);
             FileOutputStream fos = new FileOutputStream(localFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    private FileFormat findExtension(String path) {
        String extension = null;
        try {
            int pos = path.indexOf('.');
            if (pos == -1) {
                String err = "Не найдено расширение у присланного файла";
                log.error(err);
                throw new WrongFileFormatException(err);
            }
            extension = path.substring(pos + 1);
            return FileFormat.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            String err = String.format("Расширение файла %s не поддерживается", extension);
            log.error("Err: {}", err, e);
            throw new WrongFileFormatException(err);
        }
    }

    private static class FileAbout {
        InputStream contentStream;
        String charset;
        FileFormat fileFormat;

        public FileAbout(InputStream contentStream, String charset, FileFormat fileFormat) {
            this.contentStream = contentStream;
            this.charset = charset;
            this.fileFormat = fileFormat;
        }

        public InputStream getContentStream() {
            return contentStream;
        }

        public String getCharset() {
            return charset;
        }

        public FileFormat getFileFormat() {
            return fileFormat;
        }
    }
}
