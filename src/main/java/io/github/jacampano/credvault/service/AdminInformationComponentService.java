package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import io.github.jacampano.credvault.dto.admin.InformationComponentForm;
import io.github.jacampano.credvault.repository.catalog.InformationComponentRepository;
import io.github.jacampano.credvault.repository.catalog.InformationSystemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminInformationComponentService {

    private final InformationComponentRepository informationComponentRepository;
    private final InformationSystemRepository informationSystemRepository;

    public AdminInformationComponentService(InformationComponentRepository informationComponentRepository,
                                            InformationSystemRepository informationSystemRepository) {
        this.informationComponentRepository = informationComponentRepository;
        this.informationSystemRepository = informationSystemRepository;
    }

    @Transactional(readOnly = true)
    public List<InformationComponent> findAll() {
        return informationComponentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public InformationComponent findById(Long id) {
        return informationComponentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Componente no encontrado: " + id));
    }

    @Transactional
    public void create(InformationComponentForm form) {
        String name = normalize(form.getName());
        String identifier = normalize(form.getIdentifier());
        validateUniqueness(name, identifier, null);

        InformationComponent component = new InformationComponent();
        applyForm(form, component, name, identifier);
        informationComponentRepository.save(component);
    }

    @Transactional
    public void update(Long id, InformationComponentForm form) {
        InformationComponent component = findById(id);
        String name = normalize(form.getName());
        String identifier = normalize(form.getIdentifier());
        validateUniqueness(name, identifier, id);

        applyForm(form, component, name, identifier);
        informationComponentRepository.save(component);
    }

    @Transactional
    public void delete(Long id) {
        InformationComponent component = findById(id);
        informationComponentRepository.delete(component);
    }

    @Transactional(readOnly = true)
    public InformationComponentForm toForm(InformationComponent component) {
        InformationComponentForm form = new InformationComponentForm();
        form.setName(component.getName());
        form.setIdentifier(component.getIdentifier());
        form.setInformationSystemId(component.getInformationSystem().getId());
        form.setDescription(component.getDescription());
        return form;
    }

    private void applyForm(InformationComponentForm form, InformationComponent component, String name, String identifier) {
        InformationSystem informationSystem = informationSystemRepository.findById(form.getInformationSystemId())
                .orElseThrow(() -> new EntityNotFoundException("Sistema de información no encontrado: " + form.getInformationSystemId()));
        component.setName(name);
        component.setIdentifier(identifier);
        component.setInformationSystem(informationSystem);
        component.setDescription(normalizeNullable(form.getDescription()));
    }

    private void validateUniqueness(String name, String identifier, Long id) {
        boolean duplicatedName = id == null
                ? informationComponentRepository.existsByNameIgnoreCase(name)
                : informationComponentRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicatedName) {
            throw new IllegalArgumentException("Ya existe un componente con ese nombre");
        }
        boolean duplicatedIdentifier = id == null
                ? informationComponentRepository.existsByIdentifierIgnoreCase(identifier)
                : informationComponentRepository.existsByIdentifierIgnoreCaseAndIdNot(identifier, id);
        if (duplicatedIdentifier) {
            throw new IllegalArgumentException("Ya existe un componente con ese identificador");
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
