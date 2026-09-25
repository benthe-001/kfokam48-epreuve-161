package com.kf48.backend.service;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.MarquerPresenceRequest;
import com.kf48.backend.dto.PresenceResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.PresenceRepository;
import com.kf48.backend.repository.SessionRepository;
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
}