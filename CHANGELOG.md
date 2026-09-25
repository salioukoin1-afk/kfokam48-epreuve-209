# CHANGELOG — kfokam48-epreuve-209

Format : Keep a Changelog. Les versions correspondent aux jalons de l'épreuve.

## [v1.0] — Étape 4 · Version finale

### Ajouté
- **Frontend React (Vite)** — 3 écrans imposés (F2) :
  - Formateur : ouvrir une session (code affiché en grand), clôturer, ajouter une présence à la main, tableau récapitulatif
  - Étudiant : marquer sa présence, déposer/remplacer son exercice, consulter sa note
  - Relecteur : liste « à relire », rendu de note/commentaire
  - Couche API unique `src/api/` (F3) : aucun fetch ailleurs, moyenne jamais recalculée côté client, états de chargement/erreur avec bouton Réessayer
  - Build de production vert (F1) ; servi par nginx avec proxy `/api` (une seule origine)
- **GET /api/relectures/en-attente** — liste des exercices à relire d'un relecteur (EF6/RG10), y compris après clôture (Q11)
- **Service RelecturesEnAttente transactionnel** + journalisation des erreurs internes côté serveur (B4 debuggable)
- CI GitHub Actions : tests backend obligatoires sur `main` et les PR (B6)
- `docker compose up` complet : db (healthcheck) + backend + frontend, jeu de démonstration chargé par Flyway

### Corrigé (découverts par la vérification de bout en bout en conteneur — PR #2, #3, #4)
- CHECK avec sous-requête invalide sur PostgreSQL (la défense RG4 reste dans le service, testée)
- Migrations rendues portables H2/PostgreSQL : plus d'ids explicites, références par noms/codes, V3 supprimée
- LazyInitializationException sur `/api/relectures/en-attente` : navigation LAZY déplacée dans un service `@Transactional(readOnly=true)`
- Erreurs internes désormais loggées (avant : 500 silencieux indébuggable)

### Sécurité / hygiène
- `.env.example` fourni, aucun secret commité, `.gitignore` Java+JS posé avant tout commit de code

## [v0.1] — Étape 2 · Première version

### Ajouté
- **Analyse complète** (étape 1, jalon `[JALON] analyse`) : cahier des charges v1.3 (11 EF, 15 RG sourcées), 4 diagrammes Mermaid (D1 cas d'utilisation, D2 modèle de données + contraintes UK1–UK5, D3 séquence présence, D4 états-transitions bonus), backlog US-01…US-10, contrat d'API figé avant tout code
- **Les 5 opérations imposées** du contrat, à la lettre :
  - `POST /api/sessions` (201/400) — code unique sans 0/O/1/I, expiration configurable (RG1/US-09)
  - `POST /api/presences` (201/400/409/410) — ordre de vérifications de D3, blocage RG3 en 429 avant toute recherche de code
  - `POST /api/exercices` (201/400/409) — upsert RG12 verrouillé à RELU, assignation immédiate au hasard parmi les présents hors auteur (RG6/RG14)
  - `POST /api/relectures/{id}` (200/400/403/409) — RG4/RG5/RG8 bornes 0–20 incluses, POST = création stricte
  - `GET /api/tableau` (200/404) — agrégation 100 % serveur, moyenne null distincte de 0
- **Extensions tracées** du contrat v1.2 : présence formateur (Q14/RG13), clôture de session (EF11), PATCH de correction avant clôture (Q10/RG9), consultation de sa note avec anonymat garanti au niveau du DTO (RG7, test dédié)
- 5 migrations Flyway (schéma D2 + jeu de démonstration + RG3) — jamais `ddl-auto`
- 69 tests verts sur H2 vierge (unitaires sur les RGx + intégration sur les endpoints)
- Gestion centralisée des erreurs au format imposé `{code, message}` (B4)

### Décisions documentées (cahier des charges, section 7)
- Q10 vs Q15 : la note reste corrigeable jusqu'à la clôture (RG9)
- Upsert RG12 : remplacement du lien possible jusqu'à RELU (sinon Q13 inapplicable)
- Dépôt subordonné à la présence (400 NON_PRESENT)
- RG3 : compteur des codes inconnus global à l'étudiant, 429 BLOCAGE_TENTATIVES
- Code de présence insensible à la casse
