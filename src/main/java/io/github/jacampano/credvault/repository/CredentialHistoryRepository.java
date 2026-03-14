package io.github.jacampano.credvault.repository;

import io.github.jacampano.credvault.domain.CredentialHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CredentialHistoryRepository extends JpaRepository<CredentialHistory, Long> {
    List<CredentialHistory> findByCredentialIdOrderByEditedAtDesc(Long credentialId);

    void deleteByCredentialId(Long credentialId);
}
