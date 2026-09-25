package com.kf48.backend.repository;

import com.kf48.backend.domain.SessionCours;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionCours, Long> {
    boolean existsByCode(String code);
}