package ru.chavkin.em.linkshortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chavkin.em.linkshortener.entity.Link;
import ru.chavkin.em.linkshortener.entity.dto.ShortenRequest;
import ru.chavkin.em.linkshortener.entity.dto.ShortenResponse;
import ru.chavkin.em.linkshortener.entity.enumerated.Constants;
import ru.chavkin.em.linkshortener.entity.enumerated.ExceptionMessage;
import ru.chavkin.em.linkshortener.entity.mapper.LinkMapper;
import ru.chavkin.em.linkshortener.exception.LinkConstraintException;
import ru.chavkin.em.linkshortener.exception.LinkExpirationDateValueException;
import ru.chavkin.em.linkshortener.exception.LinkNotFoundException;
import ru.chavkin.em.linkshortener.exception.ShortCodeGenerationMaxAttemptsException;
import ru.chavkin.em.linkshortener.repository.LinkRepository;
import ru.chavkin.em.linkshortener.validator.LinkValidator;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты LinkService")
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkValidator linkValidator;

    @Mock
    private LinkMapper linkMapper;

    @InjectMocks
    private LinkService linkService;

    private static final String TEST_URL = "https://www.effective-mobile.ru/";
    private static final String GENERATED_SHORT_CODE = "AbCd12";

    @Nested
    @DisplayName("Создание короткой ссылки")
    class CreateShortLink {

        @Test
        @DisplayName("Успешное создание без alias → alias = shortCode")
        void createShortLink_WithoutAlias_ShouldUseShortCodeAsAlias() {
            // Arrange
            ShortenRequest request = new ShortenRequest(TEST_URL, null, 7);
            Link savedLink = Link.builder()
                    .id(1L)
                    .originalUrl(TEST_URL)
                    .shortCode(GENERATED_SHORT_CODE)
                    .alias(GENERATED_SHORT_CODE)
                    .createdAt(OffsetDateTime.now())
                    .expiresAt(OffsetDateTime.now().plusDays(7))
                    .build();

            ShortenResponse expectedResponse = new ShortenResponse(TEST_URL, GENERATED_SHORT_CODE);

            doNothing().when(linkValidator).validateUrl(TEST_URL);
            when(linkValidator.resolveTtlDays(7)).thenReturn(7);

            when(linkRepository.existsByShortCode(any())).thenReturn(false);
            when(linkRepository.save(any(Link.class))).thenReturn(savedLink);
            when(linkMapper.fromEntityToResponse(savedLink)).thenReturn(expectedResponse);

            // Act
            ShortenResponse response = linkService.createShortLink(request);

            // Assert
            assertThat(response)
                    .isNotNull()
                    .extracting(ShortenResponse::originalUrl, ShortenResponse::alias)
                    .containsExactly(TEST_URL, GENERATED_SHORT_CODE);
        }

        @Test
        @DisplayName("Успешное создание с кастомным alias")
        void createShortLink_WithCustomAlias_ShouldSaveWithAlias() {
            // Arrange
            String customAlias = "my-link";

            ShortenRequest request = new ShortenRequest(TEST_URL, customAlias, null);
            Link savedLink = Link.builder()
                    .id(2L)
                    .originalUrl(TEST_URL)
                    .shortCode(GENERATED_SHORT_CODE)
                    .alias(customAlias)
                    .createdAt(OffsetDateTime.now())
                    .expiresAt(OffsetDateTime.now().plusDays(Constants.DEFAULT_TIME_TO_LIVE_VALUE))
                    .build();

            ShortenResponse expectedResponse = new ShortenResponse(TEST_URL, customAlias);

            doNothing().when(linkValidator).validateUrl(TEST_URL);
            when(linkValidator.resolveTtlDays(null)).thenReturn(Constants.DEFAULT_TIME_TO_LIVE_VALUE);

            when(linkRepository.existsByShortCode(any())).thenReturn(false);
            when(linkRepository.save(any(Link.class))).thenReturn(savedLink);
            when(linkMapper.fromEntityToResponse(savedLink)).thenReturn(expectedResponse);

            // Act
            ShortenResponse response = linkService.createShortLink(request);

            // Assert
            assertThat(response.alias()).isEqualTo(customAlias);
            verify(linkRepository).save(argThat(link -> link.getAlias().equals(customAlias)));
        }

        @Test
        @DisplayName("Используется дефолтный TTL = 3 дня, если передан null или < 0")
        void createShortLink_InvalidTtl_ShouldUseDefaultTtl() {
            // Arrange
            ShortenRequest request = new ShortenRequest(TEST_URL, "test", -5);

            doNothing().when(linkValidator).validateUrl(TEST_URL);
            when(linkValidator.resolveTtlDays(-5)).thenReturn(Constants.DEFAULT_TIME_TO_LIVE_VALUE);

            when(linkRepository.existsByShortCode(any())).thenReturn(false);
            when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            linkService.createShortLink(request);

            // Assert
            verify(linkRepository).save(argThat(link ->
                    link.getExpiresAt().isAfter(OffsetDateTime.now().plusDays(2)) &&
                            link.getExpiresAt().isBefore(OffsetDateTime.now().plusDays(4))
            ));
        }

        @Test
        @DisplayName("Создание ссылки с уже существующим alias → LinkConstraintException")
        void createShortLink_DuplicateAlias_ShouldThrowLinkConstraintException() {
            // Arrange
            String duplicateAlias = "my-custom-alias";
            String generatedShortCode = "XyZ789";

            ShortenRequest request = new ShortenRequest(TEST_URL, duplicateAlias, 10);

            // Act
            LinkService spyService = spy(linkService);
            doReturn(generatedShortCode).when(spyService).generateRandomCode();

            doNothing().when(linkValidator).validateUrl(TEST_URL);
            when(linkValidator.resolveTtlDays(10)).thenReturn(10);
            when(linkRepository.existsByShortCode(generatedShortCode)).thenReturn(false);

            when(linkRepository.save(any(Link.class)))
                    .thenThrow(new org.hibernate.exception.ConstraintViolationException(
                            "Duplicate entry 'my-custom-alias' for key 'links.alias'",
                            null,
                            "links_alias_key"
                    ));

            // Assert exception message
            assertThatThrownBy(() -> spyService.createShortLink(request))
                    .isInstanceOf(LinkConstraintException.class)
                    .hasMessageContaining(ExceptionMessage.LINK_WITH_CONSTRAINT_FIELD_ALREADY_EXISTS.getErrorMessage());

            // Assert save
            ArgumentCaptor<Link> captor = ArgumentCaptor.forClass(Link.class);
            verify(linkRepository).save(captor.capture());

            Link capturedLink = captor.getValue();
            assertThat(capturedLink.getOriginalUrl()).isEqualTo(TEST_URL);
            assertThat(capturedLink.getShortCode()).isEqualTo(generatedShortCode);
            assertThat(capturedLink.getAlias()).isEqualTo(duplicateAlias);
            assertThat(capturedLink.getExpiresAt())
                    .isCloseTo(OffsetDateTime.now().plusDays(10), within(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("Генерация shortCode")
    class ShortCodeGeneration {

        @Test
        @DisplayName("Генерация уникального shortCode — успех с первой попытки")
        void generateShortCode_FirstAttemptSuccess() {
            // Act
            when(linkRepository.existsByShortCode(GENERATED_SHORT_CODE)).thenReturn(false);

            LinkService spyService = spy(linkService);
            doReturn(GENERATED_SHORT_CODE).when(spyService).generateRandomCode();

            String result = spyService.generateShortCode();

            // Assert
            assertThat(result).isEqualTo(GENERATED_SHORT_CODE);
            verify(linkRepository, times(1)).existsByShortCode(GENERATED_SHORT_CODE);
        }

        @Test
        @DisplayName("Коллизия shortCode — генерация с нескольких попыток")
        void generateShortCode_WithCollision_ShouldRetryAndSucceed() {
            // Act
            LinkService spyService = spy(linkService);

            doReturn("AAAAAA", "BBBBBB", "AbCd12")
                    .when(spyService).generateRandomCode();
            
            when(linkRepository.existsByShortCode("AAAAAA")).thenReturn(true);
            when(linkRepository.existsByShortCode("BBBBBB")).thenReturn(true);
            when(linkRepository.existsByShortCode("AbCd12")).thenReturn(false);

            String result = spyService.generateShortCode();

            // Assert
            assertThat(result).isEqualTo("AbCd12");
            verify(linkRepository, times(3)).existsByShortCode(any());
        }

        @Test
        @DisplayName("Исчерпаны все попытки генерации — исключение")
        void generateShortCode_AllAttemptsFailed_ShouldThrowException() {
            // Act
            LinkService spyService = spy(linkService);
            doReturn("CODE01").when(spyService).generateRandomCode();
            when(linkRepository.existsByShortCode("CODE01")).thenReturn(true);

            // Assert
            assertThatThrownBy(spyService::generateShortCode)
                    .isInstanceOf(ShortCodeGenerationMaxAttemptsException.class)
                    .hasMessageContaining("The maximum number of attempts for code generation has been reached.");
        }
    }

    @Nested
    @DisplayName("Получение оригинальной ссылки по коду")
    class GetOriginalUrl {

        private Link createActiveLink(String shortCode, String alias) {
            return Link.builder()
                    .originalUrl(TEST_URL)
                    .shortCode(shortCode)
                    .alias(alias)
                    .expiresAt(OffsetDateTime.now().plusDays(1))
                    .build();
        }

        private Link createExpiredLink() {
            return Link.builder()
                    .originalUrl(TEST_URL)
                    .shortCode("exp123")
                    .alias("expired")
                    .expiresAt(OffsetDateTime.now().minusHours(1))
                    .build();
        }

        @Test
        @DisplayName("Поиск по alias — успех")
        void getUrlByAliasOrShortCode_ByAlias_Success() {
            // Arrange
            Link link = createActiveLink("xyz789", "myalias");
            
            // Act
            when(linkRepository.findByAliasOrShortCode("myalias")).thenReturn(Optional.of(link));

            String result = linkService.getUrlByAliasOrShortCode("myalias");

            // Assert
            assertThat(result).isEqualTo(TEST_URL);
        }

        @Test
        @DisplayName("Поиск по shortCode — успех (alias не найден)")
        void getUrlByAliasOrShortCode_ByShortCode_Success() {
            // Arrange
            Link link = createActiveLink("short99", null);
            
            // Act
            when(linkRepository.findByAliasOrShortCode("short99")).thenReturn(Optional.of(link));

            String result = linkService.getUrlByAliasOrShortCode("short99");

            // Assert
            assertThat(result).isEqualTo(TEST_URL);
        }

        @Test
        @DisplayName("Ссылка истекла — бросается LinkExpirationDateValueException")
        void getUrlByAliasOrShortCode_LinkExpired_ThrowsException() {
            // Arrange
            Link expiredLink = createExpiredLink();

            // Act
            when(linkRepository.findByAliasOrShortCode("expired")).thenReturn(Optional.of(expiredLink));

            // Assert
            assertThatThrownBy(() -> linkService.getUrlByAliasOrShortCode("expired"))
                    .isInstanceOf(LinkExpirationDateValueException.class)
                    .hasMessageContaining("Link expired");
        }

        @Test
        @DisplayName("Ссылка не найдена — бросается LinkNotFoundException")
        void getUrlByAliasOrShortCode_NotFound_ThrowsException() {
            // Act
            when(linkRepository.findByAliasOrShortCode("unknown")).thenReturn(Optional.empty());

            // Assert
            assertThatThrownBy(() -> linkService.getUrlByAliasOrShortCode("unknown"))
                    .isInstanceOf(LinkNotFoundException.class)
                    .hasMessageContaining("Link not found");
        }
    }
}