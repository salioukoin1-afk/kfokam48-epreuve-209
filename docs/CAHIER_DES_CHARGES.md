# Cahier des charges — Gestion de présence, dépôt et relecture d'exercices — KFOKAM48

Auteur : Mamadou Saliou Diallo · Matricule 209 · Centre Yaoundé · Version 1.1 · Frontend choisi : React, parce que la SPA suffit pour 3 écrans simples et évite le surcoût d'un framework fullstack (Next.js) inutile ici.

## 1. Contexte et objectif

La direction de la formation KFOKAM48 souhaite outiller trois moments d'une session de cours : la prise de présence, le dépôt d'exercices par les étudiants, et une relecture par les pairs. L'objectif est de remplacer un suivi manuel (feuille de présence papier, exercices envoyés par messagerie, relectures non tracées) par une application qui centralise ces trois flux et donne au formateur une vue consolidée par étudiant : présence, dépôts, moyenne des notes reçues, relectures encore dues.

Le problème concret que ça résout : le formateur n'a aujourd'hui aucune vue fiable et instantanée de qui a été présent, qui a rendu son travail, et qui doit encore relire celui d'un autre.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire |
|---|---|
| **Formateur** | Ouvre une session de cours et obtient un code de présence ; ajoute une présence manuellement ; clôture une session ; consulte le tableau récapitulatif par étudiant. |
| **Étudiant** | Saisit un code pour marquer sa présence ; dépose (ou remplace) le lien de son exercice pour une session ; consulte la note et le commentaire reçus sur son propre exercice. |
| **Relecteur** | Rôle porté par un étudiant, assigné automatiquement par le système sur un exercice qui n'est pas le sien ; envoie une note (0–20) et un commentaire ; peut corriger sa note jusqu'à la clôture de la session. |

Un même compte « étudiant » endosse donc, selon le contexte, le rôle d'auteur d'exercice et le rôle de relecteur — jamais les deux sur le même exercice (RG4).

## 3. Périmètre

**Inclus :**
- Ouverture de session avec code de présence à durée de vie limitée
- Prise de présence par code, et ajout manuel par le formateur
- Dépôt et remplacement du lien d'un exercice
- Assignation automatique et anonyme d'un relecteur parmi les étudiants présents
- Notation (0–20, entière) et commentaire de relecture, modifiables jusqu'à clôture
- Tableau récapitulatif par étudiant pour le formateur
- Clôture de session par le formateur

