package com.kf48.backend.repository;

import com.kf48.backend.domain.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId); // RG18

    @Query("select distinct p.etudiantId from Presence p where p.sessionId = :sessionId")
    List<Long> findEtudiantIdsBySessionId(@Param("sessionId") Long sessionId); // RG6

    /** EF11 : nombre de présences par étudiant, sur les sessions de la promotion. */
    @Query("select p.etudiantId, count(p) from Presence p "
            + "where p.sessionId in :sessionIds group by p.etudiantId")
    List<Object[]> compterParEtudiant(@Param("sessionIds") List<Long> sessionIds);
}