package io.github.jacampano.credvault.repository;

import io.github.jacampano.credvault.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByCreatedByAndDeletedFalseOrderByUpdatedAtDesc(String createdBy);

    @Query("""
            select distinct c
            from Credential c
            left join c.groups t
            where c.deleted = false and (c.createdBy = :username or t in :groups)
            order by c.updatedAt desc
            """)
    List<Credential> findVisibleForUser(@Param("username") String username, @Param("groups") Collection<String> groups);

    Optional<Credential> findByIdAndCreatedByAndDeletedFalse(Long id, String createdBy);

    @Query("""
            select distinct c
            from Credential c
            left join c.groups t
            where c.id = :id and c.deleted = false and (c.createdBy = :username or t in :groups)
            """)
    Optional<Credential> findVisibleByIdForUser(@Param("id") Long id,
                                                @Param("username") String username,
                                                @Param("groups") Collection<String> groups);

    Optional<Credential> findByIdAndDeletedFalse(Long id);

    Optional<Credential> findByIdAndDeletedTrue(Long id);

    List<Credential> findByDeletedTrueOrderByDeletedAtDesc();
}
