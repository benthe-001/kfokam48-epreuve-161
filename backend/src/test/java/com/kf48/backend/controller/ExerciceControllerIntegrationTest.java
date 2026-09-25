package com.kf48.backend.controller;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.RelectureRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @Transactional : la base H2 de test est partagee entre les classes (DB_CLOSE_DELAY=-1).
// Sans rollback, la session 'ABCDEF' creee ici serait committee et violerait uk_session_code (RG17).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ExerciceRepository exerciceRepository;
    @Autowired private RelectureRepository relectureRepository;

    private Long sessionId;

    @BeforeEach
    void ouvrirSession() {
        OffsetDateTime ouverture = OffsetDateTime.now();
        sessionId = sessionRepository.save(new SessionCours(
                        "Seance test", 1L, "ABCDEF", ouverture, ouverture.plusMinutes(15)))
                .getId();
    }

    @Test
    void depotRenvoie201() throws Exception {
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":3,\"lien\":\"https://github.com/e/1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    @Test
    void lienNonHttpRenvoie400LIEN_INVALIDE() throws Exception {
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":3,\"lien\":\"ftp://fichier\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    void sessionInconnueRenvoie404SESSION_INCONNUE() throws Exception {
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":999999,\"etudiantId\":3,\"lien\":\"https://github.com/e/1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void secondDepotRenvoie409EXERCICE_DEJA_DEPOSE() throws Exception {
        String corps = "{\"sessionId\":" + sessionId + ",\"etudiantId\":3,\"lien\":\"https://github.com/e/1\"}";
        mockMvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void lienManquantRenvoie400CHAMP_MANQUANT() throws Exception {
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    // ---------- EF5 / RG12 : PUT /api/exercices/{id} ----------

    /** Cree un exercice sans relecteur assigne et retourne son id. */
    private long creerExercice(long etudiantId) {
        return exerciceRepository.save(new Exercice(sessionId, etudiantId, "https://github.com/e/1")).getId();
    }

    @Test
    void remplacementRenvoie200() throws Exception {
        long id = creerExercice(3);

        mockMvc.perform(put("/api/exercices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"https://github.com/e/2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    @Test
    void remplacementAvecLienInvalideRenvoie400LIEN_INVALIDE() throws Exception {
        long id = creerExercice(3);

        mockMvc.perform(put("/api/exercices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"ftp://fichier\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    void remplacementSansLienRenvoie400CHAMP_MANQUANT() throws Exception {
        long id = creerExercice(3);

        mockMvc.perform(put("/api/exercices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void exerciceInconnuRenvoie404EXERCICE_INCONNU() throws Exception {
        mockMvc.perform(put("/api/exercices/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"https://github.com/e/2\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }

    @Test
    void remplacementRenvoie409RELECTEUR_DEJA_ASSIGNE() throws Exception {
        // RG12 : refuse des l'assignation, meme si la relecture n'est pas encore rendue
        long id = creerExercice(3L);
        relectureRepository.save(new Relecture(id, 2L));

        mockMvc.perform(put("/api/exercices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"https://github.com/e/2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTEUR_DEJA_ASSIGNE"));
    }
}