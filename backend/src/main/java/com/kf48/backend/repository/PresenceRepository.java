package com.kf48.backend.repository;

import com.kf48.backend.domain.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId); // RG18
}