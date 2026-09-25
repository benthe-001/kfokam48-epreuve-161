package com.kf48.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "session_cours")
public class SessionCours {

    public enum Statut { OUVERTE, CLOTUREE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(nullable = false, length = 6, unique = true)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private OffsetDateTime ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private OffsetDateTime expirationAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Statut statut;

    @Column(name = "cloture_at")
    private OffsetDateTime clotureAt;

    protected SessionCours() {}

    public SessionCours(String titre, Long promotionId, String code,
                        OffsetDateTime ouvertureAt, OffsetDateTime expirationAt) {
        this.titre = titre;
        this.promotionId = promotionId;
        this.code = code;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = expirationAt;
        this.statut = Statut.OUVERTE; // RG14 : ouverte tant que non clôturée explicitement
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public OffsetDateTime getOuvertureAt() { return ouvertureAt; }
    public OffsetDateTime getExpirationAt() { return expirationAt; }
}