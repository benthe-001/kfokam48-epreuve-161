package com.kf48.backend.repository;

import com.kf48.backend.domain.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId); // RG19
}