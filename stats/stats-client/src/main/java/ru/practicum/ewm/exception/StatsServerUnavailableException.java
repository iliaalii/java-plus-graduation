package ru.practicum.ewm.exception;

public class StatsServerUnavailableException extends RuntimeException {
    public StatsServerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatsServerUnavailableException(String message) {
        super(message);
    }
}
