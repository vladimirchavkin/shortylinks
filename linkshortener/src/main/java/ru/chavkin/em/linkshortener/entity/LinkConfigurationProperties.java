package ru.chavkin.em.linkshortener.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.link.shortcode")
@EnableConfigurationProperties(LinkConfigurationProperties.class)
public class LinkConfigurationProperties {

    @NotNull
    int length;

    @NotNull
    int maxGenerationAttempts;

    @NotNull
    String shortCodeAllowedCharacters;

    @NotNull
    int defaultTimeToLive;
}
