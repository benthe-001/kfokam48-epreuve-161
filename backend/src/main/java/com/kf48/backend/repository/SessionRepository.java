package com.kf48.backend.repository;

import com.kf48.backend.domain.SessionCours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionCours, Long> {
    boolean existsByCode(String code);
    Optional<SessionCours> findByCode(String code); // ajouté pour EF2
}