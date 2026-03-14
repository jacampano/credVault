package io.github.jacampano.credvault.security;

import io.github.jacampano.credvault.domain.config.AuthSettings;
import io.github.jacampano.credvault.dto.admin.AuthenticationSettingsForm;
import io.github.jacampano.credvault.repository.config.AuthSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthSettingsService {

    public static final String OAUTH_REGISTRATION_ID = "credvault";
    public static final String DEFAULT_REDIRECT_URI = "{baseUrl}/oauth2/callback/{registrationId}";

    private final AuthProperties authProperties;
    private final AuthSettingsRepository authSettingsRepository;

    public AuthSettingsService(AuthProperties authProperties,
                               AuthSettingsRepository authSettingsRepository) {
        this.authProperties = authProperties;
        this.authSettingsRepository = authSettingsRepository;
    }

    @Transactional(readOnly = true)
    public EffectiveAuthSettings loadEffectiveSettings() {
        AuthSettings stored = findStoredSettings().orElse(null);

        AuthMode mode = resolveMode(firstNonBlank(authProperties.getMode(), stored != null ? stored.getMode().name() : null));

        return new EffectiveAuthSettings(
                mode,
                firstNonBlank(authProperties.getOauthClientId(), stored != null ? stored.getOauthClientId() : null),
                firstNonBlank(authProperties.getOauthClientSecret(), stored != null ? stored.getOauthClientSecret() : null),
                firstNonBlank(authProperties.getOauthAuthorizationUri(), stored != null ? stored.getOauthAuthorizationUri() : null),
                firstNonBlank(authProperties.getOauthTokenUri(), stored != null ? stored.getOauthTokenUri() : null),
                firstNonBlank(authProperties.getOauthUserInfoUri(), stored != null ? stored.getOauthUserInfoUri() : null),
                firstNonBlank(authProperties.getOauthUserNameAttribute(), stored != null ? stored.getOauthUserNameAttribute() : null),
                firstNonBlank(authProperties.getOauthScopes(), stored != null ? stored.getOauthScopes() : null),
                firstNonBlank(authProperties.getOauthRedirectUri(), stored != null ? stored.getOauthRedirectUri() : DEFAULT_REDIRECT_URI),
                firstNonBlank(authProperties.getOauthAdminUsers(), stored != null ? stored.getOauthAdminUsers() : null)
        );
    }

    @Transactional(readOnly = true)
    public AuthenticationSettingsForm loadForm() {
        AuthSettings stored = findStoredSettings().orElse(null);
        AuthenticationSettingsForm form = new AuthenticationSettingsForm();

        if (stored != null) {
            form.setMode(stored.getMode());
            form.setOauthClientId(stored.getOauthClientId());
            form.setOauthClientSecret(stored.getOauthClientSecret());
            form.setOauthAuthorizationUri(stored.getOauthAuthorizationUri());
            form.setOauthTokenUri(stored.getOauthTokenUri());
            form.setOauthUserInfoUri(stored.getOauthUserInfoUri());
            form.setOauthUserNameAttribute(stored.getOauthUserNameAttribute());
            form.setOauthScopes(stored.getOauthScopes());
            form.setOauthRedirectUri(stored.getOauthRedirectUri());
            form.setOauthAdminUsers(stored.getOauthAdminUsers());
            return form;
        }

        form.setMode(AuthMode.local);
        form.setOauthScopes("openid,profile,email");
        form.setOauthRedirectUri(DEFAULT_REDIRECT_URI);
        return form;
    }

    @Transactional
    public void saveForm(AuthenticationSettingsForm form) {
        AuthSettings settings = findStoredSettings().orElseGet(() -> {
            AuthSettings created = new AuthSettings();
            created.setId(AuthSettings.SINGLETON_ID);
            return created;
        });

        settings.setMode(form.getMode());
        settings.setOauthClientId(trimToNull(form.getOauthClientId()));
        settings.setOauthClientSecret(trimToNull(form.getOauthClientSecret()));
        settings.setOauthAuthorizationUri(trimToNull(form.getOauthAuthorizationUri()));
        settings.setOauthTokenUri(trimToNull(form.getOauthTokenUri()));
        settings.setOauthUserInfoUri(trimToNull(form.getOauthUserInfoUri()));
        settings.setOauthUserNameAttribute(trimToNull(form.getOauthUserNameAttribute()));
        settings.setOauthScopes(trimToNull(form.getOauthScopes()));
        settings.setOauthRedirectUri(trimToNull(form.getOauthRedirectUri()));
        settings.setOauthAdminUsers(trimToNull(form.getOauthAdminUsers()));

        authSettingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public Map<String, String> findActiveEnvironmentOverrides() {
        Map<String, String> overrides = new LinkedHashMap<>();
        putIfPresent(overrides, "APP_AUTH_MODE", authProperties.getMode());
        putIfPresent(overrides, "APP_AUTH_OAUTH_CLIENT_ID", authProperties.getOauthClientId());
        putIfPresent(overrides, "APP_AUTH_OAUTH_CLIENT_SECRET", maskSecret(authProperties.getOauthClientSecret()));
        putIfPresent(overrides, "APP_AUTH_OAUTH_AUTHORIZATION_URI", authProperties.getOauthAuthorizationUri());
        putIfPresent(overrides, "APP_AUTH_OAUTH_TOKEN_URI", authProperties.getOauthTokenUri());
        putIfPresent(overrides, "APP_AUTH_OAUTH_USER_INFO_URI", authProperties.getOauthUserInfoUri());
        putIfPresent(overrides, "APP_AUTH_OAUTH_USER_NAME_ATTRIBUTE", authProperties.getOauthUserNameAttribute());
        putIfPresent(overrides, "APP_AUTH_OAUTH_SCOPES", authProperties.getOauthScopes());
        putIfPresent(overrides, "APP_AUTH_OAUTH_REDIRECT_URI", authProperties.getOauthRedirectUri());
        putIfPresent(overrides, "APP_AUTH_OAUTH_ADMIN_USERS", authProperties.getOauthAdminUsers());
        return overrides;
    }

    public String resolveRedirectionEndpointBaseUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return "/oauth2/callback/*";
        }

        try {
            String candidate = redirectUri.trim()
                    .replace("{baseUrl}", "")
                    .replace("{baseScheme}://{baseHost}{basePort}", "");

            String path;
            if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
                path = URI.create(candidate).getPath();
            } else {
                path = candidate;
            }

            if (path.contains("{registrationId}")) {
                path = path.replace("{registrationId}", "*");
            }

            if (!StringUtils.hasText(path)) {
                return "/oauth2/callback/*";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.endsWith("/" + OAUTH_REGISTRATION_ID)) {
                return path.substring(0, path.length() - OAUTH_REGISTRATION_ID.length()) + "*";
            }
            if (path.endsWith("/*")) {
                return path;
            }
            return path.endsWith("/") ? path + "*" : path + "/*";
        } catch (IllegalArgumentException ex) {
            return "/oauth2/callback/*";
        }
    }

    private Optional<AuthSettings> findStoredSettings() {
        try {
            return authSettingsRepository.findById(AuthSettings.SINGLETON_ID);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private AuthMode resolveMode(String rawMode) {
        if (!StringUtils.hasText(rawMode)) {
            return AuthMode.local;
        }

        try {
            return AuthMode.valueOf(rawMode.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return AuthMode.local;
        }
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : trimToNull(second);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String maskSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return "********";
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }
}
