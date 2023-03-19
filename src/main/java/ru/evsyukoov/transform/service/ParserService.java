package ru.evsyukoov.transform.service;

import ru.evsyukoov.transform.dto.InputInfo;
import ru.evsyukoov.transform.enums.FileFormat;

import java.io.IOException;
import java.io.InputStream;

public interface ParserService {

    InputInfo parseFile(InputStream inputStream, FileFormat format) throws IOException;

    /**
     * @param text - Текст, отправленный пользователем
     * @return
     */
    InputInfo parseText(String text);

    InputInfo parseCsv(InputStream inputStream) throws IOException;

    InputInfo parseTxt(InputStream inputStream) throws IOException;

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
