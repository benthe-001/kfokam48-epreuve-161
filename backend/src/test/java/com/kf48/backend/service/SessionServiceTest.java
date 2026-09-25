package com.kf48.backend.service;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionClotureResponse;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.exception.MetierException;
import com.kf48.backend.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionServiceTest {

    @Autowired private SessionService sessionService;
    @Autowired private SessionRepository sessionRepository;

    @Test
    void lExpirationEstQuinzeMinutesApresLOuverture() { // RG1
        SessionResponse reponse = sessionService.ouvrir(new OuvrirSessionRequest("Seance test", 1L));

        Duration duree = Duration.between(reponse.ouvertureAt(), reponse.expirationAt());
        assertThat(duree).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void chaqueCodeGenereEstUnique() { // RG17
        SessionResponse s1 = sessionService.ouvrir(new OuvrirSessionRequest("Seance 1", 1L));
        SessionResponse s2 = sessionService.ouvrir(new OuvrirSessionRequest("Seance 2", 1L));

        assertThat(s1.code()).isNotEqualTo(s2.code());
        assertThat(sessionRepository.existsByCode(s1.code())).isTrue();
    }

    // ---- EF9 / RG14 : clôture explicite par le formateur ----

    @Test
    void cloturerPasseLaSessionEnCloturee() { // EF9, RG14
        Long id = sessionService.ouvrir(new OuvrirSessionRequest("Seance test", 1L)).id();

        SessionClotureResponse reponse = sessionService.cloturer(id);

        assertThat(reponse.id()).isEqualTo(id);
        assertThat(reponse.statut()).isEqualTo("CLOTUREE");
        assertThat(reponse.clotureAt()).isNotNull();
        assertThat(sessionRepository.findById(id).orElseThrow().getStatut())
                .isEqualTo(SessionCours.Statut.CLOTUREE);
    }

    @Test
    void cloturerDeuxFoisRenvoie409SESSION_DEJA_CLOTUREE() {
        Long id = sessionService.ouvrir(new OuvrirSessionRequest("Seance test", 1L)).id();
        sessionService.cloturer(id);

        assertThatThrownBy(() -> sessionService.cloturer(id))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SESSION_DEJA_CLOTUREE");
                    assertThat(e.getStatut().value()).isEqualTo(409);
                });
    }

    @Test
    void cloturerUneSessionInconnueRenvoie404() {
        assertThatThrownBy(() -> sessionService.cloturer(999999L))
                .isInstanceOfSatisfying(MetierException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SESSION_INCONNUE");
                    assertThat(e.getStatut().value()).isEqualTo(404);
                });
    }

    @Test
    void laClotureEstDistincteDeLExpirationDuCode() { // RG14
        // Une session dont le code a expiré reste OUVERTE tant que le formateur ne l'a pas close.
        OffsetDateTime ouverture = OffsetDateTime.now().minusHours(2);
        SessionCours terminee = sessionRepository.save(new SessionCours(
                "Seance terminee", 1L, "EXPIRE", ouverture, ouverture.plusMinutes(15)));
        assertThat(terminee.getExpirationAt()).isBefore(OffsetDateTime.now());

        assertThat(terminee.getStatut()).isEqualTo(SessionCours.Statut.OUVERTE);

        sessionService.cloturer(terminee.getId());

        assertThat(sessionRepository.findById(terminee.getId()).orElseThrow().getStatut())
                .isEqualTo(SessionCours.Statut.CLOTUREE);
    }
}