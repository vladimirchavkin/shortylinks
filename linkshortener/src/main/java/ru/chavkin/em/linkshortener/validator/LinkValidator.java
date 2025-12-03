package ru.chavkin.em.linkshortener.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.chavkin.em.linkshortener.entity.enumerated.ExceptionMessage;
import ru.chavkin.em.linkshortener.exception.OriginalLinkValueException;

import static ru.chavkin.em.linkshortener.entity.enumerated.Constants.DEFAULT_TIME_TO_LIVE_VALUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkValidator {

    /**
     * Method to validate originalUrl.
     *
     * @param originalUrl original url.
     */
    public void validateUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new OriginalLinkValueException(
                    ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorMessage(),
                    ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorCode()
            );
        }
    }

    /**
     * Method to resolve Time To Live value.
     *
     * @param ttlDays presented ttlDays.
     * @return presented ttlDays(if validation successfully)
     */
    public Integer resolveTtlDays(Integer ttlDays) {
        if (ttlDays == null || ttlDays < 0) {
            log.debug("Invalid TTL, using default: {}", DEFAULT_TIME_TO_LIVE_VALUE);
            return DEFAULT_TIME_TO_LIVE_VALUE;
        }
        return ttlDays;
    }

}
