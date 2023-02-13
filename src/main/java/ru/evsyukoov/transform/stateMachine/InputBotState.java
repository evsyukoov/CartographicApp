package ru.evsyukoov.transform.stateMachine;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.exceptions.UploadFileException;
import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.service.FileParser;
import ru.evsyukoov.transform.utils.TelegramUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
public class InputBotState implements BotState {

    private final FileParser fileParser;

    @Autowired
    public InputBotState(FileParser fileParser) {
        this.fileParser = fileParser;
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
    public String getStateMessage() {
        return null;
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
                } else if (TelegramUtils.isDocumentMessage(update)) {
                    FileAbout about  = downloadFile(update, client.getId());
                    switch (about.fileFormat) {
                        case CSV:
                            fileInfo = fileParser.parseCsv(about.contentStream, about.charset);
                            break;
                        case KML:
                            fileInfo = fileParser.parseKml(about.contentStream);
                            break;
                        case GPX:
                            fileInfo = fileParser.parseGpx(about.contentStream);
                            break;
                        case DXF:
                            fileInfo = fileParser.parseDxf(about.contentStream);
                            break;
                        case TXT:
                            fileInfo = fileParser.parseTxt(about.contentStream, about.charset);
                            break;
                        case KMZ:
                            fileInfo = fileParser.parseKmz(about.contentStream);
                            break;
                    }
                }
            } else {

            }
            return List.of(SendMessage.builder()
                    .text(Messages.WRONG_FORMAT_MESSAGE)
                    .build());
        } catch (WrongFileFormatException e) {
            return List.of(SendMessage.builder()
                    .text(Messages.WRONG_FILE_EXTENSION)
                    .build());
        } catch (UploadFileException e) {
            return List.of(SendMessage.builder()
                    .text(Messages.ERROR_WHILE_UPLOADING_FILE)
                    .build());
        } catch (IOException e) {
            return List.of(SendMessage.builder()
                    .text("Проблема на сервере")
                    .build());
        }
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
            downloadFileAndSave(downloadLink.openStream(), new File(localFileName), charset);
            log.info("Successfully download file {}", localFileName);
        } catch (IOException e) {
            log.error("Problem with downloading file from telegram servers: ", e);
            throw new UploadFileException("Problem with downloading file from telegram servers", e);
        }
        return inputInfo;
    }

    private void downloadFileAndSave(InputStream serverInputStream, File localFile, String charset) throws IOException{
        try (FileWriter fw = new FileWriter(localFile);
             BufferedReader uploadIn = new BufferedReader(new InputStreamReader(serverInputStream, charset))) {
            String s;
            while ((s = uploadIn.readLine()) != null) {
                fw.write(String.format("%s\n", s));
            }
        }
    }

    private FileFormat findExtension(String path) {
        String extension = null;
        try {
            int pos = path.indexOf('.');
            if (pos == -1)
                throw new WrongFileFormatException("Not found file extension");
            extension = path.substring(pos + 1);
            return FileFormat.valueOf(extension.toUpperCase());
        } catch (Exception e) {
            throw new WrongFileFormatException(
                    String.format("Extension %s not at extensions list", extension));
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
