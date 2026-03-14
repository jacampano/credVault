package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.Credential;
import io.github.jacampano.credvault.domain.CredentialHistory;
import io.github.jacampano.credvault.domain.CredentialType;
import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.dto.CredentialForm;
import io.github.jacampano.credvault.repository.CredentialHistoryRepository;
import io.github.jacampano.credvault.repository.CredentialRepository;
import io.github.jacampano.credvault.repository.catalog.AppEnvironmentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationComponentRepository;
import io.github.jacampano.credvault.service.credential.CredentialTypeHandler;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final CredentialHistoryRepository credentialHistoryRepository;
    private final InformationComponentRepository informationComponentRepository;
    private final AppEnvironmentRepository appEnvironmentRepository;
    private final Map<CredentialType, CredentialTypeHandler> handlers = new EnumMap<>(CredentialType.class);

    public CredentialService(CredentialRepository credentialRepository,
                             CredentialHistoryRepository credentialHistoryRepository,
                             InformationComponentRepository informationComponentRepository,
                             AppEnvironmentRepository appEnvironmentRepository,
                             List<CredentialTypeHandler> handlers) {
        this.credentialRepository = credentialRepository;
        this.credentialHistoryRepository = credentialHistoryRepository;
        this.informationComponentRepository = informationComponentRepository;
        this.appEnvironmentRepository = appEnvironmentRepository;
        handlers.forEach(handler -> this.handlers.put(handler.supports(), handler));
    }

    public List<InformationComponent> findAvailableComponents() {
        return informationComponentRepository.findAllByOrderByNameAsc();
    }

    public List<AppEnvironment> findAvailableEnvironments() {
        return appEnvironmentRepository.findAllByOrderByNameAsc();
    }

    public List<Credential> findAllVisibleForUser(String username, Set<String> teams) {
        Set<String> normalizedTeams = normalizeTeams(teams);
        if (normalizedTeams.isEmpty()) {
            return credentialRepository.findByCreatedByAndDeletedFalseOrderByUpdatedAtDesc(username);
        }
        return credentialRepository.findVisibleForUser(username, normalizedTeams);
    }

    public Credential findByIdVisibleForUser(Long id, String username, Set<String> teams) {
        Set<String> normalizedTeams = normalizeTeams(teams);
        if (normalizedTeams.isEmpty()) {
            return credentialRepository.findByIdAndCreatedByAndDeletedFalse(id, username)
                    .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
        }
        return credentialRepository.findVisibleByIdForUser(id, username, normalizedTeams)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
    }

    public Set<String> normalizeTeams(Set<String> teams) {
        if (teams == null) {
            return Set.of();
        }
        return teams.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void validateByType(CredentialForm form, BindingResult bindingResult) {
        CredentialTypeHandler handler = getHandler(form.getType());
        handler.validate(form, bindingResult);
    }

    public Credential create(CredentialForm form, String createdBy) {
        Credential credential = new Credential();
        credential.setCreatedBy(createdBy);
        applyMutableFields(form, credential);
        return credentialRepository.save(credential);
    }

    @Transactional
    public Credential update(Long id, CredentialForm form, String editedBy) {
        Credential credential = credentialRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
        credentialHistoryRepository.save(toHistorySnapshot(credential, editedBy));
        applyMutableFields(form, credential);
        return credentialRepository.save(credential);
    }

    public List<CredentialHistory> findHistoryByCredentialId(Long credentialId) {
        return credentialHistoryRepository.findByCredentialIdOrderByEditedAtDesc(credentialId);
    }

    public List<Credential> findAllDeleted() {
        return credentialRepository.findByDeletedTrueOrderByDeletedAtDesc();
    }

    @Transactional
    public void restore(Long id) {
        Credential credential = credentialRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada en papelera: " + id));
        credential.setDeleted(false);
        credential.setDeletedAt(null);
        credential.setDeletedBy(null);
        credentialRepository.save(credential);
    }

    @Transactional
    public void deletePermanently(Long id) {
        Credential credential = credentialRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada en papelera: " + id));
        credentialHistoryRepository.deleteByCredentialId(id);
        credentialRepository.delete(credential);
    }

    public void delete(Long id) {
        Credential credential = credentialRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
        credential.setDeleted(true);
        credential.setDeletedAt(java.time.Instant.now());
        credential.setDeletedBy(credential.getCreatedBy());
        credentialRepository.save(credential);
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        Credential credential = credentialRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Credencial no encontrada: " + id));
        credential.setDeleted(true);
        credential.setDeletedAt(java.time.Instant.now());
        credential.setDeletedBy(deletedBy);
        credentialRepository.save(credential);
    }

    public CredentialForm toForm(Credential credential) {
        CredentialForm form = new CredentialForm();
        form.setIdentifier(credential.getIdentifier());
        form.setCreatedBy(credential.getCreatedBy());
        form.setComponentId(credential.getInformationComponent().getId());
        form.setEnvironmentId(credential.getEnvironment().getId());
        form.setSystemName(credential.getInformationComponent().getInformationSystem().getName());
        form.setType(credential.getType());
        form.setSelectedTeams(new LinkedHashSet<>(credential.getTeams()));
        form.setShared(credential.isShared());
        form.setNotes(credential.getNotes());
        getHandler(credential.getType()).fillForm(credential, form);
        return form;
    }

    public List<CredentialType> supportedTypes() {
        return List.of(CredentialType.WEB_USER_PASSWORD, CredentialType.TOKEN);
    }

    private void applyMutableFields(CredentialForm form, Credential credential) {
        InformationComponent component = informationComponentRepository.findById(form.getComponentId())
                .orElseThrow(() -> new EntityNotFoundException("Componente no encontrado: " + form.getComponentId()));
        AppEnvironment environment = appEnvironmentRepository.findById(form.getEnvironmentId())
                .orElseThrow(() -> new EntityNotFoundException("Entorno no encontrado: " + form.getEnvironmentId()));

        credential.setIdentifier(form.getIdentifier().trim());
        credential.setInformationComponent(component);
        credential.setEnvironment(environment);
        credential.setType(form.getType());
        Set<String> selectedTeams = normalizeTeams(form.getSelectedTeams());
        credential.setTeams(selectedTeams);
        credential.setShared(!selectedTeams.isEmpty());
        form.setShared(!selectedTeams.isEmpty());
        form.setSystemName(component.getInformationSystem().getName());
        credential.setNotes(StringUtils.hasText(form.getNotes()) ? form.getNotes().trim() : null);
        getHandler(form.getType()).applyToCredential(form, credential);
    }

    private CredentialHistory toHistorySnapshot(Credential credential, String editedBy) {
        CredentialHistory history = new CredentialHistory();
        history.setCredential(credential);
        history.setEditedBy(editedBy);
        history.setIdentifier(credential.getIdentifier());
        history.setCreatedBy(credential.getCreatedBy());
        history.setType(credential.getType());
        history.setTeams(new LinkedHashSet<>(credential.getTeams()));
        history.setShared(credential.isShared());
        history.setWebUsername(credential.getWebUsername());
        history.setWebPassword(credential.getWebPassword());
        history.setWebUrl(credential.getWebUrl());
        history.setTokenValue(credential.getTokenValue());
        history.setTokenUrl(credential.getTokenUrl());
        history.setTokenExpirationDate(credential.getTokenExpirationDate());
        history.setTokenNoExpiry(credential.isTokenNoExpiry());
        history.setNotes(credential.getNotes());
        return history;
    }

    private CredentialTypeHandler getHandler(CredentialType type) {
        CredentialType resolvedType = type == null ? CredentialType.WEB_USER_PASSWORD : type;
        CredentialTypeHandler handler = handlers.get(resolvedType);
        if (handler == null) {
            throw new IllegalArgumentException("Tipo de credencial no soportado: " + resolvedType);
        }
        return handler;
    }
}
