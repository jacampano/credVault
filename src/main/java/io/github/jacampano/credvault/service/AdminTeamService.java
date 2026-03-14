package io.github.jacampano.credvault.service;

import io.github.jacampano.credvault.domain.auth.Team;
import io.github.jacampano.credvault.dto.admin.TeamForm;
import io.github.jacampano.credvault.repository.auth.AppUserRepository;
import io.github.jacampano.credvault.repository.auth.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminTeamService {

    private final TeamRepository teamRepository;
    private final AppUserRepository appUserRepository;

    public AdminTeamService(TeamRepository teamRepository,
                            AppUserRepository appUserRepository) {
        this.teamRepository = teamRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<Team> findAll() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Set<String> findAllTeamNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Team team : findAll()) {
            names.add(team.getName());
        }
        return names;
    }

    @Transactional(readOnly = true)
    public Team findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipo no encontrado: " + id));
    }

    @Transactional
    public void createTeam(TeamForm form) {
        String name = normalize(form.getName());
        if (teamRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe un equipo con ese nombre");
        }

        Team team = new Team();
        team.setName(name);
        team.setDescription(normalizeNullable(form.getDescription()));
        teamRepository.save(team);
    }

    @Transactional
    public void updateTeam(Long id, TeamForm form) {
        Team team = findById(id);
        String name = normalize(form.getName());
        if (teamRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Ya existe otro equipo con ese nombre");
        }
        team.setName(name);
        team.setDescription(normalizeNullable(form.getDescription()));
        teamRepository.save(team);
    }

    @Transactional
    public void deleteTeam(Long id) {
        Team team = findById(id);
        long members = appUserRepository.countMembersByTeam(team.getName());
        if (members > 0) {
            throw new IllegalArgumentException("No se puede eliminar el equipo porque tiene miembros asignados");
        }
        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    public TeamForm toForm(Team team) {
        TeamForm form = new TeamForm();
        form.setName(team.getName());
        form.setDescription(team.getDescription());
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
