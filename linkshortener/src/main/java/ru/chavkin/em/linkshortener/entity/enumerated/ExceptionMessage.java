package ru.chavkin.em.linkshortener.entity.enumerated;

import lombok.Getter;

@Getter
public enum ExceptionMessage {

    ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY("Original link value is null or empty", "400"),

    ALIAS_LINK_VALUE_IS_NULL_OR_EMPTY("Alias link value is null or empty", "400"),
    ALIAS_IS_ALREADY_EXISTS("Alias already exists", "400"),

    TTL_DAYS_IS_NULL_OR_LESS_ZERO("TTL days is null or less zero", "400"),

    SHORT_CODE_GENERATION_MAX_ATTEMPTS("The maximum number of attempts for code generation has been reached.", "400"),

    LINK_NOT_FOUND("Link not found", "404"),
    LINK_WITH_CONSTRAINT_FIELD_ALREADY_EXISTS("Link with constraint field already exists", "409"),
    LINK_EXPIRED("Link expired", "409");

    private final String errorMessage;
    private final String errorCode;

    ExceptionMessage(String errorMessage, String errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }
}
