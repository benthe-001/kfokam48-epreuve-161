package com.kf48.backend.repository;

import com.kf48.backend.domain.SessionCours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionCours, Long> {
    boolean existsByCode(String code);
    Optional<SessionCours> findByCode(String code); // ajouté pour EF2

    /** EF11 : sessions d'une promotion, pour délimiter le périmètre du tableau. */
    List<SessionCours> findByPromotionId(Long promotionId);
}