package io.github.jacampano.credvault.repository.auth;

import io.github.jacampano.credvault.domain.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Page<AppUser> findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username,
            String firstName,
            String lastName,
            String email,
            Pageable pageable
    );

    @Query("select count(u) from AppUser u join u.teams t where t = :team")
    long countMembersByTeam(@Param("team") String team);
}
