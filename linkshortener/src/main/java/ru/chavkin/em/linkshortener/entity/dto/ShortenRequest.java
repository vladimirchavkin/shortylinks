package ru.chavkin.em.linkshortener.entity.dto;

public record ShortenRequest (
        String originalUrl,
        String alias,
        Integer ttlDays
){
}
