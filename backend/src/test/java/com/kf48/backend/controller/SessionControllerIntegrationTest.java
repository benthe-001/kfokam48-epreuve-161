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
}