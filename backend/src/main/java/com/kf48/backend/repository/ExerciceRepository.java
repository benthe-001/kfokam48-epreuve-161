package com.kf48.backend.repository;

import com.kf48.backend.domain.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId); // RG19
    List<Exercice> findBySessionIdAndStatut(Long sessionId, Exercice.Statut statut); // RG6

    /**
     * RG5 révisée : exercices de la session qui ne sont pas encore notés, donc ceux
     * qui peuvent encore recevoir un relecteur. Un exercice déjà noté (NOTE) n'en
     * reçoit plus ; un DEPOSE n'en a aucun, un EN_ATTENTE_RELECTURE en a un ou deux.
     */
    List<Exercice> findBySessionIdAndStatutNot(Long sessionId, Exercice.Statut statut);

    /** EF11 : nombre d'exercices déposés par étudiant, sur les sessions de la promotion. */
    @Query("select e.etudiantId, count(e) from Exercice e "
            + "where e.sessionId in :sessionIds group by e.etudiantId")
    List<Object[]> compterParEtudiant(@Param("sessionIds") List<Long> sessionIds);

    /**
     * EF11 / RG15 / RG17 : moyenne par étudiant, toutes sessions de la promotion
     * confondues.
     *
     * Seules les notes des exercices dont les DEUX relectures sont rendues entrent
     * dans le calcul : un exercice noté provisoirement (un seul rendu sur deux) ne
     * doit pas peser dans une moyenne définitive, sans quoi le tableau oscillerait
     * jusqu'à la seconde réponse. C'est la sous-requête corrélée qui l'exprime.
     */
    @Query("select e.etudiantId, avg(r.note) from Exercice e, Relecture r "
            + "where r.exerciceId = e.id and e.sessionId in :sessionIds "
            + "and r.rendueAt is not null and r.note is not null "
            + "and (select count(r2) from Relecture r2 "
            + "     where r2.exerciceId = e.id and r2.rendueAt is not null) = 2 "
            + "group by e.etudiantId")
    List<Object[]> moyenneNoteeParEtudiant(@Param("sessionIds") List<Long> sessionIds);
}