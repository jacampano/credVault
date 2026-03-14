package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.auth.AppRole;
import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.domain.auth.Team;
import io.github.jacampano.credvault.dto.admin.UserAdminForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void listUsersWithoutQueryUsesFindAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(appUserRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        adminUserService.listUsers(" ", pageable);

        verify(appUserRepository).findAll(pageable);
        verify(appUserRepository, never()).findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(any(), any(), any(), any(), any());
    }

    @Test
    void listUsersWithQueryUsesSearchAcrossFields() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(appUserRepository.findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase("ana", "ana", "ana", "ana", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        adminUserService.listUsers(" ana ", pageable);

        verify(appUserRepository).findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase("ana", "ana", "ana", "ana", pageable);
    }

    @Test
    void findByIdThrowsWhenUserMissing() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUserUpdatesFieldsRolesAndPassword() {
        AppUser user = user(1L, "old", true, Set.of(AppRole.APP_USER));
        user.setPasswordHash("old-hash");

        UserAdminForm form = new UserAdminForm();
        form.setUsername("newuser");
        form.setFirstName("Ana");
        form.setLastName("Lopez");
        form.setEmail("ana@example.com");
        form.setEnabled(true);
        form.setAppUserRole(true);
        form.setAdminRole(true);
        form.setNewPassword("new-pass");
        form.setSelectedTeams(Set.of("SECOPS"));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.existsByUsernameAndIdNot("newuser", 1L)).thenReturn(false);
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(team("SECOPS")));
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-pass");

        adminUserService.updateUser(1L, form, "other-admin");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getLastName()).isEqualTo("Lopez");
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactlyInAnyOrder(AppRole.APP_USER, AppRole.ADMIN);
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-pass");
    }

    @Test
    void createUserCreatesAndSavesUserWhenValid() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new.user");
        form.setFirstName("Ana");
        form.setLastName("Garcia");
        form.setEmail("ana@acme.test");
        form.setEnabled(true);
        form.setAppUserRole(true);
        form.setAdminRole(false);
        form.setNewPassword(" secret ");
        form.setSelectedTeams(Set.of("DEVOPS", "BACKOFFICE"));

        when(appUserRepository.existsByUsername("new.user")).thenReturn(false);
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(team("BACKOFFICE"), team("DEVOPS")));
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        adminUserService.createUser(form);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("new.user");
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getLastName()).isEqualTo("Garcia");
        assertThat(saved.getEmail()).isEqualTo("ana@acme.test");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactly(AppRole.APP_USER);
        assertThat(saved.getTeams()).containsExactlyInAnyOrder("DEVOPS", "BACKOFFICE");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void createUserFailsWhenUsernameExists() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("dup");
        form.setAppUserRole(true);
        form.setNewPassword("x");
        form.setSelectedTeams(Set.of("TEAM1"));

        when(appUserRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un usuario");
    }

    @Test
    void createUserFailsWhenNoRoleSelected() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new");
        form.setAppUserRole(false);
        form.setAdminRole(false);
        form.setNewPassword("x");
        form.setSelectedTeams(Set.of("TEAM1"));
        when(appUserRepository.existsByUsername("new")).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.createUser(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un rol");
    }

    @Test
    void createUserFailsWhenPasswordMissing() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new");
        form.setAppUserRole(true);
        form.setNewPassword(" ");
        form.setSelectedTeams(Set.of("TEAM1"));
        when(appUserRepository.existsByUsername("new")).thenReturn(false);
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(team("TEAM1")));

        assertThatThrownBy(() -> adminUserService.createUser(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraseña es obligatoria");
    }

    @Test
    void createUserFailsWhenTeamsMissing() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new");
        form.setAppUserRole(true);
        form.setNewPassword("x");
        form.setSelectedTeams(Set.of());
        when(appUserRepository.existsByUsername("new")).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.createUser(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos a un equipo");
    }

    @Test
    void createUserFailsWhenSelectingUnknownTeam() {
        UserAdminForm form = new UserAdminForm();
        form.setUsername("new");
        form.setAppUserRole(true);
        form.setNewPassword("x");
        form.setSelectedTeams(Set.of("TEAM-X"));
        when(appUserRepository.existsByUsername("new")).thenReturn(false);
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(team("TEAM-1")));

        assertThatThrownBy(() -> adminUserService.createUser(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existen");
    }

    @Test
    void updateUserFailsWhenUsernameAlreadyExists() {
        AppUser user = user(1L, "old", true, Set.of(AppRole.APP_USER));
        UserAdminForm form = new UserAdminForm();
        form.setUsername("duplicado");
        form.setAppUserRole(true);
        form.setEnabled(true);
        form.setSelectedTeams(Set.of("TEAM1"));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.existsByUsernameAndIdNot("duplicado", 1L)).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.updateUser(1L, form, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe otro usuario");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void updateUserFailsWhenNoRolesSelected() {
        AppUser user = user(1L, "old", true, Set.of(AppRole.APP_USER));
        UserAdminForm form = new UserAdminForm();
        form.setUsername("user");
        form.setEnabled(true);
        form.setAppUserRole(false);
        form.setAdminRole(false);
        form.setSelectedTeams(Set.of("TEAM1"));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.existsByUsernameAndIdNot("user", 1L)).thenReturn(false);

        assertThatThrownBy(() -> adminUserService.updateUser(1L, form, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un rol");
    }

    @Test
    void updateUserFailsWhenDemotingLastAdmin() {
        AppUser user = user(1L, "admin", true, Set.of(AppRole.ADMIN));
        UserAdminForm form = new UserAdminForm();
        form.setUsername("admin");
        form.setEnabled(true);
        form.setAppUserRole(true);
        form.setAdminRole(false);
        form.setSelectedTeams(Set.of("TEAM1"));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.existsByUsernameAndIdNot("admin", 1L)).thenReturn(false);
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(team("TEAM1")));
        when(appUserRepository.findAll()).thenReturn(List.of(user));

        assertThatThrownBy(() -> adminUserService.updateUser(1L, form, "otro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("último administrador");
    }

    @Test
    void updateUserFailsWhenSelfDisable() {
        AppUser user = user(1L, "admin", true, Set.of(AppRole.ADMIN));
        UserAdminForm form = new UserAdminForm();
        form.setUsername("admin");
        form.setEnabled(false);
        form.setAppUserRole(true);
        form.setAdminRole(true);
        form.setSelectedTeams(Set.of("TEAM1"));

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.existsByUsernameAndIdNot("admin", 1L)).thenReturn(false);
        when(teamRepository.findAllByOrderByNameAsc()).thenReturn(List.of(team("TEAM1")));

        assertThatThrownBy(() -> adminUserService.updateUser(1L, form, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes desactivar tu propio usuario");
    }

    @Test
    void toggleUserStatusTogglesAndSaves() {
        AppUser user = user(1L, "user", true, Set.of(AppRole.APP_USER));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.toggleUserStatus(1L, "admin");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();
    }

    @Test
    void toggleUserStatusFailsWhenSelfDeactivate() {
        AppUser user = user(1L, "admin", true, Set.of(AppRole.ADMIN));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.toggleUserStatus(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes desactivar tu propio usuario");
    }

    @Test
    void deleteUserFailsWhenSelfDelete() {
        AppUser user = user(1L, "admin", true, Set.of(AppRole.ADMIN));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.deleteUser(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes eliminar tu propio usuario");

        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void deleteUserFailsWhenDeletingLastAdmin() {
        AppUser user = user(1L, "admin", true, Set.of(AppRole.ADMIN));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.findAll()).thenReturn(List.of(user));

        assertThatThrownBy(() -> adminUserService.deleteUser(1L, "otro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("último administrador");
    }

    @Test
    void deleteUserDeletesWhenAllowed() {
        AppUser user = user(1L, "user", true, Set.of(AppRole.APP_USER));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.deleteUser(1L, "admin");

        verify(appUserRepository).delete(user);
    }

    private AppUser user(Long id, String username, boolean enabled, Set<AppRole> roles) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setEnabled(enabled);
        user.setRoles(roles);
        return user;
    }

    private Team team(String name) {
        Team team = new Team();
        team.setName(name);
        return team;
    }
}
