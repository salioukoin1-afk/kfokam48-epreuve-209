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
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    private String corps(Long sessionId, Long etudiantId, String lien) {
        return "{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                + ",\"lien\":\"" + lien + "\"}";
    }

    private IntegrationIds ids() { return new IntegrationIds(jdbc); }

    @Test
    void depot_nominal_201_avec_statut() throws Exception {
        // Awa Ndiaye est présente à la session AB12CD (V5) → dépôt accepté.
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(ids().sessionParCode("AB12CD"), ids().etudiant("Awa Ndiaye"), "https://exemples.fr/exo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.statut").isString());
    }

    @Test
    void etudiant_absent_400_NON_PRESENT() throws Exception {
        // Fodé Camara n'a pas été marqué présent sur la session AB12CD (V5).
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(ids().sessionParCode("AB12CD"), ids().etudiant("Fodé Camara"), "https://exemples.fr/exo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NON_PRESENT"));
    }

    @Test
    void lien_invalide_400_LIEN_INVALIDE() throws Exception {
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(ids().sessionParCode("AB12CD"), ids().etudiant("Awa Ndiaye"), "pas une url")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    void remplacement_meme_id_pas_de_doublon_RG12() throws Exception {
        Long sessionId = ids().sessionParCode("AB12CD");
        Long etudiantId = ids().etudiant("Awa Ndiaye");
        String premier = mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(sessionId, etudiantId, "https://exemples.fr/v1")))
                .andReturn().getResponse().getContentAsString();
        int premierId = ((Number) com.jayway.jsonpath.JsonPath.parse(premier).read("$.id")).intValue();

        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(sessionId, etudiantId, "https://exemples.fr/v2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(premierId));
    }
}
