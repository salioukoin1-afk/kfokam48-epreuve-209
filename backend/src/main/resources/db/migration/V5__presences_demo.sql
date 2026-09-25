-- V5__presences_demo.sql — V2 n'insérait aucune présence : les tests d'intégration du dépôt
-- d'exercice (NON_PRESENT) et le flux relecteur en dépendaient. Complété par migration V5
-- (une migration livrée n'est jamais éditée).

INSERT INTO presence (session_id, etudiant_id, source, cree_at) VALUES
    (1, 101, 'ETUDIANT',  CURRENT_TIMESTAMP),
    (1, 102, 'ETUDIANT',  CURRENT_TIMESTAMP),
    (1, 103, 'ETUDIANT',  CURRENT_TIMESTAMP),
    (1, 104, 'ETUDIANT',  CURRENT_TIMESTAMP),
    (1, 105, 'FORMATEUR', CURRENT_TIMESTAMP),                       -- démo RG13 : badge « ajouté par le formateur »
    (2, 101, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
    (2, 102, 'ETUDIANT',  CURRENT_TIMESTAMP - INTERVAL '2' HOUR);
