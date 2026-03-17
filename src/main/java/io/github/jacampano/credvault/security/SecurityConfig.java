package io.github.jacampano.credvault.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthSettingsService authSettingsService,
                                                   GrantedAuthoritiesMapper oauthGrantedAuthoritiesMapper,
                                                   OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) throws Exception {
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
            boolean logSensitiveOauth = Boolean.parseBoolean(Optional.ofNullable(System.getenv("APP_AUTH_OAUTH_LOG_SENSITIVE")).orElse("false"));
            LOG.info("OAUTH_SECURITY_INIT mode={} provider={} sensitiveTokenLog={} redirectBaseUri={} tokenUri={}",
                    settings.mode(),
                    settings.oauthProvider(),
                    logSensitiveOauth,
                    redirectionBaseUri,
                    settings.oauthTokenUri());
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .redirectionEndpoint(redirection -> redirection.baseUri(redirectionBaseUri))
                    .tokenEndpoint(token -> token.accessTokenResponseClient(
                            oauth2AccessTokenResponseClient(settings, logSensitiveOauth)))
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(oauth2UserService)
                            .userAuthoritiesMapper(oauthGrantedAuthoritiesMapper))
                    .successHandler((request, response, authentication) -> {
                        LOG.info("OAUTH_LOGIN_SUCCESS user={} authorities={} sessionId={} remoteIp={}",
                                authentication != null ? authentication.getName() : "unknown",
                                authentication != null ? authentication.getAuthorities() : "[]",
                                request.getRequestedSessionId(),
                                request.getRemoteAddr());
                        response.sendRedirect("/credentials");
                    })
                    .failureHandler((request, response, exception) -> {
                        LOG.error("OAUTH_LOGIN_FAILURE path={} query={} sessionId={} remoteIp={} errorType={} message={}",
                                request.getRequestURI(),
                                request.getQueryString(),
                                request.getRequestedSessionId(),
                                request.getRemoteAddr(),
                                exception.getClass().getSimpleName(),
                                exception.getMessage());
                        response.sendRedirect("/login?oauthError=true");
                    }));
        } else {
            http.formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/credentials", true)
                    .permitAll());
        }

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(AuthSettingsService authSettingsService,
                                                                               OAuthGroupSyncService oauthGroupSyncService) {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        RestClient restClient = RestClient.builder().build();

        return userRequest -> {
            OAuth2User oauth2User = delegate.loadUser(userRequest);
            EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();
            if (!settings.usesGitlabProvider()) {
                return oauth2User;
            }

            String groupsUri = buildGitlabGroupsUri(settings.oauthUserInfoUri());
            List<String> groupPaths = fetchGitlabGroupPaths(restClient, groupsUri, userRequest.getAccessToken().getTokenValue());

            Map<String, Object> enrichedAttributes = new LinkedHashMap<>(oauth2User.getAttributes());
            enrichedAttributes.put("gitlabGroupPaths", groupPaths);
            LOG.info("OAUTH_GITLAB_GROUPS user={} groupsCount={} groups={}",
                    oauth2User.getName(),
                    groupPaths.size(),
                    groupPaths);
            oauthGroupSyncService.syncUser(oauth2User, groupPaths);

            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();
            if (!StringUtils.hasText(userNameAttributeName)) {
                userNameAttributeName = "sub";
            }

            Set<GrantedAuthority> enrichedAuthorities = new LinkedHashSet<>();
            for (GrantedAuthority authority : oauth2User.getAuthorities()) {
                if (!(authority instanceof OAuth2UserAuthority)) {
                    enrichedAuthorities.add(authority);
                }
            }
            enrichedAuthorities.add(new OAuth2UserAuthority(enrichedAttributes));
            return new DefaultOAuth2User(enrichedAuthorities, enrichedAttributes, userNameAttributeName);
        };
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oauth2AccessTokenResponseClient(
            EffectiveAuthSettings settings,
            boolean logSensitiveOauth
    ) {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> loggingConverter = request -> {
            RequestEntity<?> entity = defaultConverter.convert(request);
            if (entity != null) {
                if (logSensitiveOauth) {
                    LOG.warn(
                            "OAUTH_TOKEN_REQUEST_DEBUG uri={} method={} headers={} body={} clientId={} clientSecret={} authMethod={}",
                            entity.getUrl(),
                            entity.getMethod(),
                            entity.getHeaders(),
                            entity.getBody(),
                            settings.oauthClientId(),
                            settings.oauthClientSecret(),
                            settings.oauthClientAuthenticationMethod()
                    );
                } else {
                    LOG.info(
                            "OAUTH_TOKEN_REQUEST uri={} method={} authMethod={} clientId={} (set APP_AUTH_OAUTH_LOG_SENSITIVE=true for full request debug)",
                            entity.getUrl(),
                            entity.getMethod(),
                            settings.oauthClientAuthenticationMethod(),
                            settings.oauthClientId()
                    );
                }
            }
            return entity;
        };
        client.setRequestEntityConverter(loggingConverter);
        return client;
    }

    private List<String> fetchGitlabGroupPaths(RestClient restClient, String groupsUri, String accessToken) {
        if (!StringUtils.hasText(groupsUri) || !StringUtils.hasText(accessToken)) {
            return List.of();
        }
        try {
            Object responseBody = restClient.get()
                    .uri(groupsUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Object.class);
            if (!(responseBody instanceof List<?> rawList)) {
                return List.of();
            }

            List<String> groupPaths = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?, ?> group) {
                    String groupValue = firstNonBlank(
                            asNonBlankText(group.get("full_path")),
                            asNonBlankText(group.get("path")),
                            asNonBlankText(group.get("name"))
                    );
                    if (StringUtils.hasText(groupValue)) {
                        groupPaths.add(groupValue);
                    }
                }
            }
            return groupPaths;
        } catch (RuntimeException ex) {
            LOG.warn("OAUTH_GITLAB_GROUPS_FETCH_FAILED uri={} message={}", groupsUri, ex.getMessage());
            return List.of();
        }
    }

    private String buildGitlabGroupsUri(String userInfoUri) {
        if (!StringUtils.hasText(userInfoUri)) {
            return null;
        }

        String uri = userInfoUri.trim();
        if (uri.contains("/api/v4/user")) {
            return uri.replace("/api/v4/user", "/api/v4/groups?membership=true&per_page=100");
        }

        int apiV4Index = uri.indexOf("/api/v4/");
        String base = apiV4Index > 0 ? uri.substring(0, apiV4Index) : uri;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v4/groups?membership=true&per_page=100";
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
                .clientAuthenticationMethod(resolveClientAuthenticationMethod(settings.oauthClientAuthenticationMethod()))
                .authorizationUri(requireValue(settings.oauthAuthorizationUri(), "oauthAuthorizationUri"))
                .tokenUri(requireValue(settings.oauthTokenUri(), "oauthTokenUri"))
                .userInfoUri(requireValue(settings.oauthUserInfoUri(), "oauthUserInfoUri"))
                .userNameAttributeName(requireValue(settings.oauthUserNameAttribute(), "oauthUserNameAttribute"))
                .redirectUri(StringUtils.hasText(settings.oauthRedirectUri()) ? settings.oauthRedirectUri() : AuthSettingsService.DEFAULT_REDIRECT_URI)
                .scope(settings.oauthScopesSet())
                .build();

        LOG.info("OAUTH_CLIENT_REGISTRATION provider={} tokenUri={} authMethod={} redirectUri={}",
                settings.oauthProvider(),
                settings.oauthTokenUri(),
                settings.oauthClientAuthenticationMethod(),
                settings.oauthRedirectUri());

        return registrationId -> AuthSettingsService.OAUTH_REGISTRATION_ID.equals(registrationId) ? registration : null;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public GrantedAuthoritiesMapper oauthGrantedAuthoritiesMapper(AuthSettingsService authSettingsService) {
        return authorities -> {
            Set<String> adminGroups = authSettingsService.loadEffectiveSettings().adminGroupsLowerCase();
            Set<GrantedAuthority> mapped = new HashSet<>();
            mapped.add(new SimpleGrantedAuthority("ROLE_APP_USER"));

            Set<String> currentUserGroups = extractOAuthGroupsLowerCase(authorities);
            boolean isAdmin = belongsToAdminGroup(currentUserGroups, adminGroups);
            LOG.info("OAUTH_ADMIN_GROUP_CHECK configuredGroups={} userGroups={} isAdmin={}",
                    adminGroups,
                    currentUserGroups,
                    isAdmin);
            if (isAdmin) {
                mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            return mapped;
        };
    }

    private boolean belongsToAdminGroup(Set<String> currentUserGroups, Set<String> adminGroups) {
        if (adminGroups.isEmpty() || currentUserGroups.isEmpty()) {
            return false;
        }

        Set<String> normalizedConfigured = expandGroupCandidates(adminGroups);
        for (String group : currentUserGroups) {
            Set<String> userCandidates = expandGroupCandidates(Set.of(group));
            for (String candidate : userCandidates) {
                if (normalizedConfigured.contains(candidate)) {
                    return true;
                }
            }
        }
        return false;
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

    private Set<String> extractOAuthGroupsLowerCase(Collection<? extends GrantedAuthority> authorities) {
        Set<String> groups = new LinkedHashSet<>();
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                Object groupValue = oauth2UserAuthority.getAttributes().get("gitlabGroupPaths");
                if (groupValue instanceof Collection<?> rawGroups) {
                    for (Object raw : rawGroups) {
                        if (raw instanceof String stringValue && StringUtils.hasText(stringValue)) {
                            groups.add(stringValue.trim().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
        }
        return groups;
    }

    private Set<String> expandGroupCandidates(Set<String> groups) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String group : groups) {
            String normalized = normalizeGroupToken(group);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            candidates.add(normalized);
            candidates.add(normalized.replace('-', '_'));
            candidates.add(normalized.replace('_', '-'));

            int lastSlash = normalized.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash + 1 < normalized.length()) {
                String leaf = normalized.substring(lastSlash + 1);
                candidates.add(leaf);
                candidates.add(leaf.replace('-', '_'));
                candidates.add(leaf.replace('_', '-'));
            }
        }
        return candidates;
    }

    private String normalizeGroupToken(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String readString(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue;
        }
        return null;
    }

    private String asNonBlankText(Object value) {
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue.trim();
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String requireValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Falta configuración OAuth requerida: " + fieldName);
        }
        return value;
    }

    private ClientAuthenticationMethod resolveClientAuthenticationMethod(OAuthClientAuthenticationMethod method) {
        if (method == OAuthClientAuthenticationMethod.client_secret_basic) {
            return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        }
        return ClientAuthenticationMethod.CLIENT_SECRET_POST;
    }
}
