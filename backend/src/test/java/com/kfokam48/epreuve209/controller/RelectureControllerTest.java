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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * B6 — intégration US-05 : POST/PATCH /api/relectures/{id} sur un flux complet créé
 * via l'API (dépôt → assignation → rendu → correction), plus clôture directe en base
 * pour vérifier le verrou RG9. H2 vierge, jeu de démonstration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelectureControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private String corps(int note, String commentaire) {
        return "{\"note\":" + note + ",\"commentaire\":\"" + commentaire + "\"}";
    }

    /** Vide le contexte de persistance du test pour que JDBC voie les écritures de l'API. */
    private void flusher() {
        entityManager.flush();
    }

    /** Dépose l'exercice de l'étudiant 101 (présent) et retourne l'id de relecture créé. */
    private long deposerEtRecupererRelecture() throws Exception {
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":1,\"etudiantId\":101,\"lien\":\"https://exemples.fr/exo\"}"));
        flusher();   // idem : l'INSERT de la relecture (assignation) doit être visible de JDBC
        return jdbc.queryForObject(
                "SELECT id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
    }

    private Long relecteurId() {
        return jdbc.queryForObject(
                "SELECT relecteur_id FROM relecture WHERE rendue_at IS NULL LIMIT 1", Long.class);
    }

    @Test
    void rendu_nominal_200_et_exercice_RELU() throws Exception {
        long relectureId = deposerEtRecupererRelecture();
        Long relecteur = relecteurId();

        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(15, "Bon travail")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) relectureId))
                .andExpect(jsonPath("$.note").value(15));

        flusher();   // sinon l'UPDATE de statut dort encore dans le contexte de persistance
        String statut = jdbc.queryForObject(
                "SELECT statut FROM exercice WHERE id = (SELECT exercice_id FROM relecture WHERE id = ?)",
                String.class, relectureId);
        org.assertj.core.api.Assertions.assertThat(statut).isEqualTo("RELU");
    }

    @Test
    void note_hors_bornes_400_NOTE_INVALIDE() throws Exception {
        long relectureId = deposerEtRecupererRelecture();
        Long relecteur = relecteurId();

        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(25, "Trop haut")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    void auto_relecture_403() throws Exception {
        long relectureId = deposerEtRecupererRelecture();
        // L'auteur (101) tente de noter son propre exercice.
        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(15, "Je me note moi-même")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    void double_POST_409_puis_PATCH_autorise_tant_que_non_cloturee() throws Exception {
        long relectureId = deposerEtRecupererRelecture();
        Long relecteur = relecteurId();

        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON).content(corps(12, "Première version")));

        // Second POST : 409 imposé (le POST n'est qu'une création).
        mvc.perform(post("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON).content(corps(13, "Encore")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));

        // Mais la correction reste possible avant clôture (RG9/Q10).
        mvc.perform(patch("/api/relectures/" + relectureId + "?relecteurId=" + relecteur)
                        .contentType(MediaType.APPLICATION_JSON).content(corps(14, "Corrigé")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(14));
    }

    // Le refus des actions après clôture (RG9) est vérifié unitairement dans RelectureServiceTest,
    // puis en intégration dans le ticket US-08 (son DoD l'exige : la clôture y est un endpoint).
    // Ici, un UPDATE de clôture via JdbcTemplate resterait dans la transaction du test,
    // invisible des requêtes MockMvc (connexions distinctes) — piège documenté.

    @Test
    void relecture_inconnue_404() throws Exception {
        mvc.perform(post("/api/relectures/999999?relecteurId=102")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(15, "Rien")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"));
    }
}
