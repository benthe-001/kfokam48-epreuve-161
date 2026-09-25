package com.kf48.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "presence")
public class Presence {

    public enum Source { ETUDIANT, FORMATEUR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source;

    @Column(name = "marquee_at", nullable = false)
    private OffsetDateTime marqueeAt;

    protected Presence() {}

    public Presence(Long sessionId, Long etudiantId, Source source) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.source = source;
        this.marqueeAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getEtudiantId() { return etudiantId; }
    public Source getSource() { return source; }
    public OffsetDateTime getMarqueeAt() { return marqueeAt; }
}