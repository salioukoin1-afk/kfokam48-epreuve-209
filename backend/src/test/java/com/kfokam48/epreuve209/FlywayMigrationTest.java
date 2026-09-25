package com.kfokam48.epreuve209;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-10 — prouve que les migrations Flyway créent exactement le modèle de données du
 * diagramme D2 (6 entités + tentative_code) et que le jeu de démonstration est chargé.
 * Tourne sur H2 en mémoire : aucun prérequis sur le poste (B6).
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void lesTablesDuModeleD2Existent() {
        for (String table : new String[]{
                "promotion", "etudiant", "session_cours",
                "presence", "exercice", "relecture", "tentative_code"}) {
            Long compte = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            assertThat(compte).as("table " + table).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void lesContraintesDUniciteDuModeleD2SontPosees() {
        // UK2 : une seule présence par (session, étudiant) — RG15
        assertThat(compteIndexUnique("presence", "session_id", "etudiant_id")).isEqualTo(1);
        // UK3 : un seul exercice par (session, auteur) — support de l'upsert RG12
        assertThat(compteIndexUnique("exercice", "session_id", "etudiant_id")).isEqualTo(1);
        // UK4 : au plus une relecture par exercice — RG5
        assertThat(compteIndexUnique("relecture", "exercice_id")).isEqualTo(1);
        // UK5 : un compteur de tentatives par (session, étudiant) — RG3
        assertThat(compteIndexUnique("tentative_code", "session_id", "etudiant_id")).isEqualTo(1);
    }

    @Test
    void leJeuDeDemonstrationEstCharge() {
        Long promotions = jdbc.queryForObject("SELECT COUNT(*) FROM promotion", Long.class);
        Long etudiants = jdbc.queryForObject("SELECT COUNT(*) FROM etudiant", Long.class);
        assertThat(promotions).isGreaterThanOrEqualTo(1);
        assertThat(etudiants).isGreaterThanOrEqualTo(3);
    }

    private int compteIndexUnique(String table, String... colonnes) {
        // Identifiants en majuscules : H2 les stocke ainsi dans information_schema,
        // PostgreSQL est insensible à la casse non-quotée — comparaison portable.
        Object[] params = new Object[colonnes.length + 1];
        params[0] = table.toUpperCase();
        for (int i = 0; i < colonnes.length; i++) params[i + 1] = colonnes[i].toUpperCase();
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT tc.constraint_name) FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name " +
                "WHERE tc.table_name = ? AND tc.constraint_type = 'UNIQUE' " +
                "AND kcu.column_name IN (" + placeholders(colonnes.length) + ")",
                Integer.class, params);
        return n == null ? 0 : n;
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(i == 0 ? "?" : ",?");
        return sb.toString();
    }
}
