package com.kf48.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void ouvrirUneSessionRenvoie201AvecCodeEtExpiration() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.ouvertureAt").exists())
                .andExpect(jsonPath("$.expirationAt").exists());
    }

    @Test
    void titreManquantRenvoie400AuFormatDuContrat() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"promotionId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---- EF9 : clôture explicite de la session ----

    private int ouvrirEtCloturer() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance a cloturer\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(corps, "$.id");
        mockMvc.perform(post("/api/sessions/" + id + "/cloture"))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void cloturerUneSessionRenvoie200EtLeStatutCloturee() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(corps, "$.id");

        mockMvc.perform(post("/api/sessions/" + id + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.statut").value("CLOTUREE"))
                .andExpect(jsonPath("$.clotureAt").exists());
    }

    @Test
    void cloturerUneSessionDejaClotureeRenvoie409() throws Exception {
        int id = ouvrirEtCloturer();

        mockMvc.perform(post("/api/sessions/" + id + "/cloture"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_CLOTUREE"));
    }

    @Test
    void cloturerUneSessionInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/sessions/999999/cloture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    // ---- EF10 / RG13 : présence manuelle par le formateur ----

    @Test
    void ajouterUnePresenceManuelleRenvoie201SourceFormateur() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(corps, "$.id");

        mockMvc.perform(post("/api/sessions/" + id + "/presences")
                        .contentType("application/json")
                        .content("{\"etudiantId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(id))
                .andExpect(jsonPath("$.etudiantId").value(3))
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    void ajouterUnePresenceApresClotureRenvoie409() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(corps, "$.id");
        mockMvc.perform(post("/api/sessions/" + id + "/cloture")).andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions/" + id + "/presences")
                        .contentType("application/json")
                        .content("{\"etudiantId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void ajouterUnePresencePourUnEtudiantInconnuRenvoie400() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(corps, "$.id");

        mockMvc.perform(post("/api/sessions/" + id + "/presences")
                        .contentType("application/json")
                        .content("{\"etudiantId\":999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void ajouterUnePresenceSurSessionInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/sessions/999999/presences")
                        .contentType("application/json")
                        .content("{\"etudiantId\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void etudiantIdManquantRenvoie400CHAMP_MANQUANT() throws Exception {
        String corps = mockMvc.perform(post("/api/sessions")
                        .contentType("application/json")
                        .content("{\"titre\":\"Seance 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(corps, "$.id");

        mockMvc.perform(post("/api/sessions/" + id + "/presences")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }
}