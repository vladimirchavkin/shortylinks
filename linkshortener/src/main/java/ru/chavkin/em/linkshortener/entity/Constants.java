package ru.chavkin.em.linkshortener.entity;

public class Constants {
    public static final int SHORT_CODE_LENGTH = 6;
    public static final int SHORT_CODE_MAX_GENERATION_ATTEMPTS = 16;
    public static final String SHORT_CODE_ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static final Integer SHORT_CODE_MAX_LENGTH = SHORT_CODE_ALLOWED_CHARACTERS.length();
    public static final Integer DEFAULT_TIME_TO_LIVE_VALUE = 3;
}
