package com.kfokam48.epreuve209.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de non-régression — race condition #19
 *
 * Scénario reproduisant le bug :
 *   Deux requêtes POST /api/presences « presque en même temps » sur la même session.
 *   Une seule présence doit être enregistrée, l'autre doit être refusée en 409.
 *
 * Avant le correctif, la transaction de l'une des deux pouvait arriver après le SELECT
 * (existsBy) de l'autre mais avant son INSERT, contournant la vérification applicative
 * et heurtant la contrainte UNIQUE (session_id, etudiant_id) → DataIntegrityViolationException
 * non catchée → 500 au lieu de 409.
 *
 * Réf : RG15 · PresenceService.marquerPresent() · UK2 (V1__init.sql)
 *
 * <p><b>Isolation</b> : ce test n'est PAS @Transactional — il doit.commit() pour que la
 * contrainte UK2 soit réellement évaluée par la base. En contrepartie il travaille sur une
 * session créée pour l'occasion et la supprime en @AfterEach, sinon il pollue le contexte
 * H2 partagé et fait échouer les autres tests d'intégration (constaté en CI : le job
 * « Backend » est rouge sur PresenceControllerTest.nominal_201, vert en local — l'ordre
 * d'exécution des classes masquait la pollution).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceRaceConditionTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private String code;
    private Long sessionId;

    private IntegrationIds ids() { return new IntegrationIds(jdbc); }

    @BeforeEach
    void sessionDediee() throws Exception {
        String reponse = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Race\",\"promotionId\":" + ids().promotion() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        code = reponse.replaceAll(".*\"code\":\"([A-Z0-9]+)\".*", "$1");
        sessionId = ids().sessionParCode(code);
    }

    @AfterEach
    void nettoyage() {
        jdbc.update("DELETE FROM presence WHERE session_id = ?", sessionId);
        jdbc.update("DELETE FROM tentative_code WHERE session_id = ?", sessionId);
        jdbc.update("DELETE FROM session_cours WHERE id = ?", sessionId);
    }

    private String corps(long etudiantId) {
        return "{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}";
    }

    private List<Integer> enParallele(String... bodies) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(bodies.length);
        try {
            // CountDownLatch : les N requêtes partent literalmente au même instant, ce qui
            // maximise la chance que les deux SELECT passent avant le premier INSERT.
            java.util.concurrent.CountDownLatch depart = new java.util.concurrent.CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (String b : bodies) {
                futures.add(pool.submit(() -> {
                    depart.await();
                    return mvc.perform(post("/api/presences")
                                    .contentType(MediaType.APPLICATION_JSON).content(b))
                            .andReturn().getResponse().getStatus();
                }));
            }
            depart.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

            List<Integer> statuts = new ArrayList<>();
            for (Future<Integer> f : futures) statuts.add(f.get());
            return statuts;
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Cas 1 — même étudiant, deux requêtes strictement simultanées.
     * Une doit créer (201), l'autre doit être refusée en 409 DEJA_PRESENT.
     * Avant le correctif, la seconde pouvait retourner 500.
     */
    @Test
    void meme_etudiant_simultane_201_puis_409_jamais_500() throws Exception {
        Long fode = ids().etudiant("Fodé Camara");

        List<Integer> statuts = enParallele(corps(fode), corps(fode));

        assertThat(statuts)
                .as("Une seule présence créée, l'autre refusée en 409 — jamais 500")
                .containsExactlyInAnyOrder(201, 409);

        // Et la base ne contient bien qu'une seule ligne pour ce couple.
        Integer nb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM presence WHERE session_id = ? AND etudiant_id = ?",
                Integer.class, sessionId, fode);
        assertThat(nb).isEqualTo(1);
    }

    /**
     * Cas 2 — le corps de l'erreur est bien celui du contrat, pas une 500 opaque.
     * Vérifie le format d'erreur imposé B2/B4 sur le chemin de la concurrence.
     */
    @Test
    void simultanite_renvoie_le_format_derreur_impose() throws Exception {
        Long fode = ids().etudiant("Fodé Camara");

        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps(fode)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps(fode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.message").isString());
    }

    /**
     * Cas 3 — deux étudiants distincts tapent le code en même temps (le cas exact du
     * client : « deux étudiants côte à côte, un seul apparaît dans ma liste »).
     * Les deux doivent être 201, et les deux présences doivent exister en base.
     * C'est ce test qui a échoué avant le correctif.
     */
    @Test
    void deux_etudiants_distincts_simultanes_obtiennent_tous_deux_201() throws Exception {
        Long fode   = ids().etudiant("Fodé Camara");
        Long chantal = ids().etudiant("Chantal Mbeng");

        List<Integer> statuts = enParallele(corps(fode), corps(chantal));

        assertThat(statuts)
                .as("Deux étudiants différents : les deux présences doivent être enregistrées (201)")
                .containsExactlyInAnyOrder(201, 201);

        Integer nb = jdbc.queryForObject(
                "SELECT COUNT(*) FROM presence WHERE session_id = ?", Integer.class, sessionId);
        assertThat(nb).isEqualTo(2);
    }

    /**
     * Cas 4 — six requêtes simultanées pour le même étudiant (pic de charge).
     * Une seule présence, cinq 409, zéro 500. Le correctif tient sous contention.
     */
    @Test
    void six_requetes_simultanes_un_seul_201() throws Exception {
        Long fode = ids().etudiant("Fodé Camara");
        String[] bodies = new String[6];
        java.util.Arrays.fill(bodies, corps(fode));

        List<Integer> statuts = enParallele(bodies);

        assertThat(statuts).as("Un seul 201, le reste 409, aucun 500").containsOnly(201, 409);
        assertThat(statuts).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuts).filteredOn(s -> s == 409).hasSize(5);
    }
}
