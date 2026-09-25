package com.kf48.backend.repository;

import com.kf48.backend.domain.Relecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {
    boolean existsByExerciceId(Long exerciceId); // RG5
    Optional<Relecture> findByExerciceId(Long exerciceId);
}