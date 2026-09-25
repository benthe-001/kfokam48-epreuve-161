package com.kf48.relectures.repository;

import com.kfokam48.relectures.domain.SessionCours;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionCours, Long> {
    boolean existsByCode(String code);
}