package ru.chavkin.em.linkshortener.exception;

import lombok.Getter;

@Getter
public class OriginalLinkValueException extends RuntimeException {

    private final String errorCode;

    public OriginalLinkValueException(final String message, final String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
