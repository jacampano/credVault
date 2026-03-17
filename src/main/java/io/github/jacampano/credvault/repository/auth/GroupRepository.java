package io.github.jacampano.credvault.repository.auth;

import io.github.jacampano.credvault.domain.auth.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Group> findAllByOrderByNameAsc();
}
