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

**Complément (fin d'étape 2)** : la vérification de bout en bout en conteneur a attrapé trois
défauts invisibles sous MockMvc — un CHECK avec sous-requête refusé par PostgreSQL, une syntaxe
d'ALTER SEQUENCE spécifique H2, une navigation LAZY hors transaction. Chacun corrigé par une PR
(#2, #3, #4) avec CI verte. Leçon majeure : les tests passants ne prouvent pas que l'application
démarre ; seul un `docker compose up` sur base vierge le prouve. J'ai aussi appris à distinguer
un bug serveur d'un défaut de l'outil de test (un curl Windows qui envoyait du non-UTF-8) grâce
aux logs maintenant activés sur les erreurs internes.
Étape 3 — Enveloppe
Fait : bug « deux étudiants en même temps, un seul apparaît » : issue #59 ouverte avec la reproduction avant tout commit, test d'intégration concurrent (requêtes réellement simultanées, répétées) qui échoue 5/5, cause prouvée (le re-tirage RG15 insère deux fois la même relecture, la contrainte unique annule toute la transaction, présence comprise), correctif sur une branche dédiée (verrou de ligne sur l'exercice, doublon → 409), test vert 15/15, suite complète 116/116 (PR #60). Changement de besoin « deux relecteurs » : analyse mise à jour d'abord dans un commit qui le dit (#61 : RG5 révisée, RG16, EF4/EF5/EF9, DEC-13/14, D1, D2, D4, contrat v1.3), migration V4 ajoutée sans modifier V1–V3 et données existantes conservées, backend (#62, 116/116), écrans (#63, Vitest 26/26, build vert). Correctif et évolution : branches et PR séparées.

Bloqué : ~20 min pour rendre le bug reproductible : il n'apparaît que si un exercice attend un relecteur au moment des deux présences simultanées. ~10 min sur une régression de build du test e2e (extension d'import), corrigée dans sa propre PR (#66).

IA :  ia proposé les hypothèses de cause ; je n'ai retenu que celle que le test concurrent a démontrée (log duplicate key relecture_exercice_id_key). Pour le changement de besoin, j'ai tranché les zones d'ombre moi-même (DEC-13 : chaque exercice compte une fois dans la moyenne ; DEC-14 : les notes existantes restent provisoires plutôt que d'être réécrites en définitives) et imposé que la base refuse elle-même un troisième relecteur.

Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi : la correction d'une relecture (EF11, #64). Avec deux relecteurs, corriger une note modifie la note retenue et le statut provisoire : des cas que je ne pouvais pas tester correctement dans le temps restant. Q15 redevient la règle (« une fois validée, c'est fini », DEC-1 révisé) ; le contrat ne propose plus la correction. Le retrait du code correspondant est suivi par l'issue #64. EF12 et EF13 sont conservés.

Étape 4 — Version finale
Fait : CHANGELOG.md aligné sur l'historique, README (démarrage depuis un clone, URL, tests), backlog trié, jalon [JALON] v1.0. Vérifications de la version : suite backend 116/116 ; parcours v0.1 validé en navigateur (Playwright Chromium 2/2, en mode normal et en mode visible) et 50/50 contrôles curl de l'API ; écrans deux relecteurs couverts par les tests Vitest (26/26), le parcours e2e à trois étudiants est écrit et listé par Playwright.

Bloqué : la machine a manqué d'espace disque en fin de journée : l'exécution du parcours e2e à trois étudiants contre la stack reconstruite est planifiée juste après la soumission.

IA : relecture du CHANGELOG contre git log --first-parent main, une ligne par PR mergée.

design

Étape 5 — Soumission 
je devais ameliore le design 
---
