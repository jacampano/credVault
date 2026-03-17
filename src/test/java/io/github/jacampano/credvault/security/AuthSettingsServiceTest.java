package io.github.jacampano.credvault.security;

import io.github.jacampano.credvault.domain.config.AuthSettings;
import io.github.jacampano.credvault.dto.admin.AuthenticationSettingsForm;
import io.github.jacampano.credvault.repository.config.AuthSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSettingsServiceTest {

    @Mock
    private AuthProperties authProperties;

    @Mock
    private AuthSettingsRepository authSettingsRepository;

    @Mock
    private Environment environment;

    @InjectMocks
    private AuthSettingsService authSettingsService;

    @Test
    void loadEffectiveSettingsUsesGitlabDefaultsWhenProviderIsGitlab() {
        AuthSettings stored = new AuthSettings();
        stored.setId(AuthSettings.SINGLETON_ID);
        stored.setMode(AuthMode.oauth);
        stored.setOauthProvider(OAuthProvider.gitlab);
        stored.setOauthGitlabBaseUrl("https://gitlab.company.local");
        stored.setOauthClientId("client");
        stored.setOauthClientSecret("secret");
        stored.setOauthClientAuthenticationMethod(OAuthClientAuthenticationMethod.client_secret_basic);
        stored.setOauthUserNameAttribute("username");
        stored.setOauthScopes("read_user");
        stored.setOauthRedirectUri("{baseUrl}/oauth2/callback/{registrationId}");
        when(authSettingsRepository.findById(AuthSettings.SINGLETON_ID)).thenReturn(Optional.of(stored));

        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();

        assertThat(settings.oauthProvider()).isEqualTo(OAuthProvider.gitlab);
        assertThat(settings.oauthClientAuthenticationMethod()).isEqualTo(OAuthClientAuthenticationMethod.client_secret_basic);
        assertThat(settings.oauthAuthorizationUri()).isEqualTo("https://gitlab.company.local/oauth/authorize");
        assertThat(settings.oauthTokenUri()).isEqualTo("https://gitlab.company.local/oauth/token");
        assertThat(settings.oauthUserInfoUri()).isEqualTo("https://gitlab.company.local/api/v4/user");
    }

    @Test
    void saveFormStoresGitlabEndpointsAutomatically() {
        when(authSettingsRepository.findById(AuthSettings.SINGLETON_ID)).thenReturn(Optional.empty());
        AuthenticationSettingsForm form = new AuthenticationSettingsForm();
        form.setMode(AuthMode.oauth);
        form.setOauthProvider(OAuthProvider.gitlab);
        form.setOauthGitlabBaseUrl("https://gitlab.example.org/");
        form.setOauthClientId("id");
        form.setOauthClientSecret("secret");
        form.setOauthUserNameAttribute("username");
        form.setOauthScopes("read_user");
        form.setOauthRedirectUri("{baseUrl}/oauth2/callback/{registrationId}");

        authSettingsService.saveForm(form);

        ArgumentCaptor<AuthSettings> captor = ArgumentCaptor.forClass(AuthSettings.class);
        verify(authSettingsRepository).save(captor.capture());
        AuthSettings saved = captor.getValue();
        assertThat(saved.getOauthProvider()).isEqualTo(OAuthProvider.gitlab);
        assertThat(saved.getOauthClientAuthenticationMethod()).isEqualTo(OAuthClientAuthenticationMethod.client_secret_post);
        assertThat(saved.getOauthGitlabBaseUrl()).isEqualTo("https://gitlab.example.org");
        assertThat(saved.getOauthAuthorizationUri()).isEqualTo("https://gitlab.example.org/oauth/authorize");
        assertThat(saved.getOauthTokenUri()).isEqualTo("https://gitlab.example.org/oauth/token");
        assertThat(saved.getOauthUserInfoUri()).isEqualTo("https://gitlab.example.org/api/v4/user");
    }

    @Test
    void loadEffectiveSettingsUsesOauthAdminGroupsEnvOverStoredValue() {
        AuthSettings stored = new AuthSettings();
        stored.setId(AuthSettings.SINGLETON_ID);
        stored.setMode(AuthMode.oauth);
        stored.setOauthProvider(OAuthProvider.gitlab);
        stored.setOauthAdminGroups("stored-group");
        when(authSettingsRepository.findById(AuthSettings.SINGLETON_ID)).thenReturn(Optional.of(stored));
        when(authProperties.getOauthAdminGroups()).thenReturn("credvault-admin");

        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();

        assertThat(settings.adminGroupsLowerCase()).containsExactly("credvault-admin");
    }

    @Test
    void loadEffectiveSettingsUsesOauthGitlabBaseUrlEnvOverStoredValue() {
        AuthSettings stored = new AuthSettings();
        stored.setId(AuthSettings.SINGLETON_ID);
        stored.setMode(AuthMode.oauth);
        stored.setOauthProvider(OAuthProvider.gitlab);
        stored.setOauthGitlabBaseUrl("https://gitlab.stored.local");
        when(authSettingsRepository.findById(AuthSettings.SINGLETON_ID)).thenReturn(Optional.of(stored));
        when(authProperties.getOauthGitlabBaseUrl()).thenReturn("https://gitlab.env.local");

        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();

        assertThat(settings.oauthAuthorizationUri()).isEqualTo("https://gitlab.env.local/oauth/authorize");
        assertThat(settings.oauthTokenUri()).isEqualTo("https://gitlab.env.local/oauth/token");
        assertThat(settings.oauthUserInfoUri()).isEqualTo("https://gitlab.env.local/api/v4/user");
    }

    @Test
    void loadEffectiveSettingsUsesExplicitOauthUrisFromEnvWhenProviderIsGitlab() {
        AuthSettings stored = new AuthSettings();
        stored.setId(AuthSettings.SINGLETON_ID);
        stored.setMode(AuthMode.oauth);
        stored.setOauthProvider(OAuthProvider.gitlab);
        stored.setOauthGitlabBaseUrl("https://gitlab.stored.local");
        stored.setOauthAuthorizationUri("https://gitlab.stored.local/oauth/authorize");
        stored.setOauthTokenUri("https://gitlab.stored.local/oauth/token");
        stored.setOauthUserInfoUri("https://gitlab.stored.local/api/v4/user");
        when(authSettingsRepository.findById(AuthSettings.SINGLETON_ID)).thenReturn(Optional.of(stored));
        when(authProperties.getOauthGitlabBaseUrl()).thenReturn("https://gitlab.env.local");
        when(authProperties.getOauthAuthorizationUri()).thenReturn("https://oauth.env.local/authorize");
        when(authProperties.getOauthTokenUri()).thenReturn("https://oauth.env.local/token");
        when(authProperties.getOauthUserInfoUri()).thenReturn("https://oauth.env.local/userinfo");

        EffectiveAuthSettings settings = authSettingsService.loadEffectiveSettings();

        assertThat(settings.oauthAuthorizationUri()).isEqualTo("https://oauth.env.local/authorize");
        assertThat(settings.oauthTokenUri()).isEqualTo("https://oauth.env.local/token");
        assertThat(settings.oauthUserInfoUri()).isEqualTo("https://oauth.env.local/userinfo");
    }
}
