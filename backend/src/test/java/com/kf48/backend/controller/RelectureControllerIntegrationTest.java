package com.kf48.backend.controller;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.RelectureRepository;
import com.kf48.backend.repository.SessionRepository;
import com.kf48.backend.service.RelectureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @Transactional : la base H2 de test est partagee entre les classes (DB_CLOSE_DELAY=-1).
// Sans rollback, la session 'ABCDEF' serait committee et violerait uk_session_code (RG17).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelectureControllerIntegrationTest {

    private static final String LIEN = "https://github.com/etudiant/exercice-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private RelectureService relectureService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private RelectureRepository relectureRepository;

    private Long relectureId;
    private Long sessionId;

    @BeforeEach
    void assigner() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        sessionId = sessionRepository.save(new SessionCours(
                "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15))).getId();
        Long exerciceId = exerciceRepository.save(
                new Exercice(sessionId, 1L, LIEN)).getId();
        relectureId = relectureRepository.save(new Relecture(exerciceId, 2L)).getId();
    }

    private static String corps(String note) {
        return "{\"note\":" + note + ",\"commentaire\":\"Bon travail\"}";
    }

    @Test
    void rendreRenvoie200EtPasseLExerciceANote() throws Exception {
        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relectureId))
                .andExpect(jsonPath("$.note").value(15))
                .andExpect(jsonPath("$.commentaire").value("Bon travail"))
                .andExpect(jsonPath("$.rendueAt").exists())
                .andExpect(jsonPath("$.relecteurId").doesNotExist());
    }

    @Test
    void noteTropHauteRenvoie400NOTE_INVALIDE() throws Exception { // RG8
        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("21")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    void noteNegativeRenvoie400NOTE_INVALIDE() throws Exception { // RG8
        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    void noteNonEntiereRenvoie400NOTE_INVALIDE() throws Exception { // RG8
        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("12.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }
    @Test
    void relectureDejaRendueRenvoie409() throws Exception {
        relectureService.rendre(relectureId,
                new com.kf48.backend.dto.RendreRelectureRequest(BigDecimal.valueOf(12), "Premier passage."));

        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("18")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));
    }

    @Test
    void autoRelectureRenvoie403() throws Exception {
        // Autre exercice et autre auteur : uk_relecture_exercice (RG5) et
        // uk_exercice_session_etudiant (RG19) l'interdiraient sinon
        Long autreExercice = exerciceRepository.save(new Exercice(sessionId, 3L, LIEN)).getId();
        Long fautive = relectureRepository.save(new Relecture(autreExercice, 3L)).getId();

        mockMvc.perform(post("/api/relectures/" + fautive)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("15")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    void relectureInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/relectures/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("15")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }

    @Test
    void commentaireManquantRenvoie400CHAMP_MANQUANT() throws Exception {
        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":15}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }
}
