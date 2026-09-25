package com.kf48.backend.repository;

import com.kf48.backend.domain.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    /** EF11 : les étudiants de la promotion, dans l'ordre de leur identifiant. */
    List<Etudiant> findByPromotionIdOrderById(Long promotionId);
}