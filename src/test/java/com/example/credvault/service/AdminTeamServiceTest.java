package com.example.credvault.service;

import com.example.credvault.domain.auth.Team;
import com.example.credvault.dto.admin.TeamForm;
import com.example.credvault.repository.auth.AppUserRepository;
import com.example.credvault.repository.auth.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AdminTeamService adminTeamService;

    @Test
    void createTeamSavesWhenValid() {
        TeamForm form = new TeamForm();
        form.setName(" DEVOPS ");
        form.setDescription(" Equipo devops ");
        when(teamRepository.existsByNameIgnoreCase("DEVOPS")).thenReturn(false);

        adminTeamService.createTeam(form);

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("DEVOPS");
        assertThat(captor.getValue().getDescription()).isEqualTo("Equipo devops");
    }

    @Test
    void createTeamFailsWhenDuplicated() {
        TeamForm form = new TeamForm();
        form.setName("DEVOPS");
        when(teamRepository.existsByNameIgnoreCase("DEVOPS")).thenReturn(true);

        assertThatThrownBy(() -> adminTeamService.createTeam(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un equipo");
    }

    @Test
    void deleteTeamFailsWhenHasMembers() {
        Team team = new Team();
        team.setId(3L);
        team.setName("DEVOPS");
        when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
        when(appUserRepository.countMembersByTeam("DEVOPS")).thenReturn(2L);

        assertThatThrownBy(() -> adminTeamService.deleteTeam(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tiene miembros");
    }

    @Test
    void deleteTeamRemovesWhenNoMembers() {
        Team team = new Team();
        team.setId(3L);
        team.setName("DEVOPS");
        when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
        when(appUserRepository.countMembersByTeam("DEVOPS")).thenReturn(0L);

        adminTeamService.deleteTeam(3L);

        verify(teamRepository).delete(team);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTeamService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAllTeamNamesReturnsOrderedSet() {
        Team a = new Team();
        a.setName("A");
        Team b = new Team();
        b.setName("B");
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(a, b));

        assertThat(adminTeamService.findAllTeamNames()).containsExactly("A", "B");
    }
}
