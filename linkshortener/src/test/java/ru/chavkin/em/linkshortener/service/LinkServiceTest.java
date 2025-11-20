package ru.chavkin.em.linkshortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.chavkin.em.linkshortener.entity.LinkConfigurationProperties;
import ru.chavkin.em.linkshortener.entity.Link;
import ru.chavkin.em.linkshortener.entity.dto.ShortenRequest;
import ru.chavkin.em.linkshortener.entity.dto.ShortenResponse;
import ru.chavkin.em.linkshortener.entity.enumerated.ExceptionMessage;
import ru.chavkin.em.linkshortener.entity.mapper.LinkMapper;
import ru.chavkin.em.linkshortener.exception.AliasAlreadyExistException;
import ru.chavkin.em.linkshortener.exception.OriginalLinkValueException;
import ru.chavkin.em.linkshortener.exception.ShortCodeGenerationMaxAttemptsException;
import ru.chavkin.em.linkshortener.repository.LinkRepository;
import ru.chavkin.em.linkshortener.validator.LinkValidator;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для LinkService")
@MockitoSettings(strictness = Strictness.LENIENT)
@EnableConfigurationProperties(LinkConfigurationProperties.class)
class LinkServiceTest {

    @Mock
    private LinkConfigurationProperties props;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkValidator linkValidator;

    @Mock
    private LinkMapper linkMapper;

    @InjectMocks
    private LinkService linkService;

    private static final String VALID_URL = "https://example.com";
    private static final String ALIAS = "myalias";
    private static final String SHORT_CODE = "abc123";

    @Nested
    @DisplayName("Тесты метода createShortLink")
    class CreateShortLinkTests {

        @Test
        @DisplayName("Успешное создание ссылки без alias — используется shortCode")
        void createShortLink_WithoutAlias_ShouldUseShortCode() {
            // Given
            ShortenRequest request = new ShortenRequest(VALID_URL, null, 7);
            Link savedLink = Link.builder()
                    .id(1L)
                    .originalUrl(VALID_URL)
                    .shortCode(SHORT_CODE)
                    .alias(SHORT_CODE)
                    .expiresAt(OffsetDateTime.now().plusDays(7))
                    .build();

            ShortenResponse expectedResponse = new ShortenResponse(VALID_URL, SHORT_CODE);

            doNothing().when(linkValidator).validateUrl(VALID_URL);
            when(linkValidator.resolveTtlDays(7)).thenReturn(7);
            when(linkRepository.save(any(Link.class))).thenReturn(savedLink);
            when(linkMapper.fromEntityToResponse(savedLink)).thenReturn(expectedResponse);

            LinkService spyService = spy(linkService);
            doReturn(SHORT_CODE).when(spyService).generateShortCode();

            // When
            ShortenResponse response = spyService.createShortLink(request);

            // Then
            assertEquals(expectedResponse, response);
            verify(linkRepository).save(argThat(link ->
                    link.getOriginalUrl().equals(VALID_URL) &&
                            link.getShortCode().equals(SHORT_CODE) &&
                            link.getAlias().equals(SHORT_CODE)
            ));
        }

        @Test
        @DisplayName("Успешное создание ссылки с alias")
        void createShortLink_WithAlias_ShouldSaveWithAlias() {
            // Given
            ShortenRequest request = new ShortenRequest(VALID_URL, ALIAS, null);
            Link savedLink = Link.builder()
                    .id(1L)
                    .originalUrl(VALID_URL)
                    .shortCode(SHORT_CODE)
                    .alias(ALIAS)
                    .expiresAt(OffsetDateTime.now().plusDays(props.getDefaultTimeToLive()))
                    .build();

            ShortenResponse expectedResponse = new ShortenResponse(VALID_URL, ALIAS);

            doNothing().when(linkValidator).validateUrl(VALID_URL);
            when(linkValidator.resolveTtlDays(null)).thenReturn(props.getDefaultTimeToLive());
            when(linkRepository.save(any(Link.class))).thenReturn(savedLink);
            when(linkMapper.fromEntityToResponse(savedLink)).thenReturn(expectedResponse);

            LinkService spyService = spy(linkService);
            doReturn(SHORT_CODE).when(spyService).generateShortCode();

            // When
            ShortenResponse response = spyService.createShortLink(request);

            // Then
            assertEquals(expectedResponse, response);
            verify(linkRepository).save(argThat(link ->
                    link.getOriginalUrl().equals(VALID_URL) &&
                            link.getShortCode().equals(SHORT_CODE) &&
                            link.getAlias().equals(ALIAS)
            ));
        }

