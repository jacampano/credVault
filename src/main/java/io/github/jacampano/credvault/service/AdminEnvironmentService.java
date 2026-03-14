package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import io.github.jacampano.credvault.dto.admin.AppEnvironmentForm;
import io.github.jacampano.credvault.repository.catalog.AppEnvironmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminEnvironmentService {

    private final AppEnvironmentRepository appEnvironmentRepository;

    public AdminEnvironmentService(AppEnvironmentRepository appEnvironmentRepository) {
        this.appEnvironmentRepository = appEnvironmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AppEnvironment> findAll() {
        return appEnvironmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public AppEnvironment findById(Long id) {
        return appEnvironmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entorno no encontrado: " + id));
    }

    @Transactional
    public void create(AppEnvironmentForm form) {
        String name = normalize(form.getName());
        String identifier = normalize(form.getIdentifier());
        validateUniqueness(name, identifier, null);

        AppEnvironment environment = new AppEnvironment();
        environment.setName(name);
        environment.setIdentifier(identifier);
        environment.setDescription(normalizeNullable(form.getDescription()));
        appEnvironmentRepository.save(environment);
    }

    @Transactional
    public void update(Long id, AppEnvironmentForm form) {
        AppEnvironment environment = findById(id);
        String name = normalize(form.getName());
        String identifier = normalize(form.getIdentifier());
        validateUniqueness(name, identifier, id);

        environment.setName(name);
        environment.setIdentifier(identifier);
        environment.setDescription(normalizeNullable(form.getDescription()));
        appEnvironmentRepository.save(environment);
    }

    @Transactional
    public void delete(Long id) {
        AppEnvironment environment = findById(id);
        appEnvironmentRepository.delete(environment);
    }

    @Transactional(readOnly = true)
    public AppEnvironmentForm toForm(AppEnvironment environment) {
        AppEnvironmentForm form = new AppEnvironmentForm();
        form.setName(environment.getName());
        form.setIdentifier(environment.getIdentifier());
        form.setDescription(environment.getDescription());
        return form;
    }

    private void validateUniqueness(String name, String identifier, Long id) {
        boolean duplicatedName = id == null
                ? appEnvironmentRepository.existsByNameIgnoreCase(name)
                : appEnvironmentRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicatedName) {
            throw new IllegalArgumentException("Ya existe un entorno con ese nombre");
        }
        boolean duplicatedIdentifier = id == null
                ? appEnvironmentRepository.existsByIdentifierIgnoreCase(identifier)
                : appEnvironmentRepository.existsByIdentifierIgnoreCaseAndIdNot(identifier, id);
        if (duplicatedIdentifier) {
            throw new IllegalArgumentException("Ya existe un entorno con ese identificador");
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
