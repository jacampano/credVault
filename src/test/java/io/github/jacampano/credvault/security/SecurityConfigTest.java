package io.github.jacampano.credvault.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void oauthMapperAssignsAdminWhenUserBelongsToConfiguredGitlabGroup() {
        SecurityConfig securityConfig = new SecurityConfig();
        AuthSettingsService authSettingsService = mock(AuthSettingsService.class);
        when(authSettingsService.loadEffectiveSettings()).thenReturn(new EffectiveAuthSettings(
                AuthMode.oauth,
                OAuthProvider.gitlab,
                "id",
                "secret",
                OAuthClientAuthenticationMethod.client_secret_post,
                "http://gitlab/oauth/authorize",
                "http://gitlab/oauth/token",
                "http://gitlab/api/v4/user",
                "username",
                "profile,email,read_user,read_api",
                "{baseUrl}/oauth2/callback/{registrationId}",
                "credvault-admin"
        ));

        GrantedAuthoritiesMapper mapper = securityConfig.oauthGrantedAuthoritiesMapper(authSettingsService);
        Set<? extends GrantedAuthority> mapped = Set.copyOf(mapper.mapAuthorities(List.of(
                new OAuth2UserAuthority(Map.of("gitlabGroupPaths", List.of("platform/credvault-admin")))
        )));

        Set<String> roles = mapped.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertThat(roles).contains("ROLE_APP_USER", "ROLE_ADMIN");
    }

    @Test
    void oauthMapperAssignsAdminWhenConfiguredGroupUsesUnderscoreAndGitlabPathUsesDash() {
        SecurityConfig securityConfig = new SecurityConfig();
        AuthSettingsService authSettingsService = mock(AuthSettingsService.class);
        when(authSettingsService.loadEffectiveSettings()).thenReturn(new EffectiveAuthSettings(
                AuthMode.oauth,
                OAuthProvider.gitlab,
                "id",
                "secret",
                OAuthClientAuthenticationMethod.client_secret_post,
                "http://gitlab/oauth/authorize",
                "http://gitlab/oauth/token",
                "http://gitlab/api/v4/user",
                "username",
                "profile,email,read_user,read_api",
                "{baseUrl}/oauth2/callback/{registrationId}",
                "credvault_admin"
        ));

        GrantedAuthoritiesMapper mapper = securityConfig.oauthGrantedAuthoritiesMapper(authSettingsService);
        Set<? extends GrantedAuthority> mapped = Set.copyOf(mapper.mapAuthorities(List.of(
                new OAuth2UserAuthority(Map.of("gitlabGroupPaths", List.of("platform/credvault-admin")))
        )));

        Set<String> roles = mapped.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertThat(roles).contains("ROLE_APP_USER", "ROLE_ADMIN");
    }
}
