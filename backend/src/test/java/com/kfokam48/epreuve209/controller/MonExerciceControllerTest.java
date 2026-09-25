package com.kfokam48.epreuve209.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B6 — intégration US-06, dont le test RG7 dédié : le JSON brut de la réponse
 * ne contient jamais « relecteur », quel que soit l'état de l'exercice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MonExerciceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private long deposerEtRendre() throws Exception {
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":101,\"lien\":\"https://exemples.fr/exo\"}"));
        Long relectureId = jdbc.queryForObject(
                "SELECT id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        Long relecteur = jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":14,\"commentaire\":\"Travail sérieux\"}"));
        return relectureId;
    }

    @Test
    void note_et_commentaire_visibles_apres_rendu() throws Exception {
        deposerEtRendre();

        mvc.perform(get("/api/exercices/miens")
                        .param("sessionId", "1").param("etudiantId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.note").value(14))
                .andExpect(jsonPath("$.commentaire").value("Travail sérieux"));
    }

    @Test
    void avant_rendu_pas_de_note_mais_le_statut_est_visible() throws Exception {
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":101,\"lien\":\"https://exemples.fr/exo\"}"));

        mvc.perform(get("/api/exercices/miens")
                        .param("sessionId", "1").param("etudiantId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.statut").isString());
    }

    @Test
    void exercice_inconnu_404() throws Exception {
        mvc.perform(get("/api/exercices/miens")
                        .param("sessionId", "1").param("etudiantId", "106"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }

    @Test
    void RG7_le_json_brut_ne_contient_jamais_le_mot_relecteur() throws Exception {
        deposerEtRendre();

        for (String etat : new String[]{"apres rendu"}) {
            MvcResult resultat = mvc.perform(get("/api/exercices/miens")
                            .param("sessionId", "1").param("etudiantId", "101"))
                    .andExpect(status().isOk())
                    .andReturn();
            String json = resultat.getResponse().getContentAsString();
            assertThat(json.toLowerCase())
                    .as("RG7 : la réponse API ne révèle jamais le relecteur (%s)", etat)
                    .doesNotContain("relecteur");
        }
    }
}
