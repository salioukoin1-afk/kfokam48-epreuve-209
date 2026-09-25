# Gestion de présence, dépôt et relecture d'exercices — KFOKAM48

**Épreuve finale fullstack — kfokam48-epreuve-209**

| | |
|---|---|
| Auteur | Mamadou Saliou Diallo · Matricule **209** |
| Centre | Yaoundé |
| Backend | Java 17 · Spring Boot 3 · Maven (wrapper `mvnw` commité) |
| Frontend | **React** (Vite) |
| Base | PostgreSQL 15+ · migrations Flyway |
| Démarrage | `docker compose up` (ou 3 commandes, voir plus bas) |

**Frontend : React, parce que** la SPA suffit pour trois écrans simples (formateur, étudiant, relecteur) et évite le surcoût d'un framework fullstack (Next.js) inutile ici.

---

## 1. Documentation

| Document | Contenu |
|---|---|
| [`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md) | Cahier des charges v1.1 — 10 sections, EF1–EF11, RG1–RG15, zones d'ombre et contradictions tranchées |
| [`docs/diagrammes/`](docs/diagrammes/) | D1 cas d'utilisation · D2 modèle de données · D3 séquence présence · D4 états-transitions (bonus) |
| [`docs/BACKLOG_TICKETS.md`](docs/BACKLOG_TICKETS.md) | Backlog US-01…US-09 — miroir des issues GitHub |
| [`docs/JOURNAL.md`](docs/JOURNAL.md) | Journal de bord, une entrée par étape |
| [`api/contrat.yaml`](api/contrat.yaml) | Contrat d'API — 5 opérations imposées à l'identique + extensions tracées |

**En cas de doute, `api/contrat.yaml` prime sur ce README.**

## 2. Architecture

```
backend/    Spring Boot 3 — controller / service / repository / dto / exception
            migrations Flyway dans resources/db/migration/
frontend/   React 18 + Vite — src/api (couche HTTP unique), src/pages, src/hooks
api/        contrat.yaml — source de vérité
docs/       analyse, diagrammes, journal
```

Règles non négociables (voir cahier des charges, section 8) : aucune requête base dans un
contrôleur, aucune entité JPA exposée en JSON, erreurs centralisées `@RestControllerAdvice`
(format `{code, message}`), schéma versionné Flyway uniquement, moyenne calculée côté API
jamais recalculée côté frontend, aucun `fetch` hors de `src/api/`.

## 3. Statut d'avancement

- [x] Étape 1 — Analyse : cahier des charges, 4 diagrammes, backlog, contrat figé, `[JALON] analyse`
- [x] Étape 2 — v0.1 : backend complet (9 endpoints, 69 tests verts, CI), frontend React (3 écrans, build vert), `[JALON] v0.1`
- [ ] Étape 3 — Enveloppe
- [x] Étape 4 — CHANGELOG, README testé depuis un clone vierge (voir ci-dessous), `[JALON] v1.0`
- [ ] Étape 5 — Épreuve Git (dépôt séparé `kfokam48-gitlab-209`)
- [ ] Étape 6 — Soumission

## 4. Démarrage — testé depuis un clone vierge

```bash
git clone https://github.com/salioukoin1-afk/kfokam48-epreuve-209.git
cd kfokam48-epreuve-209
docker compose up --build
```

Une seule commande. Quand les conteneurs sont montés (≈ 1 minute) :

| Service | URL |
|---|---|
| Application (UI React) | http://localhost:5173 |
| API backend | http://localhost:8080 |
| PostgreSQL | interne au réseau compose (port 5432 non publié) |

**Jeu de démonstration chargé automatiquement** (migrations Flyway V2/V5) : 1 promotion
(« Promotion 209 — Yaoundé »), 6 étudiants, 2 sessions (une active `AB12CD`, une expirée
`XY34EF`), des présences dont une ajoutée « par le formateur ». Le correcteur ouvre une
application peuplée, jamais vide.

**Scénario de vérification en 2 minutes :**
1. Écran **Formateur** → « Ouvrir une session » → le code s'affiche, communiquez-le
2. Écran **Étudiant** → choisir un nom → saisir le code → présence enregistrée
3. Toujours étudiant → déposer un lien d'exercice → le système assigne un relecteur
4. Écran **Relecteur** → choisir le nom du relecteur désigné (visible dans le tableau,
   colonne « Relectures dues ») → noter l'exercice
5. Retour écran **Étudiant** → la note et le commentaire apparaissent, sans nom de relecteur
6. Formateur → « Clôturer la session » → toute nouvelle écriture est refusée (`409 SESSION_CLOTUREE`)

Sans Docker : `./mvnw spring-boot:run` dans `backend/` (Java 17, PostgreSQL local configurable
par `DB_URL`/`DB_USER`/`DB_PASSWORD`) puis `npm install && npm run dev` dans `frontend/`.

Tests : `./mvnw test` (backend, H2 vierge — 69 tests) · `npm run build` (frontend).
