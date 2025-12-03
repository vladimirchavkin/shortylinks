package ru.chavkin.em.linkshortener.entity;

public record ErrorResponse(
        String errorMessage,
        String errorCode
) {
}
