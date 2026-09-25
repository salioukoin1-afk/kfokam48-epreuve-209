package com.kfokam48.epreuve209.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B6 — intégration GET /api/tableau : les 6 champs du contrat, moyenne null ≠ 0,
 * RG10 (relectures en attente visibles), 404 PROMOTION_INCONNUE.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TableauControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private void flusher() { entityManager.flush(); }

    @Test
    void structure_conforme_au_contrat_6_champs() throws Exception {
        // NB : jsonPath().exists() échoue sur une valeur JSON null (comportement Spring) —
        // la moyenne, null au départ, est donc vérifiée par un matcher nullValue, ce qui
        // prouve à la fois la présence du champ et sa valeur Q16.
        mvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].etudiantId").exists())
                .andExpect(jsonPath("$[0].nom").exists())
                .andExpect(jsonPath("$[0].presences").exists())
                .andExpect(jsonPath("$[0].exercicesDeposes").exists())
                .andExpect(jsonPath("$[0].moyenne").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].relecturesEnAttente").exists());
    }

    @Test
    void moyenne_null_pour_un_etudiant_sans_note_distincte_de_zero() throws Exception {
        // 101 a des présences mais aucune note au départ → moyenne null.
        mvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etudiantId == 101)].moyenne").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void la_moyenne_reflete_une_relecture_rendue_et_sa_correction() throws Exception {
        // Flux complet : dépôt 101 → relecteur (102 ou 103...) rend 12 → PATCH 16.
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":101,\"lien\":\"https://exemples.fr/exo\"}"));
        flusher();
        Long relectureId = jdbc.queryForObject(
                "SELECT id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        Long relecteur = jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);

        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":12,\"commentaire\":\"Correct\"}"));
        mvc.perform(patch("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":16,\"commentaire\":\"Amélioré\"}"));
        flusher();

        // La moyenne de l'auteur (101) doit être exactement 16.0 (dernière valeur valide).
        mvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etudiantId == 101)].moyenne").value(
                        org.hamcrest.Matchers.hasItem(16.0)));
        // Et le relecteur qui a rendu n'a plus de relecture en attente (RG10 n'est plus déclenché pour lui).
    }

    @Test
    void relectures_en_attente_visibles_RG10() throws Exception {
        // Dépôt sans rendre la relecture : le relecteur désigné a 1 relecture en attente.
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":101,\"lien\":\"https://exemples.fr/exo\"}"));
        flusher();
        Long relecteur = jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);

        mvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etudiantId == " + relecteur + ")].relecturesEnAttente")
                        .value(org.hamcrest.Matchers.hasItem(1)));
    }

    @Test
    void promotion_inconnue_404_impose() throws Exception {
        mvc.perform(get("/api/tableau").param("promotionId", "9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }
}
