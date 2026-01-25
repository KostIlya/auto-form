package ru.bell.auto_form.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.bell.auto_form.service.TokenService;


@Controller
@RequestMapping(path = "auth")
@Slf4j
public class YandexOAuthController {
    private final String URI_BASE = "https://oauth.yandex.ru";
    private final TokenService tokenService;

    @Autowired
    public YandexOAuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping(path = "start")
    public String start() {
        return "redirect:" + tokenService.getUriToCodeRequest();
    }

    @GetMapping(path = "back")
    public String back(@RequestParam("code") String code) {
        log.debug("back: code: {}", code);
        tokenService.getTokenByCode(code);
        return "back(): success.";
    }
}
