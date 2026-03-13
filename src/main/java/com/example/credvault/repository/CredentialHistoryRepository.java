package com.example.credvault.repository;

import com.example.credvault.domain.CredentialHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CredentialHistoryRepository extends JpaRepository<CredentialHistory, Long> {
    List<CredentialHistory> findByCredentialIdOrderByEditedAtDesc(Long credentialId);
}
