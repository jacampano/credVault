package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.auth.AppRole;
import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.domain.auth.UserIdentitySource;
import io.github.jacampano.credvault.dto.admin.UserAdminForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AppUserRepository appUserRepository,
                            GroupRepository groupRepository,
                            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<AppUser> listUsers(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return appUserRepository.findAll(pageable);
        }

        String q = query.trim();
        return appUserRepository.findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                q, q, q, q, pageable);
    }

    @Transactional(readOnly = true)
    public AppUser findById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public UserAdminForm toForm(AppUser user) {
        UserAdminForm form = new UserAdminForm();
        form.setUsername(user.getUsername());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setEnabled(user.isEnabled());
        form.setAppUserRole(user.getRoles().contains(AppRole.APP_USER));
        form.setAdminRole(user.getRoles().contains(AppRole.ADMIN));
        form.setSelectedGroups(new LinkedHashSet<>(user.getGroups()));
        return form;
    }

    @Transactional
    public void createUser(UserAdminForm form) {
        String username = form.getUsername().trim();
        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre de usuario");
        }

        Set<AppRole> roles = EnumSet.noneOf(AppRole.class);
        if (form.isAppUserRole()) {
            roles.add(AppRole.APP_USER);
        }
        if (form.isAdminRole()) {
            roles.add(AppRole.ADMIN);
        }

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol");
        }
        Set<String> groups = normalizeGroups(form.getSelectedGroups());
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe pertenecer al menos a un grupo");
        }
        validateGroupsExist(groups);
        if (!StringUtils.hasText(form.getNewPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria para crear el usuario");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFirstName(trimToNull(form.getFirstName()));
        user.setLastName(trimToNull(form.getLastName()));
        user.setEmail(trimToNull(form.getEmail()));
        user.setEnabled(form.isEnabled());
        user.setRoles(roles);
        user.setGroups(groups);
        user.setIdentitySource(UserIdentitySource.LOCAL);
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword().trim()));
        appUserRepository.save(user);
    }

    @Transactional
    public void updateUser(Long userId, UserAdminForm form, String actorUsername) {
        AppUser user = findById(userId);
        ensureLocalUserManagement(user);

        String username = form.getUsername().trim();
        if (appUserRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new IllegalArgumentException("Ya existe otro usuario con ese nombre de usuario");
        }

        Set<AppRole> roles = EnumSet.noneOf(AppRole.class);
        if (form.isAppUserRole()) {
            roles.add(AppRole.APP_USER);
        }
        if (form.isAdminRole()) {
            roles.add(AppRole.ADMIN);
        }

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol");
        }
        Set<String> groups = normalizeGroups(form.getSelectedGroups());
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe pertenecer al menos a un grupo");
        }
        validateGroupsExist(groups);

        if (isLastAdminBeingDemoted(user, roles)) {
            throw new IllegalArgumentException("No puedes quitar el rol ADMIN al último administrador");
        }
        if (sameUsername(user.getUsername(), actorUsername) && !form.isEnabled()) {
            throw new IllegalArgumentException("No puedes desactivar tu propio usuario");
        }

        user.setUsername(username);
        user.setFirstName(trimToNull(form.getFirstName()));
        user.setLastName(trimToNull(form.getLastName()));
        user.setEmail(trimToNull(form.getEmail()));
        user.setEnabled(form.isEnabled());
        user.setRoles(roles);
        user.setGroups(groups);

        if (StringUtils.hasText(form.getNewPassword())) {
            user.setPasswordHash(passwordEncoder.encode(form.getNewPassword().trim()));
        }

        appUserRepository.save(user);
    }

    @Transactional
    public void toggleUserStatus(Long userId, String actorUsername) {
        AppUser user = findById(userId);
        ensureLocalUserManagement(user);
        boolean targetEnabled = !user.isEnabled();

        if (!targetEnabled && sameUsername(user.getUsername(), actorUsername)) {
            throw new IllegalArgumentException("No puedes desactivar tu propio usuario");
        }

        if (!targetEnabled && isLastAdmin(user)) {
            throw new IllegalArgumentException("No puedes desactivar el último administrador");
        }

        user.setEnabled(targetEnabled);
        appUserRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId, String actorUsername) {
        AppUser user = findById(userId);
        ensureLocalUserManagement(user);

        if (sameUsername(user.getUsername(), actorUsername)) {
            throw new IllegalArgumentException("No puedes eliminar tu propio usuario");
        }

        if (isLastAdmin(user)) {
            throw new IllegalArgumentException("No puedes eliminar el último administrador");
        }

        appUserRepository.delete(user);
    }

    private boolean isLastAdminBeingDemoted(AppUser user, Set<AppRole> newRoles) {
        boolean hadAdmin = user.getRoles().contains(AppRole.ADMIN);
        boolean willHaveAdmin = newRoles.contains(AppRole.ADMIN);
        return hadAdmin && !willHaveAdmin && adminCount() <= 1;
    }

    private boolean isLastAdmin(AppUser user) {
        return user.getRoles().contains(AppRole.ADMIN) && adminCount() <= 1;
    }

    private long adminCount() {
        return appUserRepository.findAll().stream()
                .filter(appUser -> appUser.getRoles().contains(AppRole.ADMIN))
                .count();
    }

    private boolean sameUsername(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureLocalUserManagement(AppUser user) {
        if (user.isExternallyManaged()) {
            throw new IllegalArgumentException("No puedes modificar un usuario gestionado por " + user.getIdentitySourceLabel());
        }
    }

    private Set<String> normalizeGroups(Set<String> selectedGroups) {
        if (selectedGroups == null || selectedGroups.isEmpty()) {
            return Set.of();
        }
        return selectedGroups.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateGroupsExist(Set<String> groups) {
        Set<String> availableGroups = groupRepository.findAllByOrderByNameAsc().stream()
                .map(group -> group.getName().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        boolean invalidGroupFound = groups.stream()
                .map(group -> group.toLowerCase(Locale.ROOT))
                .anyMatch(group -> !availableGroups.contains(group));
        if (invalidGroupFound) {
            throw new IllegalArgumentException("Hay grupos seleccionados que no existen");
        }
    }
}