**Explicitement exclu :**
- Authentification par mot de passe (Q1) — l'étudiant est choisi dans une liste, pas de gestion de comptes/sessions HTTP sécurisées
- Gestion de plusieurs formateurs par session, coanimation
- Notifications (email, push) de tout type
- Historique de modification des notes (on garde seulement la dernière valeur)
- Export ou reporting au-delà du tableau demandé (GET /api/tableau)
- Gestion des promotions elles-mêmes (création/édition) — on suppose des promotions et étudiants déjà existants en base (jeu de données de démonstration)

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session de cours pour une promotion | Quand je POST `/api/sessions` avec un titre et une promotion valides, je reçois 201 avec un code de présence unique et une date d'expiration à +15 min | Must |
| EF2 | L'étudiant marque sa présence avec un code | Quand je saisis un code valide et non expiré, ma présence apparaît dans le tableau du formateur avec `source=ETUDIANT` | Must |
| EF3 | Le formateur ajoute une présence manuellement | Quand le formateur ajoute une présence, elle apparaît dans le tableau avec `source=FORMATEUR`, visuellement distinguée | Should |
| EF4 | L'étudiant dépose le lien de son exercice pour une session | Quand je POST `/api/exercices` avec un lien valide, l'exercice apparaît avec le statut `DEPOSE` puis passe en attente de relecteur | Must |
| EF5 | L'étudiant remplace le lien de son exercice | Tant qu'aucune relecture n'a démarré sur mon exercice, un nouveau dépôt remplace l'ancien lien sans créer de doublon | Should |
| EF6 | Le système assigne un relecteur à un exercice déposé | À la création de l'exercice, un étudiant présent à la session, différent de l'auteur, est assigné comme relecteur ; si aucun candidat n'existe, l'exercice reste visible comme « en attente de relecteur » | Must |
| EF7 | Le relecteur note et commente un exercice qui lui est assigné | Quand je POST `/api/relectures/{id}` avec une note 0–20 entière et un commentaire, la relecture est enregistrée et la moyenne de l'étudiant relu se met à jour dans le tableau | Must |
| EF8 | Le relecteur corrige sa note avant clôture de la session | Tant que la session n'est pas clôturée, un nouveau POST sur la même relecture remplace note et commentaire | Should |
| EF9 | L'étudiant relu consulte sa note et le commentaire reçus | Je vois la note et le commentaire sur mon exercice, jamais l'identité du relecteur | Must |
| EF10 | Le formateur consulte le tableau récapitulatif de sa promotion | GET `/api/tableau?promotionId=` retourne, par étudiant, présences, exercices déposés, moyenne (nulle si aucune note reçue), et relectures encore dues | Must |
| EF11 | Le formateur clôture une session | Après clôture, plus aucune présence, dépôt ou correction de note n'est acceptée sur cette session | Should |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| NF1 | Volumétrie modeste : une promotion de ~30 étudiants, quelques sessions par semaine | Pas de pagination exigée sur le tableau ; testé avec un jeu de démo de cet ordre |
| NF2 | Utilisation mobile pour l'étudiant (saisie du code, dépôt de lien) | Écrans étudiant/relecteur responsives, testés en largeur téléphone |
| NF3 | Temps de réponse perçu < 1 s sur les 5 opérations du contrat | Vérifié à l'œil en local avec le jeu de démo, pas de test de charge exigé ici |
| NF4 | Aucune fuite d'information sensible en cas d'erreur | Aucune stack trace ne doit jamais atteindre le client (B4) |
| NF5 | Application démarrable par un tiers sans configuration manuelle | `docker compose up` ou 3 commandes documentées, testées depuis un clone vierge |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| RG2 | Aucune présence ne peut être marquée après expiration du code ou clôture de la session | Q2, Q3 |
| RG3 | Après 5 échecs consécutifs de saisie de code sur une session, l'étudiant est bloqué 2 minutes | Q4 |
| RG4 | Un étudiant ne peut jamais relire son propre exercice | Q5 |
| RG5 | Un exercice a au plus un relecteur | Q6 |
| RG6 | Le relecteur est choisi automatiquement, au hasard, parmi les étudiants présents à la session, à l'exclusion de l'auteur | Q7 |
| RG7 | L'étudiant relu voit la note et le commentaire, jamais l'identité du relecteur | Q8 |
| RG8 | La note est un entier compris entre 0 et 20 inclus | Q9 |
| RG9 | Le relecteur peut corriger une note déjà envoyée tant que le formateur n'a pas clôturé la session ; passé la clôture, la note est définitive | Décision tranchée — voir section 7, Q10 vs Q15 |
| RG10 | Un exercice dont la relecture n'a jamais été rendue reste au statut « en attente » et doit apparaître comme tel dans le tableau du formateur | Q11 |
| RG11 | Le dépôt d'un exercice reste possible jusqu'à la clôture de la session, pas seulement jusqu'à sa fin planifiée | Q12 |
| RG12 | Le lien d'un exercice peut être remplacé (via un nouvel appel `POST /api/exercices`, en upsert) à tout moment tant que la relecture n'a pas été rendue (statut ≠ `RELU`) — y compris pendant qu'un relecteur est déjà assigné et travaille dessus ; une fois la note rendue, un nouvel appel retourne `409 EXERCICE_DEJA_DEPOSE` | Q13 — seuil précisé en section 7 |
| RG13 | Une présence ajoutée manuellement par le formateur est marquée `source=FORMATEUR`, visuellement distincte d'une présence par code | Q14 |
| RG14 | L'assignation du relecteur a lieu immédiatement au moment du dépôt de l'exercice, parmi les étudiants déjà présents à cet instant, hors auteur ; si aucun candidat n'est éligible, l'exercice reste sans relecteur et apparaît « en attente de relecteur » dans le tableau | Zone d'ombre — voir section 7 |
| RG15 | Un étudiant ne peut avoir qu'une seule présence par session (pas de doublon) | Déduit du contrat (409 sur présence déjà enregistrée) |

