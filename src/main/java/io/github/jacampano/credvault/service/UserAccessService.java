package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class UserAccessService {

    private final AppUserRepository appUserRepository;

    public UserAccessService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Set<String> getTeamsForUser(String username) {
        return appUserRepository.findByUsername(username)
                .map(user -> new LinkedHashSet<>(user.getTeams()))
                .orElseGet(LinkedHashSet::new);
    }
}
