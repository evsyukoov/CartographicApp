package ru.evsyukoov.transform.utils;

import ru.evsyukoov.transform.dto.OutputInfo;
import ru.evsyukoov.transform.dto.Pline;
import ru.evsyukoov.transform.dto.Point;
import ru.evsyukoov.transform.enums.FileFormat;
import java.util.List;

public class Utils {

    public static OutputInfo mapToOutputInfo(List<Point> points, List<Pline> lines, List<FileFormat> chosenFormats) {
        OutputInfo outputInfo = new OutputInfo();
        outputInfo.getLines().addAll(lines);
        outputInfo.getPoints().addAll(points);
        outputInfo.setChosenFormat(chosenFormats);
        return outputInfo;
    }

    public static String getLocalFilePath(String fileStoragePath, long clientId, FileFormat format) {
        return String.format("%s/%d.%s", fileStoragePath, clientId, format.name());
    }
}
