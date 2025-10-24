package ru.chavkin.em.linkshortener.exception;

import lombok.Getter;

@Getter
public class LinkNotFoundException extends RuntimeException {

    private final String errorCode;

    public LinkNotFoundException(final String message, final String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
