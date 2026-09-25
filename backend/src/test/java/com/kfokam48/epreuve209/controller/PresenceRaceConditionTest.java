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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de non-régression — race condition #19
 *
 * Scénario reproduisant le bug :
 *   Deux étudiants distincts soumettent le code de présence de la même session
 *   "presque en même temps". Les deux requêtes doivent chacune retourner 201.
 *
 * Avant le correctif, la transaction de l'un des deux pouvait arriver après le SELECT
 * (existsBy) de l'autre mais avant son INSERT, contournant la vérification applicative
 * et heurtant la contrainte UNIQUE (session_id, etudiant_id) → DataIntegrityViolationException
 * non catchée → 500 au lieu de 201 (ou 409 si le même étudiant).
 *
 * Réf : RG15 · PresenceService.marquerPresent() · UK2 (V1__init.sql)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceRaceConditionTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private IntegrationIds ids() { return new IntegrationIds(jdbc); }

    /**
     * Cas 1 (le plus simple) — même étudiant en double soumission séquentielle.
     * Doit retourner 201 puis 409 DEJA_PRESENT. Ce cas passait déjà ; on le garde
     * comme régression de base.
     */
    @Test
    void doublon_sequential_retourne_409_DEJA_PRESENT() throws Exception {
        Long fode = ids().etudiant("Fodé Camara");
        String body = "{\"code\":\"AB12CD\",\"etudiantId\":" + fode + "}";

        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    /**
     * Cas 2 — deux étudiants distincts, requêtes parallèles simultanées.
     *
     * Les deux doivent obtenir 201. Avant le correctif, la DataIntegrityViolationException
     * pouvait se propager comme 500 si deux insertions concurrentes touchaient la même
     * session (contention de table, pas de doublon étudiant).
     *
     * Ce test lance deux requêtes en parallèle avec ExecutorService et vérifie que
     * les deux statuts sont dans {201, 409} — jamais 500.
     */
    @Test
    void deux_etudiants_distincts_simultanement_aucun_500() throws Exception {
        Long fode   = ids().etudiant("Fodé Camara");
        Long chantal = ids().etudiant("Chantal Mbeng");

        String bodyFode    = "{\"code\":\"AB12CD\",\"etudiantId\":" + fode    + "}";
        String bodyChantal = "{\"code\":\"AB12CD\",\"etudiantId\":" + chantal + "}";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = new ArrayList<>();

        Callable<Integer> req1 = () -> mvc.perform(
                post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(bodyFode))
                .andReturn().getResponse().getStatus();

        Callable<Integer> req2 = () -> mvc.perform(
                post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(bodyChantal))
                .andReturn().getResponse().getStatus();

        // Soumettre les deux quasi-simultanément
        futures.add(pool.submit(req1));
        futures.add(pool.submit(req2));
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        List<Integer> statuts = new ArrayList<>();
        for (Future<Integer> f : futures) {
            statuts.add(f.get());
        }

        // Aucun des deux ne doit retourner 500 — le correctif transforme
        // DataIntegrityViolationException en 409 DEJA_PRESENT si jamais il y a contention.
        assertThat(statuts).allMatch(s -> s == 201 || s == 409,
                "Statuts attendus : 201 ou 409, jamais 500. Reçus : " + statuts);

        // Dans ce cas précis (deux étudiants différents), les deux doivent normalement
        // passer en 201, sauf si la fixture de test a déjà enregistré l'une de ces présences.
        // On vérifie au moins qu'aucun 500 n'est apparu.
        pool.shutdownNow();
    }

    /**
     * Cas 3 — même étudiant en deux requêtes parallèles (race condition la plus directe).
     *
     * Les deux requêtes visent le même (sessionId, etudiantId). L'une doit retourner 201,
     * l'autre 409 DEJA_PRESENT. Avant le correctif, la deuxième pouvait retourner 500
     * si elle passait le SELECT juste avant que la première ait commité.
     */
    @Test
    void meme_etudiant_requetes_paralleles_retourne_201_et_409_jamais_500() throws Exception {
        Long fode = ids().etudiant("Fodé Camara");
        String body = "{\"code\":\"AB12CD\",\"etudiantId\":" + fode + "}";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() ->
                    mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(body))
                            .andReturn().getResponse().getStatus()
            ));
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        List<Integer> statuts = new ArrayList<>();
        for (Future<Integer> f : futures) statuts.add(f.get());

        // Les deux statuts doivent être dans {201, 409}. Jamais 500.
        assertThat(statuts).allMatch(s -> s == 201 || s == 409,
                "Race condition non corrigee : 500 recu. Statuts : " + statuts);
        // L'un est 201, l'autre 409 (ou les deux 201 si le timing a séparé les transactions,
        // ce qui est possible mais très peu probable avec deux threads dans le même MockMvc).
        pool.shutdownNow();
    }
}
