package ru.bell.auto_form.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.bell.auto_form.exception.CriticalException;
import ru.bell.auto_form.service.TokenService;


@Controller
@RequestMapping(path = "auth")
@Slf4j
public class YandexOAuthController {
    private final String URI_BASE = "https://oauth.yandex.ru";
    private final TokenService tokenService;
    @Autowired
    private ApplicationContext context;

    @Autowired
    public YandexOAuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping(path = "start")
    public String start() {
        return "redirect:" + tokenService.getUriToCodeRequest();
    }

    @GetMapping(path = "back")
    @ResponseBody
    public String back(@RequestParam("code") String code) {
        log.debug("back: code: {}", code);
        try {
            tokenService.getTokenByCode(code);
        } catch (CriticalException e) {
            log.error("work(): ", e);
            int exitCode = SpringApplication.exit(context, () -> 1);
            System.exit(exitCode);
        }
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Успех</title>
                </head>
                <body>
                    <h1>Токен получен!</h1>
                </body>
                </html>
                """;
    }
}
