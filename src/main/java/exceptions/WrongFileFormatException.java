package exceptions;

public class WrongFileFormatException extends Exception {
    public WrongFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongFileFormatException(String message) {
        super(message);
    }
}
