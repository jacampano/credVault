package io.github.jacampano.credvault.repository.catalog;

import io.github.jacampano.credvault.domain.catalog.AppEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppEnvironmentRepository extends JpaRepository<AppEnvironment, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByIdentifierIgnoreCase(String identifier);

    boolean existsByIdentifierIgnoreCaseAndIdNot(String identifier, Long id);

    List<AppEnvironment> findAllByOrderByNameAsc();
}
