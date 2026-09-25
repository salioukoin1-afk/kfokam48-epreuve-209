package com.kfokam48.epreuve209.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EtudiantControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private long promotionId() {
        return jdbc.queryForObject("SELECT id FROM promotion LIMIT 1", Long.class);
    }

    @Test
    void liste_200_avec_id_et_nom() throws Exception {
        mvc.perform(get("/api/etudiants").param("promotionId", String.valueOf(promotionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].nom").isString());
    }

    @Test
    void promotion_inconnue_404() throws Exception {
        Long inconnue = jdbc.queryForObject("SELECT MAX(id) + 9999 FROM promotion", Long.class);
        mvc.perform(get("/api/etudiants").param("promotionId", String.valueOf(inconnue)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }
}
