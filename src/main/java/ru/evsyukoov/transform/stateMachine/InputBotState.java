package ru.evsyukoov.transform.stateMachine;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.exceptions.UploadFileException;
import ru.evsyukoov.transform.exceptions.WrongFileFormatException;
import ru.evsyukoov.transform.model.Client;
import ru.evsyukoov.transform.utils.TelegramUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

@Component
@Slf4j
public class InputBotState implements BotState {

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
            if (!TelegramUtils.isCallbackMessage(update)) {
                if (TelegramUtils.isTextMessage(update)) {

                } else if (TelegramUtils.isDocumentMessage(update)) {
                    downloadFile(update, client.getId());
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

    private void downloadFile(Update update, long id) {
        Document doc = update.getMessage().getDocument();
        try {
            String fileServerPath = getFileServerPath(doc);
            FileFormat format = findExtension(fileServerPath);
            URL downloadLink = new URL("https://api.telegram.org/file/bot" + token + "/" + fileServerPath);
            String charset = (format == FileFormat.CSV || format == FileFormat.TXT) ? "windows-1251" : "UTF-8";
            String localFileName = String.format("%s/%d.%s", fileStoragePath, id, format.name());
            downloadFile(downloadLink.openStream(), new File(localFileName), charset);
            log.info("Successfully download file {}", localFileName);
        } catch (IOException e) {
            log.error("Problem with downloading file from telegram servers: ", e);
            throw new UploadFileException("Problem with downloading file from telegram servers", e);
        }
    }

    private void downloadFile(InputStream serverInputStream, File localFile, String charset) throws IOException{
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
            return FileFormat.valueOf(extension);
        } catch (Exception e) {
            throw new WrongFileFormatException(
                    String.format("Extension %s not at extensions list", extension));
        }
    }
}
