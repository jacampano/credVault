package com.example.credvault.controller;

import com.example.credvault.domain.Credential;
import com.example.credvault.dto.CredentialForm;
import com.example.credvault.service.CredentialService;
import com.example.credvault.service.UserAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialWebControllerTest {

    @Mock
    private CredentialService credentialService;

    @Mock
    private UserAccessService userAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CredentialWebController credentialWebController;

    @Test
    void listLoadsOnlyCredentialsVisibleForUserTeams() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        when(credentialService.findAllVisibleForTeams(Set.of("DEVOPS"))).thenReturn(List.of(new Credential()));
        Model model = new ExtendedModelMap();

        String view = credentialWebController.list(model, authentication);

        assertThat(view).isEqualTo("credentials/list");
        assertThat(model.getAttribute("credentials")).asList().hasSize(1);
        verify(credentialService).findAllVisibleForTeams(Set.of("DEVOPS"));
    }

    @Test
    void createFormAutoAssignsTeamWhenUserHasOnlyOne() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("SECOPS"));
        when(credentialService.normalizeTeams(Set.of("SECOPS"))).thenReturn(Set.of("SECOPS"));
        Model model = new ExtendedModelMap();

        String view = credentialWebController.createForm(model, authentication);

        assertThat(view).isEqualTo("credentials/form");
        CredentialForm form = (CredentialForm) model.getAttribute("form");
        assertThat(form).isNotNull();
        assertThat(form.getTeam()).isEqualTo("SECOPS");
        assertThat(model.getAttribute("mustSelectTeam")).isEqualTo(false);
    }

    @Test
    void createRequiresTeamSelectionWhenUserHasMultipleTeams() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS", "SECOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS", "SECOPS"))).thenReturn(Set.of("DEVOPS", "SECOPS"));
        CredentialForm form = new CredentialForm();
        form.setName("mail");
        form.setCreatedBy("ana");
        form.setUsername("u");
        form.setPassword("p");
        form.setTeam(" ");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.create(form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("credentials/form");
        assertThat(binding.hasFieldErrors("team")).isTrue();
        verify(credentialService, never()).create(form, "ana");
    }

    @Test
    void createAutoAssignsSingleTeamWhenMissing() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        CredentialForm form = new CredentialForm();
        form.setName("mail");
        form.setCreatedBy("ana");
        form.setUsername("u");
        form.setPassword("p");
        form.setTeam(" ");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.create(form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("redirect:/credentials");
        ArgumentCaptor<CredentialForm> formCaptor = ArgumentCaptor.forClass(CredentialForm.class);
        verify(credentialService).create(formCaptor.capture(), org.mockito.ArgumentMatchers.eq("ana"));
        assertThat(formCaptor.getValue().getTeam()).isEqualTo("DEVOPS");
    }

    @Test
    void updateRejectsNonOwnerWhenNotAdmin() {
        when(authentication.getName()).thenReturn("ana");
        Collection<? extends GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_APP_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        Credential credential = new Credential();
        credential.setCreatedBy("otro");
        when(credentialService.findByIdVisibleForTeams(9L, Set.of("DEVOPS"))).thenReturn(credential);

        CredentialForm form = new CredentialForm();
        form.setName("mail");
        form.setCreatedBy("ana");
        form.setUsername("u");
        form.setPassword("p");
        form.setTeam("DEVOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.update(9L, form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("redirect:/credentials");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
        verify(credentialService, never()).update(9L, form, "ana");
    }

    @Test
    void deleteAllowsOwnerWhenNotAdmin() {
        when(authentication.getName()).thenReturn("ana");
        Collection<? extends GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_APP_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        Credential credential = new Credential();
        credential.setCreatedBy("ana");
        when(credentialService.findByIdVisibleForTeams(11L, Set.of("DEVOPS"))).thenReturn(credential);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = credentialWebController.delete(11L, authentication, redirect);

        assertThat(view).isEqualTo("redirect:/credentials");
        assertThat(redirect.getFlashAttributes()).containsKey("message");
        verify(credentialService).delete(11L);
    }
}
