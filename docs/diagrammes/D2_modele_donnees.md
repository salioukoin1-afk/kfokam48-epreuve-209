# D2 — Modèle de données (classes)

> Entités, attributs et cardinalités. **Ce diagramme doit correspondre aux migrations Flyway** : la migration `V1` crée `promotion`, `etudiant`, `session_cours`, `presence`, `exercice`, `relecture` et la table de suivi `tentative_code` (RG3). Les contraintes d'unicité matérialisent RG15, RG5 et l'unicité (session, auteur).

```mermaid
erDiagram
    PROMOTION ||--o{ ETUDIANT : "regroupe"
    PROMOTION ||--o{ SESSION_COURS : "structure"
    SESSION_COURS ||--o{ PRESENCE : "recense"
    ETUDIANT ||--o{ PRESENCE : "prouve"
    SESSION_COURS ||--o{ EXERCICE : "reçoit"
    ETUDIANT ||--o{ EXERCICE : "dépose (auteur)"
    EXERCICE ||--o| RELECTURE : "subit (0..1)"
    ETUDIANT ||--o| RELECTURE : "effectue (relecteur)"
    SESSION_COURS ||--o{ TENTATIVE_CODE : "comptabilise"
    ETUDIANT ||--o{ TENTATIVE_CODE : "est limité par"

    PROMOTION {
        bigint id PK
        varchar nom
    }

    ETUDIANT {
        bigint id PK
        bigint promotion_id FK
        varchar nom
    }

    SESSION_COURS {
        bigint id PK
        bigint promotion_id FK
        varchar titre
        varchar code UK "unique, non devinable"
        timestamp ouverture_at
        timestamp expiration_at "ouverture_at + durée configurable (RG1)"
        timestamp cloturee_at "NULL tant que non clôturée (RG9/RG11)"
    }

    PRESENCE {
        bigint id PK
        bigint session_id FK
        bigint etudiant_id FK
        varchar source "ETUDIANT | FORMATEUR (RG13)"
        timestamp cree_at
    }

    EXERCICE {
        bigint id PK
        bigint session_id FK
        bigint etudiant_id FK "l'auteur"
        varchar lien "dernier lien soumis (RG12 upsert)"
        varchar statut "DEPOSE | EN_ATTENTE_RELECTEUR | EN_RELECTURE | RELU (D4)"
        timestamp depose_at
    }

    RELECTURE {
        bigint id PK
        bigint exercice_id FK "unique — RG5 : au plus une relecture par exercice"
        bigint relecteur_id FK "≠ auteur (RG4) — anonyme pour l'auteur (RG7)"
        int note "NULL tant que non rendue ; entier 0–20 bornes incluses au rendu (RG8)"
        varchar commentaire "NULL tant que non rendue"
        timestamp rendue_at "NULL = relecteur assigné, note pas encore rendue (état EN_RELECTURE de D4)"
        timestamp maj_at "dernière correction (PATCH, RG9)"
    }

    TENTATIVE_CODE {
        bigint id PK
        bigint session_id FK
        bigint etudiant_id FK
        int echecs_consecutifs "à 5 → blocage 2 min (RG3)"
        timestamp bloque_jusqua "NULL si non bloqué"
    }
```

**Contraintes d'unicité (matérialisées en base) :**

| Contrainte | Colonnes | Règle source |
|---|---|---|
| UK1 | `session_cours.code` | code unique par session (EF1) |
| UK2 | `presence (session_id, etudiant_id)` | RG15 — une seule présence par session |
| UK3 | `exercice (session_id, etudiant_id)` | un seul exercice par (session, auteur) — support de l'upsert RG12 |
| UK4 | `relecture.exercice_id` | RG5 — au plus un relecteur par exercice |
| UK5 | `tentative_code (session_id, etudiant_id)` | RG3 — un compteur par (session, étudiant) |

**Lecture :**
- `EXERCICE.etudiant_id` (l'auteur) et `RELECTURE.relecteur_id` sont deux rôles du même `ETUDIANT` : une contrainte applicative (service, RG4) interdit `relecteur_id = exercice.etudiant_id` — elle ne peut pas s'exprimer comme une simple clé étrangère.
- `SESSION_COURS.cloturee_at` nullable est le pivot des verrouillages RG2/RG9/RG11.
- Le statut de l'exercice est dérivable (`EN_RELECTURE` ⇔ relecture existante non rendue, etc.) mais stocké pour refléter D4 et simplifier le tableau.
- **Correction v1.2** : la ligne `relecture` est créée **à l'assignation** (RG14), avec `note`/`commentaire`/`rendue_at` nullables — c'est ce qui rend l'état `EN_RELECTURE` de D4 persistant. Le rendu (EF7) remplit `note`, `commentaire` et `rendue_at` ; contrainte de cohérence en base : `rendue_at IS NULL OR note IS NOT NULL`. Aucune décision client (Qx/RGx) n'est modifiée par cette correction — c'est une cohérence interne entre D2 et D4.
