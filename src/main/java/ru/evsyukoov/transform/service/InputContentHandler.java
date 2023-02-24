package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.FileInfo;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.model.Client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface InputContentHandler {

    FileInfo getInfo(Client client) throws FileNotFoundException, IOException;

    FileInfo putInfo(InputStream inputStream, String charset, FileFormat format, long clientId) throws IOException;

    FileInfo putInfo(String text, long clientId) throws IOException;

    FileInfo parseFile(InputStream inputStream, String charset, FileFormat format) throws IOException;

    /**
     * @param text - Текст, отправленный пользователем
     * @return
     */
    FileInfo parseText(String text);

    FileInfo parseCsv(InputStream inputStream, String charset) throws IOException;

    FileInfo parseTxt(InputStream inputStream, String charset) throws IOException;

    /**
     * Парсим только точки, без линий
     * @param inputStream - IS с файлового сервера ТГ
     * @return
     */
    FileInfo parseKml(InputStream inputStream) throws IOException;

    /**
     * Архив, состоящий из нескольких KML файлов и папок с графическим отображением меток (игнорируем)
     * @param inputStream
     * @return
     * @throws IOException
     */
    FileInfo parseKmz(InputStream inputStream) throws IOException;

    /**
     * Парсим только точки, без линий
     * @param inputStream - IS с файлового сервера ТГ
     * @return
     */
    FileInfo parseGpx(InputStream inputStream) throws IOException;

    /**
     * Парсим только блоки и полилинии, имя точки - первый непустой атрибут блока
     * @param inputStream - IS с файлового сервера ТГ
     * @return
     */
    FileInfo parseDxf(InputStream inputStream);
}
