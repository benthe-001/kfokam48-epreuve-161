package com.kf48.backend.service;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.domain.TentativeBlocage;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.SessionRepository;
import com.kf48.backend.repository.TentativeBlocageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PresenceServiceTest {

    @Autowired private PresenceService presenceService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private PresenceRepository presenceRepository;
    @Autowired private TentativeBlocageRepository tentativeBlocageRepository;

    private SessionCours session;

    @BeforeEach
    void ouvrirSession() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        session = sessionRepository.save(new SessionCours(
                "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15)));
    }

    @Test
    void marquerAvecCodeValideCreeUnePresenceSourceEtudiant() { // RG13, RG18
        PresenceResponse reponse = presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 1L));

        assertThat(reponse.sessionId()).isEqualTo(session.getId());
        assertThat(reponse.etudiantId()).isEqualTo(1L);
        assertThat(reponse.source()).isEqualTo("ETUDIANT");
        assertThat(presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), 1L)).isTrue();
    }

    @Test
    void codeInconnuRenvoleCODE_INCONNU() { // RG2
        assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ZZZZZZ", 1L)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("CODE_INCONNU"));
    }

    @Test
    void codeExpireRenvoleCODE_EXPIRE() { // RG1
        OffsetDateTime passe = OffsetDateTime.now().minusMinutes(16);
        SessionCours expiree = sessionRepository.save(new SessionCours(
                "Seance passee", 1L, "EXPIRE", passe, passe.plusMinutes(15)));

        assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("EXPIRE", 1L)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("CODE_EXPIRE"));
    }

    @Test
    void doubleMarquageRenvoleDEJA_PRESENT() { // RG18
        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 2L));

        assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 2L)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("DEJA_PRESENT"));
    }

    // ---------- RG3 (revisee) : blocage par etudiant, seuls les CODE_INCONNU comptent ----------

    @Test
    void cinqEchecsCodeInconnuBloquentLEtudiant() { // RG3
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ZZZZZZ", 3L)))
                    .isInstanceOfSatisfying(MetierException.class,
                            e -> assertThat(e.getCode()).isEqualTo("CODE_INCONNU"));
        }

        // 6e tentative : bloque, y compris avec un code VALIDE (tous codes confondus).
        assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 3L)))
                .isInstanceOfSatisfying(MetierException.class,
                        e -> assertThat(e.getCode()).isEqualTo("ETUDIANT_BLOQUE"));
    }

    @Test
    void leBlocageEstPropreAUnEtudiant() { // RG3 : suivi par etudiant seul
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ZZZZZZ", 3L)))
                    .isInstanceOfSatisfying(MetierException.class, e -> assertThat(e.getCode()).isEqualTo("CODE_INCONNU"));
        }

        // L'etudiant 4 n'est pas affecte par les echecs de l'etudiant 3.
        PresenceResponse reponse = presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 4L));
        assertThat(reponse.etudiantId()).isEqualTo(4L);
    }

    @Test
    void unSuccesReinitialiseLeCompteur() { // RG3
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ZZZZZZ", 3L)))
                    .isInstanceOfSatisfying(MetierException.class, e -> assertThat(e.getCode()).isEqualTo("CODE_INCONNU"));
        }

        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 3L)); // succes -> remise a zero

        TentativeBlocage apresReussite = tentativeBlocageRepository.findById(3L).orElseThrow();
        assertThat(apresReussite.getEchecs()).isZero();
        assertThat(apresReussite.estBloque()).isFalse();

        // Le compteur repart de zero : 4 echecs supplementaires ne bloquent toujours pas.
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("YYYYYY", 3L)))
                    .isInstanceOfSatisfying(MetierException.class, e -> assertThat(e.getCode()).isEqualTo("CODE_INCONNU"));
        }
    }

    @Test
    void codeExpireEtDejaPresentNeComptentPasCommeEchec() { // RG3 revisee
        OffsetDateTime passe = OffsetDateTime.now().minusMinutes(16);
        sessionRepository.save(new SessionCours(
                "Seance passee", 1L, "EXPIRE", passe, passe.plusMinutes(15)));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("EXPIRE", 3L)))
                    .isInstanceOfSatisfying(MetierException.class, e -> assertThat(e.getCode()).isEqualTo("CODE_EXPIRE"));
        }
        assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("EXPIRE", 3L)))
                .isInstanceOfSatisfying(MetierException.class, e -> assertThat(e.getCode()).isEqualTo("CODE_EXPIRE"));

        // DEJA_PRESENT non plus : l'etudiant connaissait un code valide, ce n'est pas du brute-force.
        presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 3L));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> presenceService.marquer(new MarquerPresenceRequest("ABCDEF", 3L)))
                    .isInstanceOfSatisfying(MetierException.class, e -> assertThat(e.getCode()).isEqualTo("DEJA_PRESENT"));
        }

        // Aucune ligne n'est bloquee et le compteur est a zero : ces echecs n'ont pas ete comptes.
        TentativeBlocage apresEchecsIgnores = tentativeBlocageRepository.findById(3L).orElseThrow();
        assertThat(apresEchecsIgnores.getEchecs()).isZero();
        assertThat(apresEchecsIgnores.estBloque()).isFalse();
    }
}