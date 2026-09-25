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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B6 — intégration US-05 : POST/PATCH /api/relectures/{id} sur un flux complet créé
 * via l'API (dépôt → assignation → rendu → correction). Ids par noms/codes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelectureControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private void flusher() { entityManager.flush(); }
    private IntegrationIds ids() { return new IntegrationIds(jdbc); }

    private String corps(int note, String commentaire) {
        return "{\"note\":" + note + ",\"commentaire\":\"" + commentaire + "\"}";
    }

    /** Dépose l'exercice d'Awa (présente) et retourne [relectureId, relecteurId]. */
    private long[] deposerEtRecupererRelecture() throws Exception {
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + ids().sessionParCode("AB12CD")
                        + ",\"etudiantId\":" + ids().etudiant("Awa Ndiaye")
                        + ",\"lien\":\"https://exemples.fr/exo\"}"));
        flusher();   // sinon l'assignation n'est pas encore visible de JDBC
        Long relectureId = jdbc.queryForObject(
                "SELECT id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        Long relecteurId = jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
        return new long[]{relectureId, relecteurId};
    }

    @Test
    void rendu_nominal_200_et_exercice_RELU() throws Exception {
        long[] ids = deposerEtRecupererRelecture();

        mvc.perform(post("/api/relectures/" + ids[0] + "?relecteurId=" + ids[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(15, "Bon travail")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ids[0]))
                .andExpect(jsonPath("$.note").value(15));

        flusher();   // sinon l'UPDATE de statut dort dans le contexte de persistance
        assertThat(jdbc.queryForObject(
                "SELECT statut FROM exercice WHERE id = (SELECT exercice_id FROM relecture WHERE id = ?)",
                String.class, ids[0])).isEqualTo("RELU");
    }

    @Test
    void note_hors_bornes_400_NOTE_INVALIDE() throws Exception {
        long[] ids = deposerEtRecupererRelecture();

        mvc.perform(post("/api/relectures/" + ids[0] + "?relecteurId=" + ids[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(25, "Trop haut")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    void auto_relecture_403() throws Exception {
        long[] ids = deposerEtRecupererRelecture();
        // L'auteure (Awa) tente de noter son propre exercice.
        mvc.perform(post("/api/relectures/" + ids[0] + "?relecteurId=" + ids().etudiant("Awa Ndiaye"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(15, "Je me note moi-même")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    void double_POST_409_puis_PATCH_autorise_tant_que_non_cloturee() throws Exception {
        long[] ids = deposerEtRecupererRelecture();

        mvc.perform(post("/api/relectures/" + ids[0] + "?relecteurId=" + ids[1])
                .contentType(MediaType.APPLICATION_JSON).content(corps(12, "Première version")));

        mvc.perform(post("/api/relectures/" + ids[0] + "?relecteurId=" + ids[1])
                        .contentType(MediaType.APPLICATION_JSON).content(corps(13, "Encore")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));

        mvc.perform(patch("/api/relectures/" + ids[0] + "?relecteurId=" + ids[1])
                        .contentType(MediaType.APPLICATION_JSON).content(corps(14, "Corrigé")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(14));
    }

    @Test
    void relecture_inconnue_404() throws Exception {
        mvc.perform(post("/api/relectures/999999?relecteurId=" + ids().etudiant("Boubacar Traoré"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(15, "Rien")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }

    // Le refus après clôture (RG9) est vérifié unitairement (RelectureServiceTest) puis en
    // intégration dans ClotureControllerTest — la clôture y est un endpoint de l'API.
}
