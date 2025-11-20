CREATE TABLE links
(
    id           BIGSERIAL PRIMARY KEY,
    original_url TEXT        NOT NULL,
    short_code   VARCHAR(10) NOT NULL UNIQUE,
    alias        VARCHAR(50) UNIQUE,
    expires_at   TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL
);