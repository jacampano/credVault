package com.example.credvault.service;

import com.example.credvault.domain.Credential;
import com.example.credvault.domain.CredentialHistory;
import com.example.credvault.dto.CredentialForm;
import com.example.credvault.repository.CredentialHistoryRepository;
import com.example.credvault.repository.CredentialRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final CredentialHistoryRepository credentialHistoryRepository;

    public CredentialService(CredentialRepository credentialRepository,
                             CredentialHistoryRepository credentialHistoryRepository) {
        this.credentialRepository = credentialRepository;
        this.credentialHistoryRepository = credentialHistoryRepository;
    }

    public List<Credential> findAllVisibleForTeams(Set<String> teams) {
        if (teams == null || teams.isEmpty()) {
            return List.of();
        }
        return credentialRepository.findByTeamInOrderByUpdatedAtDesc(teams);
    }

    public Credential findByIdVisibleForTeams(Long id, Set<String> teams) {
        if (teams == null || teams.isEmpty()) {
            throw new EntityNotFoundException("Credencial no encontrada: " + id);
        }
        return credentialRepository.findByIdAndTeamIn(id, teams)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
    }

    public Set<String> normalizeTeams(Set<String> teams) {
        if (teams == null) {
            return Set.of();
        }
        return teams.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public Credential create(CredentialForm form, String createdBy) {
        Credential credential = new Credential();
        applyMutableFields(form, credential);
        credential.setCreatedBy(createdBy);
        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential update(Long id, CredentialForm form, String editedBy) {
        Credential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
        credentialHistoryRepository.save(toHistorySnapshot(credential, editedBy));
        applyMutableFields(form, credential);
        return credentialRepository.save(credential);
    }

    public List<CredentialHistory> findHistoryByCredentialId(Long credentialId) {
        return credentialHistoryRepository.findByCredentialIdOrderByEditedAtDesc(credentialId);
    }

    public void delete(Long id) {
        credentialRepository.deleteById(id);
    }

    public CredentialForm toForm(Credential credential) {
        CredentialForm form = new CredentialForm();
        form.setName(credential.getName());
        form.setCreatedBy(credential.getCreatedBy());
        form.setTeam(credential.getTeam());
        form.setShared(credential.isShared());
        form.setUsername(credential.getUsername());
        form.setPassword(credential.getPassword());
        form.setUrl(credential.getUrl());
        form.setNotes(credential.getNotes());
        return form;
    }

    private void applyMutableFields(CredentialForm form, Credential credential) {
        credential.setName(form.getName());
        credential.setTeam(StringUtils.hasText(form.getTeam()) ? form.getTeam().trim() : null);
        credential.setShared(form.isShared());
        credential.setUsername(form.getUsername());
        credential.setPassword(form.getPassword());
        credential.setUrl(form.getUrl());
        credential.setNotes(form.getNotes());
    }

    private CredentialHistory toHistorySnapshot(Credential credential, String editedBy) {
        CredentialHistory history = new CredentialHistory();
        history.setCredential(credential);
        history.setEditedBy(editedBy);
        history.setName(credential.getName());
        history.setCreatedBy(credential.getCreatedBy());
        history.setTeam(credential.getTeam());
        history.setShared(credential.isShared());
        history.setUsername(credential.getUsername());
        history.setPassword(credential.getPassword());
        history.setUrl(credential.getUrl());
        history.setNotes(credential.getNotes());
        return history;
    }
}
