package ru.evsyukoov.transform.constants;

public final class Messages {

    public static final String DELIMETR = ",";

    public final static String INLINE_QUERY_PROMPT = "Начните вводить систему координат";

    public final static String START = "/start";

    public final static String STOP = "/stop";

    public final static String INPUT_PROMPT = "Отправьте файл или текст с координатами";

    public final static String HELP = "Помощь 🔽";

    public final static String WRONG_FORMAT_MESSAGE = "Неизвестный формат отправленного сообщения";

    public final static String FATAL_ERROR = "Произошла непредвиденная ошибка. Обратитесь к администратору";

    public static final String EMPTY_SYMBOL = "🔳 ";

    public static final String CONFIRM_SYMBOL = "✅ ";

    public static final String APPROVE = "📝 Подтвердить";

    public static final String TRANSFORMATION_TYPE_CHOICE = "Выберите тип трансформации";

    public static final String COORDINATE_SYSTEM_TARGET_CHOICE = "Выберите систему координат результирующего файла";

    public static final String COORDINATE_SYSTEM_SRC_CHOICE = "Выберите систему координат исходного файла";

    public static final String BACK = " ⬆️️ Назад";

    public static final String FILE_FORMAT_CHOICE = "Выберите формат выходного файла";

    public static final String NO_SUCH_COORDINATE_SYSTEM = "Вы выбрали систему координат не из списка. Повторите выбор";

    public static final String INLINE_BUTTON_NAME = "Ввод";

    public static final String HELP_PROMPT =
            "Принимает: kml, kmz, csv, txt, dxf, текст\n" +
            "Разделитель: Name; North; East; (Elv)\n" +
            "Разделитель целой и дробной части: точка\n" +
            "Формат WGS: DD.DDDDD\n" +
            "Возврат в начало разговора: /start или /stop\n";
}
