package com.example.credvault.controller;

import com.example.credvault.domain.auth.Team;
import com.example.credvault.dto.admin.TeamForm;
import com.example.credvault.service.AdminTeamService;
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
class AdminTeamControllerTest {

    @Mock
    private AdminTeamService adminTeamService;

    @InjectMocks
    private AdminTeamController adminTeamController;

    @Test
    void listTeamsLoadsView() {
        when(adminTeamService.findAll()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = adminTeamController.listTeams(model);

        assertThat(view).isEqualTo("admin/teams/list");
        assertThat(model.getAttribute("teams")).isNotNull();
    }

    @Test
    void createTeamRedirectsWhenSuccess() {
        TeamForm form = new TeamForm();
        form.setName("DEVOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminTeamController.createTeam(form, binding, redirect);

        assertThat(view).isEqualTo("redirect:/admin/teams");
        verify(adminTeamService).createTeam(form);
    }

    @Test
    void createTeamReturnsFormWhenBusinessError() {
        TeamForm form = new TeamForm();
        form.setName("DEVOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("exists")).when(adminTeamService).createTeam(form);

        String view = adminTeamController.createTeam(form, binding, redirect);

        assertThat(view).isEqualTo("admin/teams/create");
        assertThat(binding.hasErrors()).isTrue();
    }

    @Test
    void editTeamRedirectsWhenNotFound() {
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(adminTeamService.findById(9L)).thenThrow(new EntityNotFoundException("not found"));

        String view = adminTeamController.editTeam(9L, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/teams");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }

    @Test
    void editTeamLoadsFormWhenFound() {
        Team team = new Team();
        team.setId(2L);
        team.setName("DEVOPS");
        TeamForm form = new TeamForm();
        form.setName("DEVOPS");
        when(adminTeamService.findById(2L)).thenReturn(team);
        when(adminTeamService.toForm(team)).thenReturn(form);
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminTeamController.editTeam(2L, model, redirect);

        assertThat(view).isEqualTo("admin/teams/edit");
        assertThat(model.getAttribute("teamId")).isEqualTo(2L);
    }

    @Test
    void updateTeamRedirectsWhenSuccess() {
        TeamForm form = new TeamForm();
        form.setName("SECOPS");
        BindingResult binding = new BeanPropertyBindingResult(form, "form");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminTeamController.updateTeam(4L, form, binding, model, redirect);

        assertThat(view).isEqualTo("redirect:/admin/teams");
        verify(adminTeamService).updateTeam(4L, form);
    }

    @Test
    void deleteTeamRedirectsWithErrorWhenBusinessRuleFails() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("miembros")).when(adminTeamService).deleteTeam(5L);

        String view = adminTeamController.deleteTeam(5L, redirect);

        assertThat(view).isEqualTo("redirect:/admin/teams");
        assertThat(redirect.getFlashAttributes()).containsKey("error");
    }
}
