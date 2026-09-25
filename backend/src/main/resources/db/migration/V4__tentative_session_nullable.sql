-- V4__tentative_session_nullable.sql — précision RG3 (décision documentée, CDC v1.3) :
-- un échec de « code inconnu » ne permet pas d'identifier la session (c'est précisément
-- l'attaque de devinette visée par Q4) : le compteur est alors porté par l'étudiant seul,
-- via une ligne tentative_code avec session_id NULL.

ALTER TABLE tentative_code ALTER COLUMN session_id DROP NOT NULL;
