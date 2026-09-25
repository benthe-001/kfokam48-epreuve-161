package com.kf48.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** RG3 (revisee) : compteur d'echecs CODE_INCONNU par etudiant, tous codes confondus. */
@Entity
@Table(name = "tentative_blocage")
public class TentativeBlocage {

    @Id
    @Column(name = "etudiant_id")
    private Long etudiantId;

    @Column(nullable = false)
    private int echecs;

    @Column(name = "bloque_jusqua")
    private OffsetDateTime bloqueJusqua;

    protected TentativeBlocage() {}

    public TentativeBlocage(Long etudiantId) {
        this.etudiantId = etudiantId;
        this.echecs = 0;
    }

    public Long getEtudiantId() { return etudiantId; }

    public int getEchecs() { return echecs; }

    public OffsetDateTime getBloqueJusqua() { return bloqueJusqua; }

    public boolean estBloque() {
        return bloqueJusqua != null && OffsetDateTime.now().isBefore(bloqueJusqua);
    }

    /** RG3 : 5 echecs CODE_INCONNU consecutifs -> blocage 2 minutes. */
    public void enregistrerEchec() {
        echecs++;
        if (echecs >= 5) {
            bloqueJusqua = OffsetDateTime.now().plusMinutes(2);
            echecs = 0;
        }
    }

    public void reinitialiser() {
        echecs = 0;
        bloqueJusqua = null;
    }
}
