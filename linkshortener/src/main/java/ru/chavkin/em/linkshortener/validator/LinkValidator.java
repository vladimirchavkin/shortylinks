package ru.chavkin.em.linkshortener.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.chavkin.em.linkshortener.entity.LinkConfigurationProperties;
import ru.chavkin.em.linkshortener.entity.enumerated.ExceptionMessage;
import ru.chavkin.em.linkshortener.exception.AliasAlreadyExistException;
import ru.chavkin.em.linkshortener.exception.OriginalLinkValueException;
import ru.chavkin.em.linkshortener.repository.LinkRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({LinkConfigurationProperties.class})
public class LinkValidator {

    private final LinkRepository linkRepository;
    private final LinkConfigurationProperties props;

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
     * or default value ({@link LinkConfigurationProperties}).
     */
    public Integer resolveTtlDays(Integer ttlDays) {
        if (ttlDays == null || ttlDays < 0) {
            log.debug("Invalid TTL, using default: {}", props.getDefaultTimeToLive());
            return props.getDefaultTimeToLive();
        }
        return ttlDays;
    }

    /**
     * Method checks if alias exists and throws {@link AliasAlreadyExistException}.
     *
     * @param alias presented alias.
     */
    public void checkAndThrowIfAliasExists(String alias) {
        if (linkRepository.existsByAlias(alias)) {
            throw new AliasAlreadyExistException(
                    ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorMessage(),
                    ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorCode()
            );
        }
    }

}
