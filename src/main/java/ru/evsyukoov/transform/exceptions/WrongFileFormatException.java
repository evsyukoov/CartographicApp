package ru.evsyukoov.transform.exceptions;

public class WrongFileFormatException extends RuntimeException {
    public WrongFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongFileFormatException(String message) {
        super(message);
    }
}
