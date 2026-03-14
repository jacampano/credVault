package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.auth.AppRole;
import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.dto.admin.UserAdminForm;
import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import io.github.jacampano.credvault.security.EffectiveAuthSettings;
import io.github.jacampano.credvault.service.AdminTeamService;
import io.github.jacampano.credvault.service.AdminUserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private AuthSettingsService authSettingsService;

    @Mock
    private AdminTeamService adminTeamService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        lenient().when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        lenient().when(adminTeamService.findAllTeamNames()).thenReturn(Set.of("TEAM1", "TEAM2"));
    }

    @Test
    void listUsersUsesSafeDefaultsWhenParamsNull() {
        Page<AppUser> users = new PageImpl<>(List.of());
        when(adminUserService.listUsers(any(), any())).thenReturn(users);
        Model model = new ExtendedModelMap();

        String view = adminUserController.listUsers(null, null, null, null, null, model);

        assertThat(view).isEqualTo("admin/users/list");
        assertThat(model.getAttribute("sort")).isEqualTo("username");
        assertThat(model.getAttribute("dir")).isEqualTo("asc");
        assertThat(model.getAttribute("size")).isEqualTo(10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminUserService).listUsers(any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("username")).isNotNull();
    }

    @Test
    void listUsersFallsBackWhenSortNotAllowed() {
        Page<AppUser> users = new PageImpl<>(List.of());
        when(adminUserService.listUsers(any(), any())).thenReturn(users);
        Model model = new ExtendedModelMap();

        adminUserController.listUsers("ana", 0, 20, "hack", "desc", model);

        assertThat(model.getAttribute("sort")).isEqualTo("username");
        assertThat(model.getAttribute("dir")).isEqualTo("desc");
    }

    @Test
    void editUserRedirectsWhenNotFound() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();
        when(adminUserService.findById(5L)).thenThrow(new EntityNotFoundException("not found"));

        String view = adminUserController.editUser(5L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void updateUserReturnsEditViewWhenBindingErrors() {
        UserAdminForm form = new UserAdminForm();
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        binding.rejectValue("username", "required", "required");

        String view = adminUserController.updateUser(3L, form, binding, authentication, model, redirect);

        assertThat(view).isEqualTo("admin/users/edit");
        assertThat(model.getAttribute("userId")).isEqualTo(3L);
    }

    @Test
    void updateUserRedirectsOnSuccess() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("user1");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(authentication.getName()).thenReturn("admin");

        String view = adminUserController.updateUser(3L, form, binding, authentication, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(redirect.getFlashAttributes()).containsKey("message");
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Usuario actualizado correctamente");
        verify(adminUserService).updateUser(3L, form, "admin");
    }

    @Test
    void updateUserReturnsEditWhenBusinessError() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("user1");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(authentication.getName()).thenReturn("admin");
        doThrow(new IllegalArgumentException("bad user")).when(adminUserService).updateUser(3L, form, "admin");

        String view = adminUserController.updateUser(3L, form, binding, authentication, model, redirect);

        assertThat(view).isEqualTo("admin/users/edit");
        assertThat(binding.hasErrors()).isTrue();
    }

    @Test
    void toggleStatusRedirectsWithMessageOnSuccess() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(authentication.getName()).thenReturn("admin");

        String view = adminUserController.toggleStatus(10L, authentication, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(redirect.getFlashAttributes()).containsKey("message");
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Estado de usuario actualizado");
    }

    @Test
    void deleteUserRedirectsWithErrorOnFailure() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(authentication.getName()).thenReturn("admin");
        doThrow(new IllegalArgumentException("cannot delete")).when(adminUserService).deleteUser(10L, "admin");

        String view = adminUserController.deleteUser(10L, authentication, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editUserLoadsFormWhenExists() {
        AppUser user = new AppUser();
        user.setId(5L);
        user.setUsername("u1");
        user.setRoles(Set.of(AppRole.APP_USER));

        UserAdminForm form = new UserAdminForm();
        form.setUsername("u1");

        when(adminUserService.findById(5L)).thenReturn(user);
        when(adminUserService.toForm(user)).thenReturn(form);

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminUserController.editUser(5L, model, redirect);

        assertThat(view).isEqualTo("admin/users/edit");
        assertThat(model.getAttribute("form")).isEqualTo(form);
        assertThat(model.getAttribute("userId")).isEqualTo(5L);
    }

    @Test
    void newUserFormReturnsCreateViewInLocalMode() {
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminUserController.newUserForm(model, redirect);

        assertThat(view).isEqualTo("admin/users/create");
        assertThat(model.getAttribute("form")).isInstanceOf(UserAdminForm.class);
    }

    @Test
    void newUserFormRedirectsInOauthMode() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminUserController.newUserForm(model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void createUserCallsServiceAndRedirects() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new.user");
        form.setNewPassword("secret");
        form.setAppUserRole(true);
        form.setEnabled(true);
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminUserController.createUser(form, binding, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        verify(adminUserService).createUser(form);
        assertThat(redirect.getFlashAttributes()).containsKey("message");
    }

    @Test
    void createUserReturnsFormWhenBindingError() {
        UserAdminForm form = new UserAdminForm();
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        binding.rejectValue("username", "required", "required");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminUserController.createUser(form, binding, model, redirect);

        assertThat(view).isEqualTo("admin/users/create");
    }

    @Test
    void createUserReturnsFormWhenBusinessError() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new.user");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("exists")).when(adminUserService).createUser(form);

        String view = adminUserController.createUser(form, binding, model, redirect);

        assertThat(view).isEqualTo("admin/users/create");
        assertThat(binding.hasErrors()).isTrue();
    }

    @Test
    void createUserRedirectsInOauthMode() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        UserAdminForm form = new UserAdminForm();
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminUserController.createUser(form, binding, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    private EffectiveAuthSettings localSettings() {
        return new EffectiveAuthSettings(
                AuthMode.local,
                null,
                null,
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
                "id",
                "secret",
                "https://auth.example/authorize",
                "https://auth.example/token",
                "https://auth.example/userinfo",
                "email",
                "openid,email",
                "{baseUrl}/oauth2/callback/{registrationId}",
                null
        );
    }
}
