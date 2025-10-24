package ru.chavkin.em.linkshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import ru.chavkin.em.linkshortener.entity.ErrorResponse;
import ru.chavkin.em.linkshortener.entity.Link;
import ru.chavkin.em.linkshortener.entity.dto.ShortenRequest;
import ru.chavkin.em.linkshortener.entity.dto.ShortenResponse;
import ru.chavkin.em.linkshortener.entity.enumerated.ExceptionMessage;
import ru.chavkin.em.linkshortener.repository.LinkRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@SqlGroup({
        @Sql(value = "classpath:sql/clear-links.sql", executionPhase = BEFORE_TEST_METHOD),
        @Sql(value = "classpath:sql/clear-links.sql", executionPhase = AFTER_TEST_METHOD)
})
@DisplayName("Интеграционные тесты LinkController")
class LinkControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LinkRepository linkRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/link";

        restTemplate.getRestTemplate().setRequestFactory(
                new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                        super.prepareConnection(connection, httpMethod);
                        connection.setInstanceFollowRedirects(false); // КЛЮЧЕВОЕ!
                    }
                }
        );
    }

    @Nested
    @DisplayName("POST /api/v1/link/shorten")
    class ShortenEndpointTests {

        @Test
        @DisplayName("Успешно создаёт короткую ссылку без alias")
        void shorten_WithoutAlias_ShouldReturnShortCodeAsAlias() {
            // Given
            ShortenRequest request = new ShortenRequest("http://example.com", null, 7);
            HttpEntity<ShortenRequest> entity = new HttpEntity<>(request, contentHeaders());

            // When
            ResponseEntity<ShortenResponse> response = restTemplate.postForEntity(
                    baseUrl + "/shorten", entity, ShortenResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            ShortenResponse body = response.getBody();
            assertNotNull(body, "Response body should not be null");
            assertEquals("http://example.com", body.originalUrl());

            String alias = body.alias();
            assertNotNull(alias);
            assertEquals(6, alias.length());

            // Проверка в БД
            Link savedLink = linkRepository.findByAlias(alias).orElse(null);
            assertNotNull(savedLink);
            assertEquals("http://example.com", savedLink.getOriginalUrl());
            assertEquals(alias, savedLink.getShortCode());
            assertEquals(alias, savedLink.getAlias()); // shortCode == alias
            assertTrue(savedLink.getExpiresAt().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Успешно создаёт ссылку с кастомным alias")
        void shorten_WithCustomAlias_ShouldReturnAlias() {
            // Given
            ShortenRequest request = new ShortenRequest("http://google.com", "google", 5);
            HttpEntity<ShortenRequest> entity = new HttpEntity<>(request, contentHeaders());

            // When
            ResponseEntity<ShortenResponse> response = restTemplate.postForEntity(
                    baseUrl + "/shorten", entity, ShortenResponse.class);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            ShortenResponse body = response.getBody();
            assertNotNull(body);
            assertEquals("http://google.com", body.originalUrl());
            assertEquals("google", body.alias());

            Link link = linkRepository.findByAlias("google").orElse(null);
            assertNotNull(link);
            assertEquals("google", link.getAlias());

            assertNotEquals("google", link.getShortCode());
        }

        @Test
        @DisplayName("Возвращает 400 при дублировании alias")
        void shorten_DuplicateAlias_ShouldReturn400() {
            // Given
            ShortenRequest first = new ShortenRequest("http://a.com", "dup", null);
            createShortLink(first);

            // When
            ShortenRequest second = new ShortenRequest("http://b.com", "dup", null);
            HttpEntity<ShortenRequest> entity = new HttpEntity<>(second, contentHeaders());

            // Then
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    baseUrl + "/shorten", HttpMethod.POST, entity, ErrorResponse.class);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorMessage(), body.errorMessage());
            assertEquals(ExceptionMessage.ALIAS_IS_ALREADY_EXISTS.getErrorCode(), body.errorCode());
        }

        @Test
        @DisplayName("Возвращает 400 при пуст970ом URL")
        void shorten_EmptyUrl_ShouldReturn400() {
            // Given
            ShortenRequest request = new ShortenRequest("", null, 1);
            HttpEntity<ShortenRequest> entity = new HttpEntity<>(request, contentHeaders());

            // When
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    baseUrl + "/shorten", HttpMethod.POST, entity, ErrorResponse.class);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorMessage(), body.errorMessage());
            assertEquals(ExceptionMessage.ORIGINAL_LINK_VALUE_IS_NULL_OR_EMPTY.getErrorCode(), body.errorCode());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/link/{code} — редирект")
    class RedirectEndpointTests {

        @Test
        @DisplayName("Успешный редирект по alias")
        void redirect_ByAlias_ShouldRedirectToOriginalUrl() {
            createShortLink(new ShortenRequest("http://example.com", "testalias", 1));

            ResponseEntity<Void> response = restTemplate.getForEntity(
                    baseUrl + "/testalias", Void.class);

            assertEquals(HttpStatus.FOUND, response.getStatusCode());
            assertEquals("http://example.com", response.getHeaders().getLocation().toString());
        }

        @Test
        @DisplayName("Успешный редирект по shortCode (если alias не найден)")
        void redirect_ByShortCode_ShouldRedirect() {
            // Given – без alias
            ShortenRequest request = new ShortenRequest("http://shortcode-test.com", null, 1);
            ShortenResponse created = createShortLink(request);
            String shortCode = created.alias(); // = shortCode

            // When
            ResponseEntity<Void> response = restTemplate.getForEntity(
                    baseUrl + "/" + shortCode, Void.class);

            // Then
            assertEquals(HttpStatus.FOUND, response.getStatusCode());
            assertEquals("http://shortcode-test.com", response.getHeaders().getLocation().toString());
        }

        @Test
        @DisplayName("Возвращает 404 при несуществующем коде")
        void redirect_NotFound_ShouldReturn404() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    baseUrl + "/nonexistent",
                    HttpMethod.GET,
                    null,
                    ErrorResponse.class
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(ExceptionMessage.LINK_NOT_FOUND.getErrorMessage(), body.errorMessage());
            assertEquals(ExceptionMessage.LINK_NOT_FOUND.getErrorCode(), body.errorCode());
        }

    }

    private HttpHeaders contentHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ShortenResponse createShortLink(ShortenRequest request) {
        HttpEntity<ShortenRequest> entity = new HttpEntity<>(request, contentHeaders());
        ResponseEntity<ShortenResponse> resp = restTemplate.postForEntity(
                baseUrl + "/shorten", entity, ShortenResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "Failed to create short link: " + resp.getStatusCode());
        assertNotNull(resp.getBody(), "Response body is null after creating link");
        return resp.getBody();
    }
}