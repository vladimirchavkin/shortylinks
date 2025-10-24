package ru.chavkin.em.linkshortener.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import ru.chavkin.em.linkshortener.entity.dto.ShortenRequest;
import ru.chavkin.em.linkshortener.entity.dto.ShortenResponse;
import ru.chavkin.em.linkshortener.exception.LinkNotFoundException;
import ru.chavkin.em.linkshortener.service.LinkService;


import static ru.chavkin.em.linkshortener.entity.enumerated.ExceptionMessage.LINK_NOT_FOUND;

@RestController
@RequestMapping("/api/v1/link")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> createShortUrl(@RequestBody ShortenRequest shortenRequest) {
        return ResponseEntity.ok(linkService.createShortLink(shortenRequest));
    }

    @GetMapping("/{code}")
    public RedirectView redirect(@PathVariable String code) {
        return linkService.findByAliasOrShortCode(code)
                .map(link -> new RedirectView(link.getOriginalUrl()))
                .orElseThrow(() -> new LinkNotFoundException(
                        LINK_NOT_FOUND.getErrorMessage(),
                        LINK_NOT_FOUND.getErrorCode()
                ));
    }

}
