package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.dto.admin.AuthenticationSettingsForm;
import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import io.github.jacampano.credvault.security.OAuthProvider;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AuthSettingsService authSettingsService;

    public AdminController(AuthSettingsService authSettingsService) {
        this.authSettingsService = authSettingsService;
    }

    @GetMapping
    public String adminHome() {
        return "admin/index";
    }

    @GetMapping("/authentication")
    public String authenticationForm(Model model) {
        model.addAttribute("form", authSettingsService.loadForm());
        model.addAttribute("envOverrides", authSettingsService.findActiveEnvironmentOverrides());
        return "admin/authentication";
    }

    @PostMapping("/authentication")
    public String saveAuthentication(@Valid @ModelAttribute("form") AuthenticationSettingsForm form,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        validateOauthFields(form, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("envOverrides", authSettingsService.findActiveEnvironmentOverrides());
            return "admin/authentication";
        }

        authSettingsService.saveForm(form);
        redirectAttributes.addFlashAttribute("message", "Configuración de autenticación guardada. Reinicia la aplicación para aplicar cambios de modo/proveedor.");
        return "redirect:/admin/authentication";
    }

    private void validateOauthFields(AuthenticationSettingsForm form, BindingResult bindingResult) {
        if (form.getMode() != AuthMode.oauth) {
            return;
        }
        Map<String, String> envOverrides = authSettingsService.findActiveEnvironmentOverrides();

        Map<String, String> required = Map.of(
                "oauthClientId", "Client ID",
                "oauthClientSecret", "Client Secret",
                "oauthClientAuthenticationMethod", "Método de autenticación de cliente",
                "oauthUserNameAttribute", "Atributo de usuario",
                "oauthRedirectUri", "URL de retorno",
                "oauthScopes", "Scopes"
        );

        required.forEach((field, label) -> {
            Object value = bindingResult.getFieldValue(field);
            boolean empty = value == null || value.toString().isBlank();
            if (empty && !envOverrides.containsKey(toEnvKey(field))) {
                bindingResult.addError(new FieldError("form", field,
                        label + " es obligatorio si no se define por variable de entorno"));
            }
        });

        if (form.getOauthProvider() == OAuthProvider.generic) {
            Map<String, String> genericRequired = Map.of(
                    "oauthAuthorizationUri", "Authorization URI",
                    "oauthTokenUri", "Token URI",
                    "oauthUserInfoUri", "User Info URI"
            );
            genericRequired.forEach((field, label) -> {
                Object value = bindingResult.getFieldValue(field);
                boolean empty = value == null || value.toString().isBlank();
                if (empty && !envOverrides.containsKey(toEnvKey(field))) {
                    bindingResult.addError(new FieldError("form", field,
                            label + " es obligatorio si el proveedor OAuth es genérico y no se define por variable de entorno"));
                }
            });
        }

        if (form.getOauthProvider() == OAuthProvider.gitlab) {
            Object value = bindingResult.getFieldValue("oauthGitlabBaseUrl");
            boolean empty = value == null || value.toString().isBlank();
            if (empty && !envOverrides.containsKey(toEnvKey("oauthGitlabBaseUrl"))) {
                bindingResult.addError(new FieldError("form", "oauthGitlabBaseUrl",
                        "Gitlab Base URL es obligatorio si el proveedor OAuth es GitLab y no se define por variable de entorno"));
            }
        }
    }

    private String toEnvKey(String field) {
        return switch (field) {
            case "oauthClientId" -> "APP_AUTH_OAUTH_CLIENT_ID";
            case "oauthClientSecret" -> "APP_AUTH_OAUTH_CLIENT_SECRET";
            case "oauthClientAuthenticationMethod" -> "APP_AUTH_OAUTH_CLIENT_AUTHENTICATION_METHOD";
            case "oauthProvider" -> "APP_AUTH_OAUTH_PROVIDER";
            case "oauthGitlabBaseUrl" -> "APP_AUTH_OAUTH_GITLAB_BASE_URL";
            case "oauthAuthorizationUri" -> "APP_AUTH_OAUTH_AUTHORIZATION_URI";
            case "oauthTokenUri" -> "APP_AUTH_OAUTH_TOKEN_URI";
            case "oauthUserInfoUri" -> "APP_AUTH_OAUTH_USER_INFO_URI";
            case "oauthUserNameAttribute" -> "APP_AUTH_OAUTH_USER_NAME_ATTRIBUTE";
            case "oauthRedirectUri" -> "APP_AUTH_OAUTH_REDIRECT_URI";
            case "oauthScopes" -> "APP_AUTH_OAUTH_SCOPES";
            default -> "";
        };
    }
}
