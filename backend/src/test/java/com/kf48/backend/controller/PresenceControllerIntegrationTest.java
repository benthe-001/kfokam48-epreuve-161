package com.kf48.backend.controller;

import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @Transactional : la base H2 de test est partagee entre les classes de tests (DB_CLOSE_DELAY=-1).
// Sans rollback, la session 'ABCDEF' creee ici serait committee et violerait uk_session_code (RG17).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PresenceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SessionRepository sessionRepository;

    @BeforeEach
    void ouvrirSession() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        sessionRepository.save(new SessionCours(
                "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15)));
    }

    @Test
    void marquerPresenceRenvoie201() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCDEF\",\"etudiantId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.etudiantId").value(3))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    void codeInconnuRenvoie400CODE_INCONNU() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void codeManquantRenvoie400CHAMP_MANQUANT() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etudiantId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void sixiemeEssaiRenvoie429ETUDIANT_BLOQUE() throws Exception { // RG3
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/presences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":3}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }

        // Meme avec le bon code, l'etudiant est bloque 2 minutes.
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCDEF\",\"etudiantId\":3}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ETUDIANT_BLOQUE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}