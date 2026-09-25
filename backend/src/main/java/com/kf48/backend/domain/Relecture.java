package com.kf48.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "relecture")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercice_id", nullable = false)
    private Long exerciceId;

    @Column(name = "relecteur_id", nullable = false)
    private Long relecteurId;

    private Integer note;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "attribuee_at", nullable = false)
    private OffsetDateTime attribueeAt;

    @Column(name = "rendue_at")
    private OffsetDateTime rendueAt; // NULL = assignée (RG6), renseignée = rendue

    protected Relecture() {}

    public Relecture(Long exerciceId, Long relecteurId) {
        this.exerciceId = exerciceId;
        this.relecteurId = relecteurId;
        this.attribueeAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getExerciceId() { return exerciceId; }
    public Long getRelecteurId() { return relecteurId; }
    public Integer getNote() { return note; }
    public String getCommentaire() { return commentaire; }
    public OffsetDateTime getAttribueeAt() { return attribueeAt; }
    public OffsetDateTime getRendueAt() { return rendueAt; }
}