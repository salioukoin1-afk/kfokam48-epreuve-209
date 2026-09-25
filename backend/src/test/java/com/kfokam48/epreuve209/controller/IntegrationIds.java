package com.kfokam48.epreuve209.controller;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Résolution des ids du jeu de démonstration PAR NOMS/CODES — plus aucun id codé en dur
 * (leçon : les ids explicites des migrations ont été retirés pour la portabilité
 * H2/PostgreSQL, les tests suivent).
 */
final class IntegrationIds {

    private final JdbcTemplate jdbc;

    IntegrationIds(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    Long etudiant(String nom) {
        return jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    }

    Long sessionParCode(String code) {
        return jdbc.queryForObject("SELECT id FROM session_cours WHERE code = ?", Long.class, code);
    }

    Long promotion() {
        return jdbc.queryForObject("SELECT id FROM promotion LIMIT 1", Long.class);
    }

    Long promotionInconnue() {
        return jdbc.queryForObject("SELECT MAX(id) + 9999 FROM promotion", Long.class);
    }
}
