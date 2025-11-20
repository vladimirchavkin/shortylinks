package ru.chavkin.em.linkshortener.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import ru.chavkin.em.linkshortener.entity.dto.ShortenRequest;
import ru.chavkin.em.linkshortener.entity.dto.ShortenResponse;
import ru.chavkin.em.linkshortener.service.LinkService;

@RestController
@RequestMapping("/api/v1/link")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping("/shorten")
    public ShortenResponse createShortUrl(@RequestBody ShortenRequest shortenRequest) {
        return linkService.createShortLink(shortenRequest);
    }

    @GetMapping("/{code}")
    public RedirectView redirect(@PathVariable String code) {
        String url = linkService.getUrlByAliasOrShortCode(code);
        return new RedirectView(url);
    }

}
