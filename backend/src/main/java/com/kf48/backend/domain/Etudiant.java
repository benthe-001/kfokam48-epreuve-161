package com.kf48.backend.domain;

import jakarta.persistence.*;

/**
 * Entité minimale : l'étudiant n'a pas de règles de gestion propres, mais il doit
 * exister pour que l'API puisse distinguer un identifiant inconnu (ETUDIANT_INCONNU)
 * d'une présence valide. Le tableau récapitulatif (EF11) fera appel à nom et promotion.
 */
@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    protected Etudiant() {}

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public Long getPromotionId() { return promotionId; }
}