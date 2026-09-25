package com.kfokam48.epreuve209.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B6 — intégration POST /api/presences : les 4 issues du contrat (201, 400, 409, 410)
 * plus l'extension 429 (RG3), format d'erreur imposé partout. H2 vierge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PresenceControllerTest {

    @Autowired MockMvc mvc;

    private String corps(String code, long etudiantId) {
        return "{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}";
    }

    @Test
    void nominal_201_source_ETUDIANT() throws Exception {
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content(corps("AB12CD", 101)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.etudiantId").value(101))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    void code_inconnu_400_format_impose() throws Exception {
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content(corps("ZZZZZZ", 101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void code_expire_410() throws Exception {
        // Session 2 (V2) : expirée depuis 1 h, non clôturée.
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content(corps("XY34EF", 101)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    void deja_present_409() throws Exception {
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content(corps("AB12CD", 101)));
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content(corps("AB12CD", 101)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void cinq_echecs_puis_429_blocage_tentatives() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                            .content(corps("ZZZZZZ", 102)))
                    .andExpect(status().isBadRequest());
        }
        // 6e tentative : 429, même avec un code VALIDE (le blocage prime — D3).
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content(corps("AB12CD", 102)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("BLOCAGE_TENTATIVES"));
    }

    @Test
    void champ_manquant_400() throws Exception {
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etudiantId\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }
}
