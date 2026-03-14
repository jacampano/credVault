package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.admin.InformationSystemForm;
import io.github.jacampano.credvault.repository.catalog.InformationComponentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationSystemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminInformationSystemService {

    private final InformationSystemRepository informationSystemRepository;
    private final InformationComponentRepository informationComponentRepository;

    public AdminInformationSystemService(InformationSystemRepository informationSystemRepository,
                                         InformationComponentRepository informationComponentRepository) {
        this.informationSystemRepository = informationSystemRepository;
        this.informationComponentRepository = informationComponentRepository;
    }

    @Transactional(readOnly = true)
    public List<InformationSystem> findAll() {
        return informationSystemRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public InformationSystem findById(Long id) {
        return informationSystemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema de información no encontrado: " + id));
    }

    @Transactional
    public void create(InformationSystemForm form) {
        String name = normalize(form.getName());
        String identifier = normalize(form.getIdentifier());
        validateUniqueness(name, identifier, null);

        InformationSystem system = new InformationSystem();
        system.setName(name);
        system.setIdentifier(identifier);
        system.setDescription(normalizeNullable(form.getDescription()));
        informationSystemRepository.save(system);
    }

    @Transactional
    public void update(Long id, InformationSystemForm form) {
        InformationSystem system = findById(id);
        String name = normalize(form.getName());
        String identifier = normalize(form.getIdentifier());
        validateUniqueness(name, identifier, id);

        system.setName(name);
        system.setIdentifier(identifier);
        system.setDescription(normalizeNullable(form.getDescription()));
        informationSystemRepository.save(system);
    }

    @Transactional
    public void delete(Long id) {
        InformationSystem system = findById(id);
        long components = informationComponentRepository.countByInformationSystemId(id);
        if (components > 0) {
            throw new IllegalArgumentException("No se puede eliminar el sistema porque tiene componentes asignados");
        }
        informationSystemRepository.delete(system);
    }

    @Transactional(readOnly = true)
    public InformationSystemForm toForm(InformationSystem system) {
        InformationSystemForm form = new InformationSystemForm();
        form.setName(system.getName());
        form.setIdentifier(system.getIdentifier());
        form.setDescription(system.getDescription());
        return form;
    }

    private void validateUniqueness(String name, String identifier, Long id) {
        boolean duplicatedName = id == null
                ? informationSystemRepository.existsByNameIgnoreCase(name)
                : informationSystemRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicatedName) {
            throw new IllegalArgumentException("Ya existe un sistema de información con ese nombre");
        }
        boolean duplicatedIdentifier = id == null
                ? informationSystemRepository.existsByIdentifierIgnoreCase(identifier)
                : informationSystemRepository.existsByIdentifierIgnoreCaseAndIdNot(identifier, id);
        if (duplicatedIdentifier) {
            throw new IllegalArgumentException("Ya existe un sistema de información con ese identificador");
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
