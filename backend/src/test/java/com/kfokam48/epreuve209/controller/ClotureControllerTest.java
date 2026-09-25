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
 * B6 — intégration US-08 : la clôture verrouille les 3 écritures (RG2/RG9/RG11).
 * Le verrou est vérifié de bout en bout : on clôture PAR L'API (chaque appel MockMvc
 * participe à la même transaction de test, donc la clôture est visible des appels suivants).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClotureControllerTest {

    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void cloture_nominal_200_avec_date() throws Exception {
        mvc.perform(post("/api/sessions/1/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clotureeAt").exists());
    }

    @Test
    void re_cloture_400_BLOCAGE_CLOTURE_sans_ecraser_la_date() throws Exception {
        String premiere = mvc.perform(post("/api/sessions/1/cloture"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/sessions/1/cloture"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BLOCAGE_CLOTURE"));
    }

    @Test
    void session_inconnue_404() throws Exception {
        mvc.perform(post("/api/sessions/999999/cloture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void apres_cloture_presence_409_RG2() throws Exception {
        mvc.perform(post("/api/sessions/1/cloture")).andExpect(status().isOk());

        // L'étudiant 106 était absent : sans clôture il pourrait se marquer présent.
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AB12CD\",\"etudiantId\":106}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void apres_cloture_depot_409_RG11() throws Exception {
        mvc.perform(post("/api/sessions/1/cloture")).andExpect(status().isOk());

        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":1,\"etudiantId\":102,\"lien\":\"https://exemples.fr/trop-tard\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void apres_cloture_correction_note_409_RG9() throws Exception {
        // Relecture rendue AVANT la clôture…
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":101,\"lien\":\"https://exemples.fr/exo\"}"));
        Long relectureId = jdbc.queryForObject(
                "SELECT id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        Long relecteur = jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":12,\"commentaire\":\"Initial\"}"));

        // …puis clôture PAR L'API (visible des appels suivants — même transaction de test).
        mvc.perform(post("/api/sessions/1/cloture")).andExpect(status().isOk());

        // La correction est désormais verrouillée (RG9/Q15 filet de sécurité).
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":18,\"commentaire\":\"Trop tard\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }
}
