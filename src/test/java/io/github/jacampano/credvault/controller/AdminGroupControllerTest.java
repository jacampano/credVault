package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.dto.admin.GroupForm;
import io.github.jacampano.credvault.security.AuthMode;
import io.github.jacampano.credvault.security.AuthSettingsService;
import io.github.jacampano.credvault.security.EffectiveAuthSettings;
import io.github.jacampano.credvault.security.OAuthClientAuthenticationMethod;
import io.github.jacampano.credvault.security.OAuthProvider;
import io.github.jacampano.credvault.service.AdminGroupService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGroupControllerTest {

    @Mock
    private AdminGroupService adminGroupService;

    @Mock
    private AuthSettingsService authSettingsService;

    @InjectMocks
    private AdminGroupController adminGroupController;

    private EffectiveAuthSettings localSettings() {
        return new EffectiveAuthSettings(
                AuthMode.local,
                OAuthProvider.gitlab,
                "id",
                "secret",
                OAuthClientAuthenticationMethod.client_secret_post,
                "http://gitlab/oauth/authorize",
                "http://gitlab/oauth/token",
                "http://gitlab/api/v4/user",
                "username",
                "profile,email",
                "{baseUrl}/oauth2/callback/{registrationId}",
                "credvault-admin"
        );
    }

    private EffectiveAuthSettings oauthSettings() {
        return new EffectiveAuthSettings(
                AuthMode.oauth,
                OAuthProvider.gitlab,
                "id",
                "secret",
                OAuthClientAuthenticationMethod.client_secret_post,
                "http://gitlab/oauth/authorize",
                "http://gitlab/oauth/token",
                "http://gitlab/api/v4/user",
                "username",
                "profile,email",
                "{baseUrl}/oauth2/callback/{registrationId}",
                "credvault-admin"
        );
    }

    @Test
    void listGroupsLoadsView() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        when(adminGroupService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = adminGroupController.listGroups(model);

        assertThat(view).isEqualTo("admin/groups/list");
        assertThat(model.getAttribute("groups")).isNotNull();
        assertThat(model.getAttribute("oauthManaged")).isEqualTo(false);
    }

    @Test
    void createGroupRedirectsWhenSuccess() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        GroupForm form = new GroupForm();
        form.setName("DEVOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.createGroup(form, binding, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        verify(adminGroupService).createGroup(form);
    }

    @Test
    void createGroupReturnsFormWhenBusinessError() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        GroupForm form = new GroupForm();
        form.setName("DEVOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("exists")).when(adminGroupService).createGroup(form);

        String view = adminGroupController.createGroup(form, binding, redirect);

        assertThat(view).isEqualTo("admin/groups/create");
        assertThat(binding.hasErrors()).isTrue();
    }

    @Test
    void editGroupRedirectsWhenNotFound() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(adminGroupService.findById(9L)).thenThrow(new EntityNotFoundException("not found"));

        String view = adminGroupController.editGroup(9L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editGroupLoadsFormWhenFound() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        Group group = new Group();
        group.setId(2L);
        group.setName("DEVOPS");
        GroupForm form = new GroupForm();
        form.setName("DEVOPS");
        when(adminGroupService.findById(2L)).thenReturn(group);
        when(adminGroupService.toForm(group)).thenReturn(form);
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.editGroup(2L, model, redirect);

        assertThat(view).isEqualTo("admin/groups/edit");
        assertThat(model.getAttribute("groupId")).isEqualTo(2L);
    }

    @Test
    void updateGroupRedirectsWhenSuccess() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        GroupForm form = new GroupForm();
        form.setName("SECOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.updateGroup(4L, form, binding, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        verify(adminGroupService).updateGroup(4L, form);
    }

    @Test
    void deleteGroupRedirectsWithErrorWhenBusinessRuleFails() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(localSettings());
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("miembros")).when(adminGroupService).deleteGroup(5L);

        String view = adminGroupController.deleteGroup(5L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void createGroupInOauthModeRedirectsWithError() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        GroupForm form = new GroupForm();
        form.setName("DEVOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.createGroup(form, binding, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void newGroupFormInOauthModeRedirects() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.newGroupForm(model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editGroupInOauthModeAllowsManualGroup() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        Group group = new Group();
        group.setId(2L);
        group.setName("MANUAL");
        group.setOauthSynchronized(false);
        GroupForm form = new GroupForm();
        form.setName("MANUAL");
        when(adminGroupService.findById(2L)).thenReturn(group);
        when(adminGroupService.toForm(group)).thenReturn(form);
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.editGroup(2L, model, redirect);

        assertThat(view).isEqualTo("admin/groups/edit");
    }

    @Test
    void editGroupInOauthModeBlocksSyncedGroup() {
        when(authSettingsService.loadEffectiveSettings()).thenReturn(oauthSettings());
        Group group = new Group();
        group.setId(2L);
        group.setName("SYNCED");
        group.setOauthSynchronized(true);
        when(adminGroupService.findById(2L)).thenReturn(group);
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminGroupController.editGroup(2L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/groups");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }
}
