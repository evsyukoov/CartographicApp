package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.model.Client;

import java.io.IOException;
import java.io.InputStream;

public interface InputContentHandler {

    /**
     * Удаление отправленного клиентом контента из кеша
     * @param client
     */
    void removeInfo(Client client);

    /**
     * Получение клиентских данных из кеша или с диска, если кеш пуст
     * @param client
     * @return
     * @throws IOException
     */
    InputInfo getInfo(Client client) throws IOException;

    /**
     * Получить контент из входящего потока и положить в кеш
     * @param inputStream
     * @param format
     * @param clientId
     * @return
     * @throws IOException
     */
    InputInfo putInfo(InputStream inputStream, FileFormat format, long clientId) throws IOException;

    /**
     * Получить контент из текста и положить в кеш
     * @param text
     * @param clientId
     * @return
     */
    InputInfo putInfo(String text, long clientId);

}
