package com.kf48.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TableauControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void tableauRenvoie200AvecUneLigneParEtudiant() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // étudiants 1 et 2
                .andExpect(jsonPath("$[0].etudiantId").exists())
                .andExpect(jsonPath("$[0].nom").exists())
                .andExpect(jsonPath("$[0].presences").exists())
                .andExpect(jsonPath("$[0].exercicesDeposes").exists())
                .andExpect(jsonPath("$[0].relecturesEnAttente").exists());
    }

    @Test
    void laMoyenneEstAbsenteQuandAucunExerciceNEstNote() throws Exception {
        // RG15 : la moyenne vaut null (champ non présent), pas 0
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moyenne").doesNotExist());
    }

    @Test
    void promotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    void promotionIdManquantRenvoie400() throws Exception {
        mockMvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }
}