## 7. Zones d'ombre, hypothèses et contradictions tranchées

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Pourquoi |
|---|---|---|---|
| Modification de la note après envoi | Q10 dit que c'est possible jusqu'à clôture ; Q15 dit que la note est définitive dès l'envoi — les deux réponses se contredisent frontalement | La note reste modifiable jusqu'à la clôture de la session (RG9), conformément à Q10 | Q15 répond à une question de principe général ; Q10 répond à un cas opérationnel concret et anticipe explicitement l'erreur de saisie. En cas de conflit entre une règle de principe et une règle qui traite un cas réel identifié, je retiens la règle opérationnelle : elle correspond à un besoin vécu, pas à une intention abstraite. Le verrouillage à la clôture (Q15 partiellement respecté) reste le filet de sécurité final. |
| Moment de l'assignation du relecteur et absence de candidat éligible | Q7 précise *qui* choisit (le système, au hasard, parmi les présents) mais jamais *quand* l'assignation a lieu, ni ce qu'il se passe si aucun étudiant présent n'est éligible (session avec un seul présent, ou tous déjà relecteurs d'un autre exercice) — trou non couvert par les 16 questions | Assignation immédiate au dépôt de l'exercice, parmi les présents à cet instant hors auteur ; sans candidat, l'exercice reste `EN_ATTENTE_RELECTEUR`, visible comme tel dans le tableau (cohérent avec `relecturesEnAttente` du contrat et avec Q11) | Une assignation différée (en lot à la clôture) complexifierait sans bénéfice visible pour le client, et le champ `relecturesEnAttente` du contrat prouve que ce cas « sans relecteur » est déjà prévu comme un état normal et affichable, pas une erreur. |
| Authentification | Q1 : pas de mot de passe, l'étudiant se choisit dans une liste | On ne construit aucune notion de session HTTP sécurisée ; l'identité (`etudiantId`) est portée par le client à chaque appel, comme le montre le contrat | Cohérent avec Q1 et avec le fait que le contrat transmet systématiquement `etudiantId` en paramètre |
| Blocage après 5 échecs de code | Q4 : bloquer 2 minutes après 5 erreurs, sans préciser la granularité du blocage (par étudiant ? par IP ? par session ?) ni le comportement HTTP attendu pendant le blocage | Blocage par couple (session, étudiant identifié dans le formulaire) ; pendant les 2 minutes, toute nouvelle tentative est refusée avec `429 BLOCAGE_TENTATIVES`, sans même vérifier le code | Le client craint que les étudiants « devinent les codes entre eux » (Q4) : bloquer par étudiant identifié empêche un même étudiant de multiplier les essais, sans pénaliser toute la salle sur une IP partagée (réseau Wi-Fi commun) ; le 429 est le statut sémantique standard pour « trop de requêtes » et reste hors des codes imposés, donc libre |
| Dépôt d'exercice et présence | Aucune des 16 questions ne dit si le dépôt d'un exercice suppose d'être présent à la session — le sujet ne le précise pas non plus | Le dépôt exige une présence enregistrée sur la session (par code ou ajout formateur) ; sans présence, refus 400 avec code dédié `NON_PRESENT` | Q7 fait de la présence le critère d'éligibilité à la relecture ; autoriser le dépôt sans présence créerait un exercice hors du périmètre social du cours. Cohérent avec RG6/RG14 (relecteur choisi parmi les présents) et avec le fait qu'un exercice déposé par un absent n'aurait ni relecteur ni place dans le flux de la journée |
| Remplacement du lien (Q13) vs contrat imposé | Le contrat n'expose que `POST /api/exercices` avec `409 EXERCICE_DEJA_DEPOSE` dès qu'un exercice existe déjà, sans opération PUT/PATCH dédiée — alors que Q13 autorise explicitement le remplacement tant qu'aucune relecture n'a démarré, sans préciser ce que « démarré » signifie au niveau des statuts (`EN_RELECTURE` = relecteur assigné mais pas encore de note, ou `RELU` = note effectivement rendue ?) | `POST /api/exercices` fonctionne en *upsert* : le remplacement reste possible tant que le statut n'est pas `RELU`, y compris pendant que l'exercice est `EN_RELECTURE` (relecteur déjà assigné, note pas encore rendue) ; `409 EXERCICE_DEJA_DEPOSE` ne se déclenche qu'une fois la note effectivement rendue | Reste strictement dans les 5 opérations imposées par le contrat. Bloquer dès l'assignation (`EN_RELECTURE`) aurait rendu le remplacement quasi impossible en pratique, puisque l'assignation est immédiate (RG14) — ça aurait vidé Q13 de son sens. Bloquer seulement à `RELU` correspond au moment où un travail humain (la note) existe réellement et mérite d'être protégé d'un écrasement |
| Sensibilité à la casse du code de présence | Le contrat ne dit rien de la casse du code saisi ; aucune des 16 questions non plus | Le code généré est comparé **insensible à la casse** (normalisation en majuscules des deux côtés, l'égalité reste stricte après normalisation) | Réduire les échecs « bêtes » au moment critique de la prise de présence ; aucune conséquence de sécurité : le code est court-vécu (RG1) et le blocage RG3 limite les essais |

**Contradictions relevées :**

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| Q10 vs Q15 | Voir la première ligne du tableau des zones d'ombre ci-dessus — la note reste modifiable jusqu'à la clôture de la session (RG9), conformément à Q10 | Q15 répond à une intention de principe ; Q10 traite le cas opérationnel concret de la correction d'erreur, avec le verrou de clôture comme filet de sécurité |

## 8. Contraintes techniques

- Backend imposé : Java 17+, Spring Boot, Maven, wrapper `mvnw` commité
- Frontend : React (choix justifié en 4.), build qui passe
- Contrat `api/contrat.yaml` respecté à la lettre pour les 5 opérations imposées (chemins, verbes, codes de statut, format d'erreur `{code, message}`)
- Séparation stricte contrôleur / service / repository, DTO en frontière d'API, aucune entité JPA sérialisée directement
- Gestion centralisée des erreurs via `@RestControllerAdvice`, aucune stack trace exposée
- Schéma de base versionné par Flyway, `ddl-auto=update` interdit hors tests
- Deux tests obligatoires : un test unitaire de règle métier réelle, un test d'intégration d'endpoint, exécutables sur poste vierge
- Démarrage documenté et testé depuis un clone vierge (`docker compose up` ou 3 commandes max), avec données de démonstration chargées automatiquement

## 9. Livrables

- Dépôt GitHub public `kfokam48-epreuve-<matricule>` contenant `/docs`, `/api`, `/backend`, `/frontend`
- `docs/CAHIER_DES_CHARGES.md` (ce document), `docs/diagrammes/` (D1 à D4)
- Backlog complet en issues GitHub, priorisées
- `api/contrat.yaml` complété
- Application fonctionnelle : backend Spring Boot + frontend React, avec migrations Flyway et jeu de données de démonstration
- `CHANGELOG.md`, `JOURNAL.md`, `README.md` d'installation
- Second dépôt public `kfokam48-gitlab-<matricule>` pour l'épreuve Git
- `SOUMISSION.md` avec les deux liens et les deux hash de commit finaux

## 10. Démarche prévue

1. **Analyse** (ce document, diagrammes, backlog, contrat) → jalon `[JALON] analyse`, avant tout code.
2. **v0.1** : implémentation des stories Must uniquement, une branche + une PR par ticket, issues fermées par les commits → jalon `[JALON] v0.1`.
3. **Enveloppe** : ouverture, traitement du bug et du changement de besoin dans deux commits/PR séparés, mise à jour du schéma (migration Flyway supplémentaire), du contrat, des diagrammes et du cahier des charges devenus obsolètes.
4. **v1.0** : finalisation, `CHANGELOG.md`, README testé à froid, backlog restant trié → jalon `[JALON] v1.0`.
5. **Épreuve Git** sur le dépôt séparé fourni.
6. **Soumission** : commits finaux posés, hash relevés, `SOUMISSION.md` téléversé avant 18h00.

**Definition of Done** d'un ticket : code mergé sur `main` via PR liée à l'issue, migration Flyway si le schéma change, endpoint conforme au contrat (codes HTTP et format d'erreur inclus), au moins un test si la règle métier le justifie, aucune régression visible sur le tableau du formateur.

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | Début d'épreuve | Version initiale |
| 1.1 | Après relecture d'analyse | Hypothèses jusque-là implicites rédigées en section 7 : dépôt d'exercice subordonné à la présence (`400 NON_PRESENT`), code insensible à la casse, refus `429 BLOCAGE_TENTATIVES` pendant le blocage RG3 ; contradiction Q10/Q15 explicitée dans son propre tableau « Contradictions relevées » |
