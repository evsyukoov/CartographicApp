package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.model.Client;

import java.io.IOException;
import java.io.InputStream;

public interface InputContentHandler {

    void removeInfo(Client client);

    InputInfo getInfo(Client client) throws IOException;

    InputInfo putInfo(InputStream inputStream, String charset, FileFormat format, long clientId) throws IOException;

    InputInfo putInfo(String text, long clientId);

    InputInfo parseFile(InputStream inputStream, String charset, FileFormat format) throws IOException;

    /**
     * @param text - Текст, отправленный пользователем
     * @return
     */
    InputInfo parseText(String text);

    InputInfo parseCsv(InputStream inputStream, String charset) throws IOException;

    InputInfo parseTxt(InputStream inputStream, String charset) throws IOException;

    /**
     * Парсим только точки, без линий
     * @param inputStream - IS с файлового сервера ТГ
     * @return
     */
    InputInfo parseKml(InputStream inputStream) throws IOException;

    /**
     * Архив, состоящий из нескольких KML файлов и папок с графическим отображением меток (игнорируем)
     * @param inputStream
     * @return
     * @throws IOException
     */
    InputInfo parseKmz(InputStream inputStream) throws IOException;

    /**
     * Парсим только точки, без линий
     * @param inputStream - IS с файлового сервера ТГ
     * @return
     */
    InputInfo parseGpx(InputStream inputStream) throws IOException;

    /**
     * Парсим только блоки и полилинии, имя точки - первый непустой атрибут блока
     * @param inputStream - IS с файлового сервера ТГ
     * @return
     */
    InputInfo parseDxf(InputStream inputStream);
}
