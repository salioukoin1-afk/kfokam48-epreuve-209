# Journal de bord — Mamadou Saliou Diallo · 209

> Une entrée par étape, écrite au moment où elle se termine. Un journal rédigé d'un bloc à la fin
> se repère dans l'historique et ne compte pas.

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges v1.1 (11 EF, 15 RG, toutes sourcées Qx/contrat), 4 diagrammes Mermaid
(D1 cas d'utilisation, D2 modèle de données avec contraintes d'unicité, D3 séquence présence avec
les codes HTTP du contrat, D4 états-transitions en bonus), 9 tickets rédigés (US-01…US-09) à
transformer en issues GitHub, contrat d'API complété (9 chemins, 10 opérations, 5 opérations
imposées à l'identique) et figé avant tout commit de code. Commit `[JALON] analyse` posé.

**Bloqué :** environ 1 h sur la section 7 : la contradiction Q10/Q15 d'abord, puis le seuil réel du
remplacement de lien (Q13 vs contrat imposé) — tranché via l'upsert POST avec verrou à `RELU`.
La CLI `gh` étant absente de la salle, les issues GitHub sont créées manuellement depuis
`docs/BACKLOG_TICKETS.md` (chaque ticket est prêt à copier).

**IA :** utilisée pour relier demande floue → exigences numérotées et pour produire un premier
jeu de tickets. Vérification faite par relecture croisée systématique : chaque renvoi EFx/RGx des
tickets et des diagrammes a été confronté au cahier des charges ; chaque code HTTP de D3 et du
contrat a été vérifié contre l'annexe B du sujet ; la section 7 a été relue pour qu'aucune
hypothèse ne reste implicite. Les décisions tranchées (Q10/Q15, upsert RG12, dépôt subordonné à la
présence, 429 RG3, casse du code) sont réécrites avec mes arguments, pas recopiées.

---

## Étape 2 — Première version

**Fait :** tous les endpoints du contrat v1.2 implémentés (les 5 opérations imposées + présence
formateur, clôture, PATCH de correction, consultation de note, liste « à relire »), en TDD :
65 tests verts sur H2 vierge (B6), CI GitHub Actions qui bloque main. Cinq migrations Flyway
(V1 schéma D2, V2/V5 jeu de démonstration, V3 séquences, V4 RG3). Cycle git flow complet pour
chaque ticket : branche → commits atomiques → push → merge --no-ff → push.

**Bloqué :** ~40 min au total sur trois pièges : les séquences BIGSERIAL après le jeu de démo à
ids explicites (V3) ; un UPDATE de clôture fait via JdbcTemplate invisible des appels MockMvc
(connexions distinctes) — résolu en clôturant par l'API dans le test US-08 ; la navigation JPQL
qui ignore mon alias Java getEtudiant() — renommée en propriété persistée auteur. Sous-tester le
jeu de démo m'a coûté deux migrations de correction : leçon retenue.

**IA :** utilisée pour produire le premier jet des entités/services/tests. Vérifié à chaque fois
par : exécution réelle de la suite de tests (rouge d'abord, vert ensuite — jamais « semble
marcher »), relecture du code généré contre le contrat ligne à ligne (codes HTTP, corps,
format d'erreur), et confrontation au CDC (chaque branche de test nomme sa RGx). Deux erreurs
d'IA attrapées de cette façon : un statut figé à 400 dans le gestionnaire d'erreurs (le contrat
exige 404 sur le tableau) et un test qui mockait le mock.

---
