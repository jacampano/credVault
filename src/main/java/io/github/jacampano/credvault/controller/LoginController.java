package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import io.github.jacampano.credvault.security.EffectiveAuthSettings;
import io.github.jacampano.credvault.security.OAuthProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final AuthSettingsService authSettingsService;

    public LoginController(AuthSettingsService authSettingsService) {
        this.authSettingsService = authSettingsService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();
        boolean oauthEnabled = settings.mode() == AuthMode.oauth;
        String oauthProviderName = settings.oauthProvider() == OAuthProvider.gitlab ? "GitLab" : "OAuth";
        model.addAttribute("oauthEnabled", oauthEnabled);
        model.addAttribute("oauthProviderName", oauthProviderName);
        return "auth/login";
    }
}
