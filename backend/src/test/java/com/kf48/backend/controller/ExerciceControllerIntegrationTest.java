package com.kf48.backend.controller;

import com.kf48.backend.domain.Exercice;
import com.kf48.backend.domain.Presence;
import com.kf48.backend.domain.Relecture;
import com.kf48.backend.domain.SessionCours;
import com.kf48.backend.repository.ExerciceRepository;
import com.kf48.backend.repository.PresenceRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired private PresenceRepository presenceRepository;

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


    // ---- EF8 / RG7 : consultation de sa note et de son commentaire ----

    /** Depose un exercice, l'assigne a l'etudiant 2, et rend sa relecture. */
    private Long exerciceNote() {
        presenceRepository.save(new Presence(sessionId, 2L, Presence.Source.ETUDIANT));
        Long id = exerciceRepository.save(new Exercice(sessionId, 1L, "https://github.com/e/1")).getId();
        Relecture relecture = relectureRepository.save(new Relecture(id, 2L));
        relecture.rendre(14, "Bon travail.");
        return id;
    }

    @Test
    void consulterRenvoie200AvecLaNoteEtLeCommentaire() throws Exception {
        Long id = exerciceNote();

        mockMvc.perform(get("/api/exercices/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                // DEPOSE et non EN_ATTENTE_RELECTURE : ici l'exercice est inséré via le
                // repository, donc AssignationRelecteurService n'est pas passé par là.
                // L'affectation en elle-même est couverte par AssignationRelecteurServiceTest.
                .andExpect(jsonPath("$.statut").value("DEPOSE"))
                .andExpect(jsonPath("$.note").value(14))
                .andExpect(jsonPath("$.commentaire").value("Bon travail."));
    }

    @Test
    void laConsultationNExposePasLIdentiteDuRelecteur() throws Exception { // RG7
        Long id = exerciceNote();

        // Le corps JSON complet ne doit contenir aucun champ identifiant le relecteur
        String corps = mockMvc.perform(get("/api/exercices/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps.toLowerCase()).doesNotContain("relecteur");
    }

    @Test
    void consulterAvantRenduRenvoie200SansNote() throws Exception {
        Long id = exerciceRepository.save(new Exercice(sessionId, 1L, "https://github.com/e/1")).getId();

        mockMvc.perform(get("/api/exercices/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.statut").value("DEPOSE"))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.commentaire").doesNotExist());
    }

    @Test
    void consulterUnExerciceInconnuRenvoie404() throws Exception {
        mockMvc.perform(get("/api/exercices/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }

}
