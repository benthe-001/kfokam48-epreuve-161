package com.kf48.backend.service;

import com.kf48.backend.dto.OuvrirSessionRequest;
import com.kf48.backend.dto.SessionResponse;
import com.kf48.backend.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

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
}