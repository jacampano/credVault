package com.example.credvault.repository;

import com.example.credvault.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByTeamInOrderByUpdatedAtDesc(Collection<String> teams);

    Optional<Credential> findByIdAndTeamIn(Long id, Collection<String> teams);
}
