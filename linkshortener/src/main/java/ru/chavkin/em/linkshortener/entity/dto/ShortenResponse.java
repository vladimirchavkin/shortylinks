package ru.chavkin.em.linkshortener.entity.dto;

public record ShortenResponse (
        String originalUrl,
        String alias
){
}
