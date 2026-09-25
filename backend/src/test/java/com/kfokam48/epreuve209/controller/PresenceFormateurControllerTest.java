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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B6 — intégration US-03 (présence formateur) et GET /api/relectures/en-attente.
 * Ids par noms/codes ; flush avant toute lecture JDBC brute.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PresenceFormateurControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private void flusher() { entityManager.flush(); }
    private IntegrationIds ids() { return new IntegrationIds(jdbc); }

    @Test
    void ajout_manuel_201_source_FORMATEUR() throws Exception {
        // Fodé Camara n'était pas présent : le formateur le marque présent à la main (Q14).
        mvc.perform(post("/api/presences/formateur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + ids().sessionParCode("AB12CD")
                                + ",\"etudiantId\":" + ids().etudiant("Fodé Camara") + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"))
                .andExpect(jsonPath("$.sessionId").value(ids().sessionParCode("AB12CD").intValue()))
                .andExpect(jsonPath("$.etudiantId").value(ids().etudiant("Fodé Camara").intValue()));
    }

    @Test
    void doublon_409_RG15_meme_via_le_formateur() throws Exception {
        String corps = "{\"sessionId\":" + ids().sessionParCode("AB12CD")
                + ",\"etudiantId\":" + ids().etudiant("Fodé Camara") + "}";
        mvc.perform(post("/api/presences/formateur").contentType(MediaType.APPLICATION_JSON).content(corps));

        mvc.perform(post("/api/presences/formateur").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void presence_formateur_visible_dans_le_tableau() throws Exception {
        mvc.perform(post("/api/presences/formateur").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + ids().sessionParCode("AB12CD")
                        + ",\"etudiantId\":" + ids().etudiant("Fodé Camara") + "}"));

        mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(ids().promotion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.etudiantId == " + ids().etudiant("Fodé Camara") + ")].presences")
                        .value(org.hamcrest.Matchers.hasItem(1)));
    }

    @Test
    void liste_en_attente_du_relecteur_RG10() throws Exception {
        // Un dépôt d'Awa désigne un relecteur qui n'a encore rien rendu.
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + ids().sessionParCode("AB12CD")
                        + ",\"etudiantId\":" + ids().etudiant("Awa Ndiaye")
                        + ",\"lien\":\"https://exemples.fr/exo\"}"));
        flusher();
        Long relecteur = jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);

        mvc.perform(get("/api/relectures/en-attente").param("etudiantId", String.valueOf(relecteur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].relectureId").exists())
                .andExpect(jsonPath("$[0].lien").value("https://exemples.fr/exo"))
                .andExpect(jsonPath("$[0].auteurNom").value("Awa Ndiaye"))
                .andExpect(jsonPath("$[0].sessionTitre").isString());
    }
}
