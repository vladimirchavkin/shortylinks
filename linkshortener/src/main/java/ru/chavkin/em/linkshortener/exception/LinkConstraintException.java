package ru.chavkin.em.linkshortener.exception;

import lombok.Getter;

@Getter
public class LinkConstraintException extends RuntimeException {

    private final String errorCode;

    public LinkConstraintException(final String message, final String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
