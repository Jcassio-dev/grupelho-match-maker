package com.grupelho.riot;

public class RiotApiException extends RuntimeException {

    public RiotApiException(String message) {
        super(message);
    }

    public RiotApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
