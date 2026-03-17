package io.github.jacampano.credvault.security;

import io.github.jacampano.credvault.domain.auth.AppRole;
import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.domain.auth.UserIdentitySource;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.GroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthGroupSyncServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private OAuthGroupSyncService oauthGroupSyncService;

    @Test
    void syncUserCreatesMissingGroupsAndUser() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(),
                Map.of("username", "root", "name", "Administrador CredVault", "email", "root@example.com"),
                "username"
        );

        when(appUserRepository.findByUsername("root")).thenReturn(Optional.empty());
        when(groupRepository.existsByNameIgnoreCase("credvault_admin")).thenReturn(false);
        when(groupRepository.existsByNameIgnoreCase("si-plataforma-cicd")).thenReturn(false);

        oauthGroupSyncService.syncUser(oauth2User, Set.of("credvault_admin", "si-plataforma-cicd"));

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository, times(2)).save(groupCaptor.capture());
        assertThat(groupCaptor.getAllValues()).allMatch(Group::isOauthSynchronized);
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());

        AppUser saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("root");
        assertThat(saved.getEmail()).isEqualTo("root@example.com");
        assertThat(saved.getIdentitySource()).isEqualTo(UserIdentitySource.OAUTH);
        assertThat(saved.getRoles()).contains(AppRole.APP_USER);
        assertThat(saved.getGroups()).containsExactlyInAnyOrder("credvault_admin", "si-plataforma-cicd");
    }

    @Test
    void syncUserUpdatesExistingUserGroups() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(),
                Map.of("username", "user1", "name", "User One"),
                "username"
        );

        AppUser existing = new AppUser();
        existing.setUsername("user1");
        existing.setPasswordHash("hash");
        existing.setRoles(new LinkedHashSet<>(Set.of(AppRole.APP_USER, AppRole.ADMIN)));
        existing.setGroups(new LinkedHashSet<>(Set.of("old-group")));

        when(appUserRepository.findByUsername("user1")).thenReturn(Optional.of(existing));
        when(groupRepository.existsByNameIgnoreCase("new-group")).thenReturn(true);

        oauthGroupSyncService.syncUser(oauth2User, Set.of("new-group"));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getGroups()).containsExactly("new-group");
        assertThat(userCaptor.getValue().getIdentitySource()).isEqualTo(UserIdentitySource.OAUTH);
        assertThat(userCaptor.getValue().getRoles()).contains(AppRole.ADMIN);
        verify(groupRepository, atLeastOnce()).existsByNameIgnoreCase("new-group");
    }
}
