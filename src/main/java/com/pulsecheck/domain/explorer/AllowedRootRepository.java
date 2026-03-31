package com.pulsecheck.domain.explorer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllowedRootRepository extends JpaRepository<AllowedRoot, Long> {
    Optional<AllowedRoot> findByPath(String path);
    boolean existsByPath(String path);
}
