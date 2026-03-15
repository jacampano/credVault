package io.github.jacampano.credvault.security;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record EffectiveAuthSettings(
        AuthMode mode,
        OAuthProvider oauthProvider,
        String oauthClientId,
        String oauthClientSecret,
        OAuthClientAuthenticationMethod oauthClientAuthenticationMethod,
        String oauthAuthorizationUri,
        String oauthTokenUri,
        String oauthUserInfoUri,
        String oauthUserNameAttribute,
        String oauthScopes,
        String oauthRedirectUri,
        String oauthAdminGroups
) {
    public boolean usesGitlabProvider() {
        return oauthProvider == OAuthProvider.gitlab;
    }

    public Set<String> adminGroupsLowerCase() {
        return splitCsv(oauthAdminGroups).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> oauthScopesSet() {
        Set<String> scopes = splitCsv(oauthScopes);
        if (scopes.isEmpty()) {
            return Set.of("profile", "email", "read_user", "read_api");
        }
        return scopes;
    }

    public static Set<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
