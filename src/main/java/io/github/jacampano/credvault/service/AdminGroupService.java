package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.auth.Group;
import io.github.jacampano.credvault.dto.admin.GroupForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminGroupService {

    private final GroupRepository groupRepository;
    private final AppUserRepository appUserRepository;

    public AdminGroupService(GroupRepository groupRepository,
                            AppUserRepository appUserRepository) {
        this.groupRepository = groupRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<Group> findAll() {
        return groupRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Set<String> findAllGroupNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Group group : findAll()) {
            names.add(group.getName());
        }
        return names;
    }

    @Transactional(readOnly = true)
    public Group findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado: " + id));
    }

    @Transactional
    public void createGroup(GroupForm form) {
        String name = normalize(form.getName());
        if (groupRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe un grupo con ese nombre");
        }

        Group group = new Group();
        group.setName(name);
        group.setDescription(normalizeNullable(form.getDescription()));
        group.setOauthSynchronized(false);
        groupRepository.save(group);
    }

    @Transactional
    public void updateGroup(Long id, GroupForm form) {
        Group group = findById(id);
        String name = normalize(form.getName());
        if (groupRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Ya existe otro grupo con ese nombre");
        }
        group.setName(name);
        group.setDescription(normalizeNullable(form.getDescription()));
        groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = findById(id);
        long members = appUserRepository.countMembersByGroup(group.getName());
        if (members > 0) {
            throw new IllegalArgumentException("No se puede eliminar el grupo porque tiene miembros asignados");
        }
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public GroupForm toForm(Group group) {
        GroupForm form = new GroupForm();
        form.setName(group.getName());
        form.setDescription(group.getDescription());
        return form;
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
