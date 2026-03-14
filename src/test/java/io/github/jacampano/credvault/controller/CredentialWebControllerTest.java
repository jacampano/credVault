package io.github.jacampano.credvault.controller;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.CredentialForm;
import io.github.jacampano.credvault.service.CredentialService;
import io.github.jacampano.credvault.service.UserAccessService;
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
    void listLoadsVisibleCredentialsForUser() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        when(credentialService.findAvailableComponents()).thenReturn(List.of());
        when(credentialService.findAvailableEnvironments()).thenReturn(List.of());
        when(credentialService.supportedTypes()).thenReturn(List.of(CredentialType.WEB_USER_PASSWORD, CredentialType.TOKEN));
        when(credentialService.findAllVisibleForUser("ana", Set.of("DEVOPS"))).thenReturn(List.of(new Credential()));
        Model model = new ExtendedModelMap();

        String view = credentialWebController.list(null, null, null, null, null, null, null, null, null, model, authentication);

        assertThat(view).isEqualTo("credentials/list");
        verify(credentialService).findAllVisibleForUser("ana", Set.of("DEVOPS"));
        assertThat(model.getAttribute("sort")).isEqualTo("updatedAt");
        assertThat(model.getAttribute("dir")).isEqualTo("desc");
    }

    @Test
    void listSortsByIdentifierAscending() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        when(credentialService.findAvailableComponents()).thenReturn(List.of());
        when(credentialService.findAvailableEnvironments()).thenReturn(List.of());
        when(credentialService.supportedTypes()).thenReturn(List.of(CredentialType.WEB_USER_PASSWORD, CredentialType.TOKEN));

        Credential b = new Credential();
        b.setIdentifier("zzz");
        Credential a = new Credential();
        a.setIdentifier("aaa");
        when(credentialService.findAllVisibleForUser("ana", Set.of("DEVOPS"))).thenReturn(List.of(b, a));
        Model model = new ExtendedModelMap();

        String view = credentialWebController.list("identifier", "asc", null, null, null, null, null, null, null, model, authentication);

        assertThat(view).isEqualTo("credentials/list");
        @SuppressWarnings("unchecked")
        List<Credential> credentials = (List<Credential>) model.getAttribute("credentials");
        assertThat(credentials).extracting(Credential::getIdentifier).containsExactly("aaa", "zzz");
    }

    @Test
    void listAppliesMultiFilters() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS", "SECOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS", "SECOPS"))).thenReturn(Set.of("DEVOPS", "SECOPS"));
        when(credentialService.supportedTypes()).thenReturn(List.of(CredentialType.WEB_USER_PASSWORD, CredentialType.TOKEN));

        InformationSystem erp = new InformationSystem();
        erp.setIdentifier("ERP");
        InformationSystem crm = new InformationSystem();
        crm.setIdentifier("CRM");
        InformationComponent api = new InformationComponent();
        api.setIdentifier("API");
        api.setInformationSystem(erp);
        InformationComponent backoffice = new InformationComponent();
        backoffice.setIdentifier("BACK");
        backoffice.setInformationSystem(crm);
        AppEnvironment pro = new AppEnvironment();
        pro.setIdentifier("PRO");
        AppEnvironment pre = new AppEnvironment();
        pre.setIdentifier("PRE");
        when(credentialService.findAvailableComponents()).thenReturn(List.of(api, backoffice));
        when(credentialService.findAvailableEnvironments()).thenReturn(List.of(pro, pre));

        Credential match = new Credential();
        match.setIdentifier("ok");
        match.setInformationComponent(api);
        match.setEnvironment(pro);
        match.setType(CredentialType.TOKEN);
        match.setTeams(Set.of("DEVOPS"));
        match.setShared(true);

        Credential filteredOut = new Credential();
        filteredOut.setIdentifier("no");
        filteredOut.setInformationComponent(backoffice);
        filteredOut.setEnvironment(pre);
        filteredOut.setType(CredentialType.WEB_USER_PASSWORD);
        filteredOut.setTeams(Set.of("SECOPS"));
        filteredOut.setShared(false);

        when(credentialService.findAllVisibleForUser("ana", Set.of("DEVOPS", "SECOPS")))
                .thenReturn(List.of(match, filteredOut));
        Model model = new ExtendedModelMap();

        String view = credentialWebController.list(
                "identifier",
                "asc",
                null,
                List.of("ERP"),
                List.of("API"),
                List.of("PRO"),
                List.of("TOKEN"),
                List.of("DEVOPS"),
                "yes",
                model,
                authentication
        );

        assertThat(view).isEqualTo("credentials/list");
        @SuppressWarnings("unchecked")
        List<Credential> credentials = (List<Credential>) model.getAttribute("credentials");
        assertThat(credentials).hasSize(1);
        assertThat(credentials.getFirst().getIdentifier()).isEqualTo("ok");
    }

    @Test
    void listAppliesIdentifierSearchFilter() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        when(credentialService.findAvailableComponents()).thenReturn(List.of());
        when(credentialService.findAvailableEnvironments()).thenReturn(List.of());
        when(credentialService.supportedTypes()).thenReturn(List.of(CredentialType.WEB_USER_PASSWORD, CredentialType.TOKEN));

        Credential first = new Credential();
        first.setIdentifier("token-api");
        Credential second = new Credential();
        second.setIdentifier("correo");
        when(credentialService.findAllVisibleForUser("ana", Set.of("DEVOPS"))).thenReturn(List.of(first, second));
        Model model = new ExtendedModelMap();

        String view = credentialWebController.list(
                "identifier",
                "asc",
                "API",
                null,
                null,
                null,
                null,
                null,
                null,
                model,
                authentication
        );

        assertThat(view).isEqualTo("credentials/list");
        @SuppressWarnings("unchecked")
        List<Credential> credentials = (List<Credential>) model.getAttribute("credentials");
        assertThat(credentials).extracting(Credential::getIdentifier).containsExactly("token-api");
        assertThat(model.getAttribute("q")).isEqualTo("API");
    }

    @Test
    void createAllowsPrivateCredentialWithoutTeams() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        CredentialForm form = validWebForm();
        form.setSelectedTeams(Set.of());
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.create(form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("redirect:/credentials");
        verify(credentialService).create(form, "ana");
    }

    @Test
    void createRejectsTeamOutsideUserMembership() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        CredentialForm form = validWebForm();
        form.setSelectedTeams(Set.of("SECOPS"));
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.create(form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("credentials/form");
        assertThat(binding.hasFieldErrors("selectedTeams")).isTrue();
        verify(credentialService, never()).create(form, "ana");
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
        when(credentialService.findByIdVisibleForUser(9L, "ana", Set.of("DEVOPS"))).thenReturn(credential);
        CredentialForm form = validWebForm();
        form.setSelectedTeams(Set.of("DEVOPS"));
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.update(9L, form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("redirect:/credentials");
        verify(credentialService, never()).update(9L, form, "ana");
    }

    @Test
    void createCallsServiceWithTokenTypeForm() {
        when(authentication.getName()).thenReturn("ana");
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        CredentialForm form = new CredentialForm();
        form.setIdentifier("api-token");
        form.setCreatedBy("ana");
        form.setType(CredentialType.TOKEN);
        form.setTokenValue("secret-token");
        form.setTokenNoExpiry(true);
        form.setComponentId(1L);
        form.setEnvironmentId(1L);
        form.setSelectedTeams(Set.of("DEVOPS"));
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Model model = new ExtendedModelMap();

        String view = credentialWebController.create(form, authentication, binding, redirect, model);

        assertThat(view).isEqualTo("redirect:/credentials");
        ArgumentCaptor<CredentialForm> captor = ArgumentCaptor.forClass(CredentialForm.class);
        verify(credentialService).create(captor.capture(), org.mockito.ArgumentMatchers.eq("ana"));
        assertThat(captor.getValue().getType()).isEqualTo(CredentialType.TOKEN);
    }

    @Test
    void deleteMovesCredentialToTrash() {
        when(authentication.getName()).thenReturn("ana");
        Collection<? extends GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_APP_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(userAccessService.getTeamsForUser("ana")).thenReturn(Set.of("DEVOPS"));
        when(credentialService.normalizeTeams(Set.of("DEVOPS"))).thenReturn(Set.of("DEVOPS"));
        Credential credential = new Credential();
        credential.setCreatedBy("ana");
        when(credentialService.findByIdVisibleForUser(7L, "ana", Set.of("DEVOPS"))).thenReturn(credential);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = credentialWebController.delete(7L, authentication, redirect);

        assertThat(view).isEqualTo("redirect:/credentials");
        verify(credentialService).delete(7L, "ana");
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Credencial enviada a la papelera");
    }

    private CredentialForm validWebForm() {
        CredentialForm form = new CredentialForm();
        form.setIdentifier("mail");
        form.setCreatedBy("ana");
        form.setType(CredentialType.WEB_USER_PASSWORD);
        form.setComponentId(1L);
        form.setEnvironmentId(1L);
        form.setWebUsername("u");
        form.setWebPassword("p");
        return form;
    }
}
