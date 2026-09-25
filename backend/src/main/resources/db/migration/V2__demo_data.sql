-- V2__demo_data.sql — jeu de démonstration : le correcteur ne doit jamais ouvrir une application vide
-- (exigence « Démarrage » du sujet). 1 promotion, 1 formateur non modélisé (pas de compte), 6 étudiants, 2 sessions.

INSERT INTO promotion (id, nom) VALUES (1, 'Promotion 209 — Yaoundé');

INSERT INTO etudiant (id, promotion_id, nom) VALUES
    (101, 1, 'Awa Ndiaye'),
    (102, 1, 'Boubacar Traoré'),
    (103, 1, 'Chantal Mbeng'),
    (104, 1, 'Djibril Faye'),
    (105, 1, 'Estelle Kona'),
    (106, 1, 'Fodé Camara');

INSERT INTO session_cours (id, promotion_id, titre, code, ouverture_at, expiration_at, cloturee_at) VALUES
    (1, 1, 'Algèbre linéaire — séance 1', 'AB12CD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '15' MINUTE, NULL),
    (2, 1, 'Bases de données — séance 2',  'XY34EF', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, NULL);
