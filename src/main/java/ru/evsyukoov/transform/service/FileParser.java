package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.FileInfo;

import java.io.IOException;
import java.io.InputStream;

public interface FileParser {

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
     * Архив, состоящий из нескольких KML файлов
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
