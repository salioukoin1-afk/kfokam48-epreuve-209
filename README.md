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
- [ ] Étape 2 — v0.1 : stories Must (une branche + une PR par ticket, issues fermées par les commits)
- [ ] Étape 3 — Enveloppe
- [ ] Étape 4 — v1.0 : CHANGELOG, README testé depuis un clone vierge
- [ ] Étape 5 — Épreuve Git (dépôt séparé `kfokam48-gitlab-209`)
- [ ] Étape 6 — Soumission

## 4. Démarrage

*(à compléter à l'étape 4, testé depuis un clone vierge — cf. EF/NF5 du cahier des charges)*

```bash
# Objectif final :
docker compose up --build
# ou :
# 1. docker compose up -d db
# 2. cd backend && ./mvnw spring-boot:run
# 3. cd frontend && npm install && npm run dev
```

Jeu de démonstration chargé automatiquement au démarrage (promotions, étudiants, sessions) :
le correcteur ne doit jamais ouvrir une application vide.
