package io.github.jacampano.credvault.security;

import io.github.jacampano.credvault.domain.auth.AppRole;
import io.github.jacampano.credvault.domain.auth.AppUser;
import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LocalAdminBootstrap implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LocalAdminBootstrap.class);

    private final AuthSettingsService authSettingsService;
    private final AuthProperties authProperties;
    private final AppUserRepository appUserRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalAdminBootstrap(AuthSettingsService authSettingsService,
                               AuthProperties authProperties,
                               AppUserRepository appUserRepository,
                               GroupRepository groupRepository,
                               PasswordEncoder passwordEncoder) {
        this.authSettingsService = authSettingsService;
        this.authProperties = authProperties;
        this.appUserRepository = appUserRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (authSettingsService.loadEffectiveSettings().mode() != AuthMode.local) {
            return;
        }

        String username = authProperties.getLocalAdminUsername();
        String password = authProperties.getLocalAdminPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            LOG.info("No se crea admin local automatico: APP_AUTH_LOCAL_ADMIN_USERNAME/PASSWORD no informados");
            if (appUserRepository.count() == 0) {
                LOG.warn("No hay usuarios locales en la BBDD. Define APP_AUTH_LOCAL_ADMIN_USERNAME y APP_AUTH_LOCAL_ADMIN_PASSWORD para poder acceder.");
            }
            return;
        }

        appUserRepository.findByUsername(username).ifPresentOrElse(existing ->
                        LOG.info("Usuario admin local ya existente: {}", username),
                () -> {
                    ensureAdminGroupExists();
                    AppUser admin = new AppUser();
                    admin.setUsername(username);
                    admin.setPasswordHash(passwordEncoder.encode(password));
                    admin.setRoles(Set.of(AppRole.APP_USER, AppRole.ADMIN));
                    admin.setGroups(Set.of("ADMIN"));
                    admin.setEnabled(true);
                    appUserRepository.save(admin);
                    LOG.info("Usuario admin local creado: {}", username);
                });
    }

    private void ensureAdminGroupExists() {
        if (groupRepository.existsByNameIgnoreCase("ADMIN")) {
            return;
        }
        Group group = new Group();
        group.setName("ADMIN");
        group.setDescription("Grupo administrativo");
        groupRepository.save(group);
    }
}
