package com.kf48.backend.repository;

import com.kf48.backend.domain.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId); // RG19
    List<Exercice> findBySessionIdAndStatut(Long sessionId, Exercice.Statut statut); // RG6

    /** EF11 : nombre d'exercices déposés par étudiant, sur les sessions de la promotion. */
    @Query("select e.etudiantId, count(e) from Exercice e "
            + "where e.sessionId in :sessionIds group by e.etudiantId")
    List<Object[]> compterParEtudiant(@Param("sessionIds") List<Long> sessionIds);

    /**
     * EF11 / RG15 : moyenne par étudiant, toutes sessions confondues, sur les seuls
     * exercices notés. Les relectures non rendues et les notes nulles sont exclues.
     * Le produit cartésien avec un filtre d'égalité remplace un JOIN, les deux entités
     * n'ayant pas de relation déclarée entre elles (seulement des clés étrangères).
     */
    @Query("select e.etudiantId, avg(r.note) from Exercice e, Relecture r "
            + "where r.exerciceId = e.id and e.sessionId in :sessionIds "
            + "and r.rendueAt is not null and r.note is not null "
            + "group by e.etudiantId")
    List<Object[]> moyenneNoteeParEtudiant(@Param("sessionIds") List<Long> sessionIds);
}