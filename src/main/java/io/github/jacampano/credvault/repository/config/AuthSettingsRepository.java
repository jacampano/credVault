package io.github.jacampano.credvault.repository.config;

import io.github.jacampano.credvault.domain.config.AuthSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSettingsRepository extends JpaRepository<AuthSettings, Long> {
}
