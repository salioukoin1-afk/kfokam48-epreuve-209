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
 * B6 — intégration POST /api/exercices : contrat (201/400/409) + upsert RG12
 * vérifié de bout en bout sur le jeu de démonstration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciceControllerTest {

    @Autowired MockMvc mvc;

    private String corps(Long sessionId, Long etudiantId, String lien) {
        return "{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                + ",\"lien\":\"" + lien + "\"}";
    }

    @Test
    void depot_nominal_201_avec_statut() throws Exception {
        // Étudiant 101 présent à la session 1 (V2) → dépôt accepté.
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(1L, 101L, "https://exemples.fr/exo-101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.statut").isString());
    }

    @Test
    void etudiant_absent_400_NON_PRESENT() throws Exception {
        // Étudiant 106 (V2) n'a pas été marqué présent sur la session 1.
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(1L, 106L, "https://exemples.fr/exo-106")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NON_PRESENT"));
    }

    @Test
    void lien_invalide_400_LIEN_INVALIDE() throws Exception {
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(1L, 101L, "pas une url")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    void remplacement_meme_id_pas_de_doublon_RG12() throws Exception {
        String premier = mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(1L, 101L, "https://exemples.fr/v1")))
                .andReturn().getResponse().getContentAsString();
        int premierId = ((Number) com.jayway.jsonpath.JsonPath.parse(premier).read("$.id")).intValue();

        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(1L, 101L, "https://exemples.fr/v2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(premierId));
    }
}
