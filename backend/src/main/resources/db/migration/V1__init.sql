-- V1__init.sql — modèle de données conforme au diagramme D2 (v1.2)
-- UK1 : code de session unique (EF1) · UK2 : RG15 · UK3 : unicité (session, auteur) pour l'upsert RG12
-- UK4 : RG5 · UK5 : RG3 · rg9_coherence : rendu ⇒ note présente

CREATE TABLE promotion (
    id   BIGSERIAL PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL
);

CREATE TABLE etudiant (
    id            BIGSERIAL PRIMARY KEY,
    promotion_id  BIGINT       NOT NULL REFERENCES promotion(id),
    nom           VARCHAR(100) NOT NULL
);

CREATE TABLE session_cours (
    id            BIGSERIAL PRIMARY KEY,
    promotion_id  BIGINT       NOT NULL REFERENCES promotion(id),
    titre         VARCHAR(100) NOT NULL,
    code          VARCHAR(10)  NOT NULL,
    ouverture_at  TIMESTAMP    NOT NULL,
    expiration_at TIMESTAMP    NOT NULL,
    cloturee_at   TIMESTAMP    NULL,
    CONSTRAINT uk_session_code UNIQUE (code)                -- UK1
);

CREATE TABLE presence (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT    NOT NULL REFERENCES session_cours(id),
    etudiant_id BIGINT    NOT NULL REFERENCES etudiant(id),
    source      VARCHAR(10) NOT NULL CHECK (source IN ('ETUDIANT', 'FORMATEUR')),  -- RG13
    cree_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_presence_session_etudiant UNIQUE (session_id, etudiant_id)           -- UK2 / RG15
);

CREATE TABLE exercice (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT    NOT NULL REFERENCES session_cours(id),
    etudiant_id BIGINT    NOT NULL REFERENCES etudiant(id),
    lien        VARCHAR(500) NOT NULL,
    statut      VARCHAR(25)  NOT NULL CHECK (statut IN ('DEPOSE', 'EN_ATTENTE_RELECTEUR', 'EN_RELECTURE', 'RELU')),
    depose_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_exercice_session_etudiant UNIQUE (session_id, etudiant_id)           -- UK3 (upsert RG12)
);

CREATE TABLE relecture (
    id           BIGSERIAL PRIMARY KEY,
    exercice_id  BIGINT      NOT NULL REFERENCES exercice(id),
    relecteur_id BIGINT      NOT NULL REFERENCES etudiant(id),
    note         INT         NULL CHECK (note IS NULL OR (note BETWEEN 0 AND 20)),     -- RG8, bornes incluses
    commentaire  VARCHAR(2000) NULL,
    rendue_at    TIMESTAMP   NULL,
    maj_at       TIMESTAMP   NULL,
    CONSTRAINT uk_relecture_exercice UNIQUE (exercice_id),                              -- UK4 / RG5
    -- RG4 (relecteur ≠ auteur) ne peut PAS être une contrainte CHECK : PostgreSQL interdit
    -- les sous-requêtes dedans (découvert au premier docker compose up — H2 l'acceptait).
    -- Elle est appliquée en défense dans RelectureService, testée unitairement + en intégration,
    -- et l'assignation automatique (RG6/RG14) ne peut de toute façon jamais la produire.
    CONSTRAINT rg9_coherence CHECK (rendue_at IS NULL OR note IS NOT NULL)
);

CREATE TABLE tentative_code (
    id                 BIGSERIAL PRIMARY KEY,
    session_id         BIGINT    NOT NULL REFERENCES session_cours(id),
    etudiant_id        BIGINT    NOT NULL REFERENCES etudiant(id),
    echecs_consecutifs INT       NOT NULL DEFAULT 0,
    bloque_jusqua      TIMESTAMP NULL,
    CONSTRAINT uk_tentative_session_etudiant UNIQUE (session_id, etudiant_id)          -- UK5 / RG3
);

CREATE INDEX idx_presence_session   ON presence (session_id);
CREATE INDEX idx_exercice_session   ON exercice (session_id);
CREATE INDEX idx_exercice_auteur    ON exercice (etudiant_id);
CREATE INDEX idx_relecture_relecteur ON relecture (relecteur_id);
