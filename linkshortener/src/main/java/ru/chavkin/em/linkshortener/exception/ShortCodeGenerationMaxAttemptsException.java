package ru.chavkin.em.linkshortener.exception;

import lombok.Getter;

@Getter
public class ShortCodeGenerationMaxAttemptsException extends RuntimeException {

    private final String errorCode;

    public ShortCodeGenerationMaxAttemptsException(final String message, final String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
