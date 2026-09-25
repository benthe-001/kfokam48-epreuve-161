package com.kf48.backend.repository;

import com.kf48.backend.domain.SessionCours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionCours, Long> {
    boolean existsByCode(String code);
    Optional<SessionCours> findByCode(String code); // ajouté pour EF2

    /** EF11 : sessions d'une promotion, pour délimiter le périmètre du tableau. */
    List<SessionCours> findByPromotionId(Long promotionId);

    /**
     * Issue #25 : verrouille la session en écriture avant de lancer une assignation.
     *
     * L'assignation de relecteur est un read-then-write sur la liste des exercices
     * DEPOSE. Deux transactions concurrentes peuvent lire le meme exercice et inserer
     * toutes deux une relecture ; c'est la contrainte unique RG5 qui tranche, et
     * l'echec qui en decoule annule toute la transaction appelante — y compris la
     * presence de l'etudiant. Serialiser sur la ligne session evite d'en arriver la.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionCours s where s.id = :id")
    Optional<SessionCours> findByIdPourMiseAJour(@Param("id") Long id);
}