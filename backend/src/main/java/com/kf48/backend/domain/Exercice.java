package com.kf48.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exercice")
public class Exercice {

    public enum Statut { DEPOSE, EN_ATTENTE_RELECTURE, NOTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Column(nullable = false, length = 500)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut;

    @Column(name = "depose_at", nullable = false)
    private OffsetDateTime deposeAt;

    @Column(name = "modifie_at")
    private OffsetDateTime modifieAt;

    protected Exercice() {}

    public Exercice(Long sessionId, Long etudiantId, String lien) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.lien = lien;
        this.statut = Statut.DEPOSE; // devient EN_ATTENTE_RELECTURE dès qu'un relecteur est tiré (RG6, ticket #6)
        this.deposeAt = OffsetDateTime.now();
    }

    /** RG6 : un relecteur vient d'être assigné, l'exercice entre dans la file de relecture. */
    public void marquerEnAttenteRelecture() {
        this.statut = Statut.EN_ATTENTE_RELECTURE;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getEtudiantId() { return etudiantId; }
    public String getLien() { return lien; }
    public Statut getStatut() { return statut; }
}