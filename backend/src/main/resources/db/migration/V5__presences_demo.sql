-- V5__presences_demo.sql — présences de démonstration (V2 n'en insérait pas).
-- Références par noms/codes (pas d'ids explicites) : portable H2 / PostgreSQL.

INSERT INTO presence (session_id, etudiant_id, source)
SELECT s.id, e.id, 'ETUDIANT' FROM session_cours s, etudiant e
WHERE s.code = 'AB12CD' AND e.nom IN ('Awa Ndiaye', 'Boubacar Traoré', 'Chantal Mbeng', 'Djibril Faye');

INSERT INTO presence (session_id, etudiant_id, source)
SELECT s.id, e.id, 'FORMATEUR' FROM session_cours s, etudiant e
WHERE s.code = 'AB12CD' AND e.nom = 'Estelle Kona';

INSERT INTO presence (session_id, etudiant_id, source)
SELECT s.id, e.id, 'ETUDIANT' FROM session_cours s, etudiant e
WHERE s.code = 'XY34EF' AND e.nom IN ('Awa Ndiaye', 'Boubacar Traoré');
