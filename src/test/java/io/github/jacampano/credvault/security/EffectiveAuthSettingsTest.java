package io.github.jacampano.credvault.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EffectiveAuthSettingsTest {

    @Test
    void oauthScopesSetUsesExpectedDefaultsWhenEmpty() {
        EffectiveAuthSettings settings = new EffectiveAuthSettings(
                AuthMode.oauth,
                OAuthProvider.gitlab,
                "id",
                "secret",
                OAuthClientAuthenticationMethod.client_secret_post,
                "http://gitlab/oauth/authorize",
                "http://gitlab/oauth/token",
                "http://gitlab/api/v4/user",
                "username",
                null,
                "{baseUrl}/oauth2/callback/{registrationId}",
                null
        );

        assertThat(settings.oauthScopesSet()).containsExactlyInAnyOrder("profile", "email", "read_user", "read_api");
    }
}
