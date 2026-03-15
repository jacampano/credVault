package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.dto.ProfileForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import io.github.jacampano.credvault.security.EffectiveAuthSettings;
import io.github.jacampano.credvault.security.OAuthClientAuthenticationMethod;
import io.github.jacampano.credvault.security.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private AuthSettingsService authSettingsService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void profileInOauthUsesUsernameAndNameAsFullName() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_APP_USER")),
                Map.of(
                        "username", "gitlab-user",
                        "name", "GitLab Administrator",
                        "email", "admin@example.com"
                ),
                "username"
        );
        when(authentication.getName()).thenReturn("fallback-user");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        Model model = new ExtendedModelMap();
        String view = profileController.profile(authentication, model);

        assertThat(view).isEqualTo("profile/view");
        ProfileForm form = (ProfileForm) model.getAttribute("form");
        assertThat(form).isNotNull();
        assertThat(form.getUsername()).isEqualTo("gitlab-user");
        assertThat(form.getFullName()).isEqualTo("GitLab Administrator");
        assertThat(form.getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void profileInLocalCombinesFirstAndLastName() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        when(authentication.getName()).thenReturn("local.user");
        AppUser user = new AppUser();
        user.setUsername("local.user");
        user.setFirstName("Ana");
        user.setLastName("Lopez");
        user.setEmail("ana@example.com");
        when(appUserRepository.findByUsername("local.user")).thenReturn(Optional.of(user));

        Model model = new ExtendedModelMap();
        String view = profileController.profile(authentication, model);

        assertThat(view).isEqualTo("profile/view");
        ProfileForm form = (ProfileForm) model.getAttribute("form");
        assertThat(form).isNotNull();
        assertThat(form.getFullName()).isEqualTo("Ana Lopez");
    }

    @Test
    void updateLocalProfileSplitsFullNameIntoFirstAndLastName() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        when(authentication.getName()).thenReturn("local.user");
        AppUser user = new AppUser();
        user.setUsername("local.user");
        when(appUserRepository.findByUsername("local.user")).thenReturn(Optional.of(user));

        ProfileForm form = new ProfileForm();
        form.setFullName("Ana Maria Lopez");
        form.setEmail("ana@example.com");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = profileController.updateLocalProfile(authentication, form, binding, model, redirect);

        assertThat(view).isEqualTo("redirect:/profile");
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getLastName()).isEqualTo("Maria Lopez");
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
    }

    private EffectiveAuthSettings localSettings() {
        return new EffectiveAuthSettings(
                AuthMode.local,
                OAuthProvider.generic,
                null,
                null,
                OAuthClientAuthenticationMethod.client_secret_post,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private EffectiveAuthSettings oauthSettings() {
        return new EffectiveAuthSettings(
                AuthMode.oauth,
                OAuthProvider.gitlab,
                "client-id",
                "client-secret",
                OAuthClientAuthenticationMethod.client_secret_post,
                "http://gitlab/oauth/authorize",
                "http://gitlab/oauth/token",
                "http://gitlab/api/v4/user",
                "username",
                "profile,email,read_api",
                "{baseUrl}/oauth2/callback/{registrationId}",
                "admin@example.com"
        );
    }
}
