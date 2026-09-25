-- V3__aligne_sequences_demo.sql — V2 insère des ids explicites (1, 2, …) : les séquences
-- BIGSERIAL doivent repartir au-delà pour que les nouveaux enregistrements n'entrent
-- pas en collision de clé primaire.

ALTER TABLE promotion    ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE etudiant     ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE session_cours ALTER COLUMN id RESTART WITH 1000;
