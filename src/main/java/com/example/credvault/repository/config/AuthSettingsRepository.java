package com.example.credvault.repository.config;

import com.example.credvault.domain.config.AuthSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSettingsRepository extends JpaRepository<AuthSettings, Long> {
}
