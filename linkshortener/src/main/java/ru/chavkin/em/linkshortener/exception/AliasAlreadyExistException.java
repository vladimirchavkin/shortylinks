package ru.chavkin.em.linkshortener.exception;

import lombok.Getter;

@Getter
public class AliasAlreadyExistException extends RuntimeException {

    private final String errorCode;

    public AliasAlreadyExistException(final String message, final String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