        @Test
        @DisplayName("Выбрасывает OriginalLinkValueException при null URL")
        void createShortLink_NullUrl_ShouldThrowOriginalLinkValueException() {
            // Given
            ShortenRequest request = new ShortenRequest(null, null, 3);

            doThrow(new OriginalLinkValueException(
                    ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorMessage(),
                    ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorCode()
            )).when(linkValidator).validateUrl(null);

            // When & Then
            OriginalLinkValueException exception = assertThrows(
                    OriginalLinkValueException.class,
                    () -> linkService.createShortLink(request)
            );

            assertEquals(ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("Выбрасывает AliasAlreadyExistException при существующем alias")
        void createShortLink_ExistingAlias_ShouldThrowAliasAlreadyExistException() {
            // Given
            ShortenRequest request = new ShortenRequest(VALID_URL, ALIAS, 5);

            doNothing().when(linkValidator).validateUrl(VALID_URL);
            doThrow(new AliasAlreadyExistException(
                    ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorMessage(),
                    ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorCode()
            )).when(linkValidator).checkAndThrowIfAliasExists(ALIAS);

            // When & Then
            AliasAlreadyExistException exception = assertThrows(
                    AliasAlreadyExistException.class,
                    () -> linkService.createShortLink(request)
            );

            assertEquals(ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorMessage(), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Тесты метода getByAliasOrShortCode")
    class GetByAliasOrShortCodeTests {

        private Link createActiveLink() {
            return Link.builder()
                    .id(1L)
                    .originalUrl(VALID_URL)
                    .shortCode(SHORT_CODE)
                    .alias(ALIAS)
                    .expiresAt(OffsetDateTime.now().plusDays(1))
                    .build();
        }

        private Link createExpiredLink() {
            return Link.builder()
                    .id(1L)
                    .originalUrl(VALID_URL)
                    .shortCode(SHORT_CODE)
                    .alias(ALIAS)
                    .expiresAt(OffsetDateTime.now().minusDays(1))
                    .build();
        }

        @Test
        @DisplayName("Находит активную ссылку по alias")
        void findByAliasOrShortCode_ByAlias_ShouldReturnLink() {
            // Given
            Link link = createActiveLink();
            when(linkRepository.findByAlias(ALIAS)).thenReturn(Optional.of(link));

            // When
            String result = linkService.getUrlByAliasOrShortCode(ALIAS);

            // Then
            assertFalse(result.isEmpty());
            assertEquals(ALIAS, result);
        }

        @Test
        @DisplayName("Находит активную ссылку по shortCode, если alias не найден")
        void findByAliasOrShortCode_ByShortCode_ShouldReturnLink() {
            // Given
            Link link = createActiveLink();
            when(linkRepository.findByAlias(SHORT_CODE)).thenReturn(Optional.empty());
            when(linkRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(link));

            // When
            String result = linkService.getUrlByAliasOrShortCode(SHORT_CODE);

            // Then
            assertFalse(result.isEmpty());
            assertEquals(SHORT_CODE, result);
        }

        @Test
        @DisplayName("Возвращает empty, если ссылка истекла")
        void findByAliasOrShortCode_ExpiredLink_ShouldReturnEmpty() {
            // Given
            Link expiredLink = createExpiredLink();
            when(linkRepository.findByAlias(ALIAS)).thenReturn(Optional.of(expiredLink));

            // When
            String result = linkService.getUrlByAliasOrShortCode(ALIAS);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Возвращает empty, если ссылка не найдена")
        void findByAliasOrShortCode_NotFound_ShouldReturnEmpty() {
            // Given
            when(linkRepository.findByAlias("unknown")).thenReturn(Optional.empty());
            when(linkRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

            // When
            String result = linkService.getUrlByAliasOrShortCode("unknown");

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты метода generateShortCode")
    class GenerateShortCodeTests {

        @Test
        @DisplayName("Генерирует shortCode после нескольких коллизий")
        void generateShortCode_WithCollisions_ShouldReturnUniqueCode() {
            // Given
            when(linkRepository.existsByShortCode("code1")).thenReturn(true);
            when(linkRepository.existsByShortCode("code2")).thenReturn(true);
            when(linkRepository.existsByShortCode("code3")).thenReturn(false);

            LinkService spyService = spy(linkService);
            doReturn("code1", "code2", "code3").when(spyService).generateRandomCode();

            // When
            String result = spyService.generateShortCode();

            // Then
            assertEquals("code3", result);
            verify(spyService, times(3)).generateRandomCode();
        }

        @Test
        @DisplayName("Выбрасывает исключение после исчерпания попыток")
        void generateShortCode_MaxAttempts_ShouldThrowException() {
            // Given
            when(linkRepository.existsByShortCode(anyString())).thenReturn(true);

            LinkService spyService = spy(linkService);
            doReturn("code").when(spyService).generateRandomCode();

            // When & Then
            ShortCodeGenerationMaxAttemptsException exception = assertThrows(
                    ShortCodeGenerationMaxAttemptsException.class,
                    spyService::generateShortCode
            );

            assertEquals(
                    ExceptionMessage.SHORT_CODE_GENERATION_MAX_ATTEMPTS.getErrorMessage(),
                    exception.getMessage()
            );
        }
    }

    @Nested
    @DisplayName("Тесты метода generateRandomCode (через рефлексию или spy)")
    class GenerateRandomCodeTests {

        @Test
        @DisplayName("Генерирует код длиной 6 символов из разрешённых")
        void generateRandomCode_ShouldReturnValidLengthAndChars() {
            // Given
            LinkService spyService = spy(linkService);

            // When
            String code = spyService.generateRandomCode();

            // Then
            assertEquals(props.getLength(), code.length());
            assertTrue(code.chars().allMatch(ch ->
                    props.getShortCodeAllowedCharacters().indexOf(ch) != -1
            ));
        }
    }
}