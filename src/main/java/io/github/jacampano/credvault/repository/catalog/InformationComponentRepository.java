package io.github.jacampano.credvault.repository.catalog;

import io.github.jacampano.credvault.domain.catalog.InformationComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InformationComponentRepository extends JpaRepository<InformationComponent, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByIdentifierIgnoreCase(String identifier);

    boolean existsByIdentifierIgnoreCaseAndIdNot(String identifier, Long id);

    long countByInformationSystemId(Long informationSystemId);

    List<InformationComponent> findAllByOrderByNameAsc();
}
