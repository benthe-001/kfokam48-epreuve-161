package com.kf48.backend.repository;

import com.kf48.backend.domain.Relecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {
    boolean existsByExerciceId(Long exerciceId); // RG5
    Optional<Relecture> findByExerciceId(Long exerciceId);

    /** EF11 : relectures encore à faire par relecteur (RG10), sur les sessions de la promotion. */
    @Query("select r.relecteurId, count(r) from Relecture r, Exercice e "
            + "where r.exerciceId = e.id and e.sessionId in :sessionIds and r.rendueAt is null "
            + "group by r.relecteurId")
    List<Object[]> compterEnAttenteParRelecteur(@Param("sessionIds") List<Long> sessionIds);

    /** RG5 révisée : toutes les relectures d'un exercice, pour compter et choisir les relecteurs. */
    List<Relecture> findByExerciceIdIn(List<Long> exerciceIds);

    /** RG17 : nombre de relectures effectivement rendues pour un exercice. */
    long countByExerciceIdAndRendueAtIsNotNull(Long exerciceId);
}