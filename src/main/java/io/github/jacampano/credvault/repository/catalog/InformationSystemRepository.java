package io.github.jacampano.credvault.repository.catalog;

import io.github.jacampano.credvault.domain.catalog.InformationSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InformationSystemRepository extends JpaRepository<InformationSystem, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByIdentifierIgnoreCase(String identifier);

    boolean existsByIdentifierIgnoreCaseAndIdNot(String identifier, Long id);

    List<InformationSystem> findAllByOrderByNameAsc();
}
