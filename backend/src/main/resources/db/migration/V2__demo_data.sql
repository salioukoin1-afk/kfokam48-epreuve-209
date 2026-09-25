-- V2__demo_data.sql — jeu de démonstration : le correcteur ne doit jamais ouvrir une application vide.
-- AUCUN id explicite : les références passent par des sous-requêtes sur les noms/codes,
-- pour rester portable H2 (tests) / PostgreSQL (docker) sans manipulation de séquences.

INSERT INTO promotion (nom) VALUES ('Promotion 209 — Yaoundé');

INSERT INTO etudiant (promotion_id, nom) SELECT id, 'Awa Ndiaye'        FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';
INSERT INTO etudiant (promotion_id, nom) SELECT id, 'Boubacar Traoré'   FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';
INSERT INTO etudiant (promotion_id, nom) SELECT id, 'Chantal Mbeng'     FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';
INSERT INTO etudiant (promotion_id, nom) SELECT id, 'Djibril Faye'      FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';
INSERT INTO etudiant (promotion_id, nom) SELECT id, 'Estelle Kona'      FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';
INSERT INTO etudiant (promotion_id, nom) SELECT id, 'Fodé Camara'       FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';

INSERT INTO session_cours (promotion_id, titre, code, ouverture_at, expiration_at)
SELECT id, 'Algèbre linéaire — séance 1', 'AB12CD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '15' MINUTE
FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';

INSERT INTO session_cours (promotion_id, titre, code, ouverture_at, expiration_at)
SELECT id, 'Bases de données — séance 2', 'XY34EF', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR
FROM promotion WHERE nom = 'Promotion 209 — Yaoundé';
