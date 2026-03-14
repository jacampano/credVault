package io.github.jacampano.credvault.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthSettingsService authSettingsService,
                                                   GrantedAuthoritiesMapper oauthGrantedAuthoritiesMapper) throws Exception {
        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/login", "/css/**").permitAll()
                        .requestMatchers("/", "/credentials", "/credentials/").hasAnyRole("APP_USER", "ADMIN")
                        .requestMatchers("/calendar", "/calendar/**").hasAnyRole("APP_USER", "ADMIN")
                        .requestMatchers("/profile").hasAnyRole("APP_USER", "ADMIN")
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/credentials/new").hasAnyRole("APP_USER", "ADMIN")
                        .requestMatchers("/credentials/*/edit").hasAnyRole("APP_USER", "ADMIN")
                        .requestMatchers("/credentials/**").hasAnyRole("APP_USER", "ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        if (settings.mode() == AuthMode.oauth) {
            String redirectionBaseUri = authSettingsService.resolveRedirectionEndpointBaseUri(settings.oauthRedirectUri());
            http.oauth2Login(oauth -> oauth
                    .loginPage("/oauth2/authorization/" + AuthSettingsService.OAUTH_REGISTRATION_ID)
                    .redirectionEndpoint(redirection -> redirection.baseUri(redirectionBaseUri))
                    .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(oauthGrantedAuthoritiesMapper)));
        } else {
            http.formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/credentials", true)
                    .permitAll());
        }

        return http.build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(AuthSettingsService authSettingsService) {
        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();

        if (settings.mode() != AuthMode.oauth) {
            return registrationId -> null;
        }

        ClientRegistration registration = ClientRegistration.withRegistrationId(AuthSettingsService.OAUTH_REGISTRATION_ID)
                .clientName("CredVault OAuth")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId(requireValue(settings.oauthClientId(), "oauthClientId"))
                .clientSecret(requireValue(settings.oauthClientSecret(), "oauthClientSecret"))
                .authorizationUri(requireValue(settings.oauthAuthorizationUri(), "oauthAuthorizationUri"))
                .tokenUri(requireValue(settings.oauthTokenUri(), "oauthTokenUri"))
                .userInfoUri(requireValue(settings.oauthUserInfoUri(), "oauthUserInfoUri"))
                .userNameAttributeName(requireValue(settings.oauthUserNameAttribute(), "oauthUserNameAttribute"))
                .redirectUri(StringUtils.hasText(settings.oauthRedirectUri()) ? settings.oauthRedirectUri() : AuthSettingsService.DEFAULT_REDIRECT_URI)
                .scope(settings.oauthScopesSet())
                .build();

        return registrationId -> AuthSettingsService.OAUTH_REGISTRATION_ID.equals(registrationId) ? registration : null;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public GrantedAuthoritiesMapper oauthGrantedAuthoritiesMapper(AuthSettingsService authSettingsService) {
        return authorities -> {
            Set<String> adminUsers = authSettingsService.loadEffectiveSettings().adminUsersLowerCase();
            Set<GrantedAuthority> mapped = new HashSet<>();
            mapped.add(new SimpleGrantedAuthority("ROLE_APP_USER"));

            String principalName = extractOAuthPrincipal(authorities);
            if (principalName != null && adminUsers.contains(principalName.toLowerCase(Locale.ROOT))) {
                mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            return mapped;
        };
    }

    private String extractOAuthPrincipal(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                String email = oidcUserAuthority.getUserInfo() != null ? oidcUserAuthority.getUserInfo().getEmail() : null;
                if (StringUtils.hasText(email)) {
                    return email;
                }
                String preferredUsername = oidcUserAuthority.getIdToken().getClaimAsString("preferred_username");
                if (StringUtils.hasText(preferredUsername)) {
                    return preferredUsername;
                }
            }

            if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                Map<String, Object> attrs = oauth2UserAuthority.getAttributes();
                String email = readString(attrs, "email");
                if (email != null) {
                    return email;
                }
                String login = readString(attrs, "login");
                if (login != null) {
                    return login;
                }
                String preferredUsername = readString(attrs, "preferred_username");
                if (preferredUsername != null) {
                    return preferredUsername;
                }
            }
        }
        return null;
    }

    private String readString(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue;
        }
        return null;
    }

    private String requireValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Falta configuración OAuth requerida: " + fieldName);
        }
        return value;
    }
}
