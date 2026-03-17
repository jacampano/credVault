package io.github.jacampano.credvault.security;

import io.github.jacampano.credvault.domain.auth.AppRole;
import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.domain.auth.UserIdentitySource;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class OAuthGroupSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthGroupSyncService.class);

    private final AppUserRepository appUserRepository;
    private final GroupRepository groupRepository;

    public OAuthGroupSyncService(AppUserRepository appUserRepository,
                                 GroupRepository groupRepository) {
        this.appUserRepository = appUserRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional
    public void syncUser(OAuth2User oauth2User, Collection<String> gitlabGroups) {
        if (oauth2User == null) {
            return;
        }

        String username = extractUsername(oauth2User);
        if (!StringUtils.hasText(username)) {
            LOG.warn("OAUTH_SYNC_SKIPPED reason=missing_username");
            return;
        }

        Set<String> normalizedGroups = normalizeGroups(gitlabGroups);
        ensureGroupsExist(normalizedGroups);

        AppUser user = appUserRepository.findByUsername(username)
                .orElseGet(() -> {
                    AppUser created = new AppUser();
                    created.setUsername(username);
                    created.setPasswordHash("{oauth}-" + UUID.randomUUID());
                    created.setEnabled(true);
                    created.setRoles(new LinkedHashSet<>(Set.of(AppRole.APP_USER)));
                    return created;
                });

        String fullName = firstNonBlank(readString(oauth2User, "name"), readString(oauth2User, "full_name"));
        NameParts nameParts = splitFullName(fullName);

        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        user.setEmail(firstNonBlank(readString(oauth2User, "email"), user.getEmail()));
        user.setGroups(normalizedGroups);
        user.setIdentitySource(UserIdentitySource.OAUTH);
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new LinkedHashSet<>(Set.of(AppRole.APP_USER)));
        }

        appUserRepository.save(user);
        LOG.info("OAUTH_USER_SYNC username={} groupsCount={}", username, normalizedGroups.size());
    }

    private void ensureGroupsExist(Set<String> groups) {
        for (String groupName : groups) {
            if (groupRepository.existsByNameIgnoreCase(groupName)) {
                continue;
            }
            Group group = new Group();
            group.setName(groupName);
            group.setDescription("Sincronizado automáticamente desde OAuth");
            group.setOauthSynchronized(true);
            groupRepository.save(group);
        }
    }

    private Set<String> normalizeGroups(Collection<String> groups) {
        Set<String> result = new LinkedHashSet<>();
        if (groups == null) {
            return result;
        }
        for (String group : groups) {
            if (StringUtils.hasText(group)) {
                result.add(group.trim());
            }
        }
        return result;
    }

    private String extractUsername(OAuth2User oauth2User) {
        return firstNonBlank(
                readString(oauth2User, "username"),
                readString(oauth2User, "preferred_username"),
                readString(oauth2User, "login"),
                oauth2User.getName()
        );
    }

    private String readString(OAuth2User user, String key) {
        Object value = user.getAttributes().get(key);
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue.trim();
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private NameParts splitFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return new NameParts(null, null);
        }
        String normalized = fullName.trim();
        int firstSpace = normalized.indexOf(' ');
        if (firstSpace < 0) {
            return new NameParts(normalized, null);
        }
        String firstName = normalized.substring(0, firstSpace).trim();
        String lastName = normalized.substring(firstSpace + 1).trim();
        return new NameParts(
                StringUtils.hasText(firstName) ? firstName : null,
                StringUtils.hasText(lastName) ? lastName : null
        );
    }

    private record NameParts(String firstName, String lastName) {
    }
}
