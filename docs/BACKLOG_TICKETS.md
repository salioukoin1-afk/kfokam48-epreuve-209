# Backlog — User Stories KFOKAM48

Convention de référence : `US-01` à `US-08` (à recopier comme titre d'issue GitHub, avec le label de priorité). Chaque ticket renvoie explicitement aux EFx/RGx du `CAHIER_DES_CHARGES.md`.

**Note de modélisation :** l'assignation automatique du relecteur (EF6, RG6, RG14) n'a pas de template dédié dans la liste fournie car aucun acteur humain ne la déclenche directement — c'est le système qui agit en postcondition du dépôt d'exercice. Je l'ai donc intégrée comme **postcondition** du ticket US-04 plutôt que d'en faire un ticket séparé avec un template forcé qui ne correspondrait pas au cas (pas de formulaire, pas d'acteur humain). Dis-moi si tu préfères un ticket technique séparé.

---

## US-01 — [Création] Ouvrir une session de cours et obtenir un code de présence
**Priorité : Must** · Réf. `EF1`, `RG1`

**Acteur** : Formateur

**Objectif métier**
Le formateur a besoin d'ouvrir formellement une session pour permettre la prise de présence. Sans code généré à l'ouverture, aucune présence ne peut être enregistrée pour ce cours — c'est le point d'entrée de tout le flux de la journée.

**Description**
En tant que Formateur,
je veux créer une session de cours pour une promotion,
afin d'obtenir un code de présence que je communique en salle.

**Chemin d'accès**
Écran Formateur → Action « Ouvrir une session »

**Préconditions**
- Rôle : Formateur
- La promotion sélectionnée existe en base
- Aucune contrainte de session déjà ouverte (plusieurs sessions simultanées autorisées)

**Données d'entrée et contraintes de saisie**

| Champ | Obligatoire | Type | Contraintes | Message d'erreur attendu |
|---|---|---|---|---|
| `titre` | Oui | Texte | 3–100 caractères, non vide après trim | « Le titre de la session est requis » |
| `promotionId` | Oui | Entier | Doit référencer une promotion existante | « Promotion inconnue » |

**Règles transversales de validation**
- Validation des deux champs obligatoires avant tout appel serveur
- Suppression des espaces superflus en début/fin de `titre`

**Règles de gestion spécifiques**
- `RG1` : la session expire 15 minutes après son ouverture — durée paramétrable côté configuration, jamais codée en dur
- Le code généré est unique, non deviné facilement (alphanumérique, sans caractères ambigus type `0`/`O`, `1`/`I`)

**Scénario nominal**
1. Le formateur accède à l'écran Formateur et déclenche « Ouvrir une session »
2. Il renseigne le titre et sélectionne la promotion
3. Le système valide les champs obligatoires
4. Le système génère un code unique et calcule `ouvertureAt`/`expirationAt`
5. Le système enregistre la session (`POST /api/sessions`)
6. Le système confirme et affiche le code à l'écran, bien visible pour être communiqué en salle

**Scénarios alternatifs**
- **Champ manquant** : condition déclenchante — `titre` ou `promotionId` absent à la soumission ; contrôle — validation serveur avant tout enregistrement ; comportement — `400`, formulaire conservé, message d'erreur affiché sous le champ concerné ; impact données — aucune session créée.
- **Promotion inconnue** : condition déclenchante — `promotionId` ne correspond à aucune promotion existante ; comportement — `400`, message explicite ; impact données — aucune création.

**Postconditions**
- Une session est créée avec un code actif pendant la durée configurée
- Aucune présence n'existe encore pour cette session
- La session apparaît dans la liste des sessions du formateur

**Critères d'acceptation**
- Un `POST /api/sessions` valide retourne `201` avec `id`, `code`, `ouvertureAt`, `expirationAt`
- Un champ manquant retourne `400` au format d'erreur imposé
- Le code affiché est identique à celui persisté en base
- La durée avant expiration est lue depuis la configuration, pas depuis une valeur codée en dur (vérifiable en changeant la config sans redéployer le code)

**Definition of Done**
Endpoint conforme au contrat · test unitaire sur le calcul de `expirationAt` · test d'intégration sur `201`/`400` · écran formateur affichant le code · migration Flyway à jour si le schéma a changé.

---

## US-02 — [Création] Marquer sa présence avec un code
**Priorité : Must** · Réf. `EF2`, `RG1`, `RG2`, `RG3`, `RG15`

**Acteur** : Étudiant

**Objectif métier**
L'étudiant doit pouvoir prouver sa présence en quelques secondes, sans compte ni mot de passe (Q1), pour que la prise de présence ne ralentisse pas le démarrage du cours.

**Description**
En tant qu'Étudiant,
je veux saisir le code de présence communiqué par le formateur,
afin que ma présence soit enregistrée pour la session en cours.

**Chemin d'accès**
Écran Étudiant → Champ « Code de présence » → Action « Valider »

**Préconditions**
- L'étudiant est identifié par sélection dans une liste (pas de mot de passe, Q1)
- L'étudiant n'a pas dépassé son quota de tentatives sur cette session (RG3)

**Données d'entrée et contraintes de saisie**

| Champ | Obligatoire | Type | Contraintes | Message d'erreur attendu |
|---|---|---|---|---|
| `code` | Oui | Texte | Format identique à la génération (US-01), insensible à la casse | « Code invalide » |
| `etudiantId` | Oui (implicite) | Entier | Sélectionné dans la liste, pas saisi librement | — |

**Règles transversales de validation**
- Nettoyage des espaces superflus autour du code saisi
- Insensibilité à la casse sur le code (évite l'échec « bête » en majuscule/minuscule)

**Règles de gestion spécifiques**
- `RG1` : code expiré au-delà de la durée configurée → refus
- `RG2` : aucune présence après clôture de la session
- `RG3` : après 5 échecs consécutifs sur cette session, blocage de 2 minutes pour cet étudiant — pendant le blocage, toute tentative est refusée `429 BLOCAGE_TENTATIVES` (extension du contrat, tranchée en section 7 du cahier des charges)
- `RG15` : une seule présence par `(sessionId, etudiantId)`

**Scénario nominal**
1. L'étudiant sélectionne son nom dans la liste
2. Il saisit le code communiqué
3. Le système vérifie code, expiration, clôture et absence de doublon
4. Le système enregistre la présence avec `source=ETUDIANT`
5. Le système confirme la prise en compte à l'écran

**Scénarios alternatifs**
- **Code inconnu** : aucune session ne correspond → `400 CODE_INCONNU`, formulaire conservé, compteur d'échecs incrémenté.
- **Code expiré** : session trouvée mais `expirationAt` dépassé → `410 CODE_EXPIRE`, message explicite, aucune présence créée.
- **Déjà présent** : présence déjà enregistrée pour cet étudiant sur cette session → `409 DEJA_PRESENT`, l'étudiant est informé qu'il est déjà marqué présent.
- **Blocage après 5 échecs** : 6e tentative avant la fin des 2 minutes → `429 BLOCAGE_TENTATIVES`, refus immédiat sans même vérifier le code, message indiquant le temps restant.

**Postconditions**
- Une présence est créée, visible immédiatement dans le tableau du formateur
- Le compteur d'échecs est réinitialisé après un succès

**Critères d'acceptation**
- Cas nominal → `201` avec `source=ETUDIANT`
- Les 3 cas d'erreur du contrat renvoient exactement les codes HTTP et `code` d'erreur définis
- Le 6e échec en moins de 2 minutes est refusé `429 BLOCAGE_TENTATIVES` sans toucher la base de sessions

**Definition of Done**
Endpoint conforme au contrat (D3) · test unitaire sur RG1/RG3 · test d'intégration sur les 4 issues possibles · écran étudiant avec retour visuel de succès/échec.

---

## US-03 — [Création] Ajouter une présence manuellement
**Priorité : Should** · Réf. `EF3`, `RG13`, `RG15`, `RG2`

**Acteur** : Formateur

**Objectif métier**
Certains étudiants ont un incident technique (téléphone en panne, Q14) et ne peuvent pas saisir le code eux-mêmes. Le formateur doit pouvoir les marquer présents sans casser la fiabilité du suivi — d'où une distinction visuelle obligatoire.

**Description**
En tant que Formateur,
je veux ajouter manuellement la présence d'un étudiant à ma session,
afin de couvrir les cas où l'étudiant ne peut pas saisir le code lui-même.

**Chemin d'accès**
Écran Formateur → Tableau de session → Ligne étudiant → Action « Marquer présent »

**Préconditions**
- Rôle : Formateur
- Session ouverte, non clôturée (`RG2`)
- Étudiant appartenant à la promotion de la session

**Données d'entrée et contraintes de saisie**

| Champ | Obligatoire | Type | Contraintes | Message d'erreur attendu |
|---|---|---|---|---|
| `etudiantId` | Oui | Entier | Doit appartenir à la promotion de la session | « Étudiant hors promotion » |
| `sessionId` | Oui (implicite) | Entier | Session non clôturée | « Session clôturée » |

**Règles de gestion spécifiques**
- `RG13` : la présence créée porte `source=FORMATEUR`, affichée distinctement (ex. badge) dans le tableau
- `RG15` : refus si une présence existe déjà pour cet étudiant sur cette session
- `RG2` : refus si la session est clôturée

**Scénario nominal**
1. Le formateur ouvre le tableau de sa session
2. Il sélectionne l'étudiant absent et déclenche « Marquer présent »
3. Le système vérifie l'absence de doublon et l'état de la session
4. Le système enregistre la présence avec `source=FORMATEUR`
5. Le tableau se met à jour, la ligne affiche le badge « ajouté par le formateur »

**Scénarios alternatifs**
- **Déjà présent** : présence existante → `409`, message « déjà marqué présent », aucune duplication.
- **Session clôturée** : action refusée → message explicite, action grisée dans l'interface plutôt qu'un simple message d'erreur après coup.

**Postconditions**
- La présence apparaît dans le tableau, visuellement distincte d'une présence par code

**Critères d'acceptation**
- La présence créée par le formateur est indiscernable dans les données mais visuellement marquée à l'écran
- Impossible d'ajouter une présence sur une session clôturée (bouton désactivé + contrôle serveur)

**Definition of Done**
Service partagé avec US-02 pour la création de présence (même règle d'unicité) · test d'intégration sur le cas `source=FORMATEUR` · badge visible à l'écran.

---

## US-04 — [Création] Déposer ou remplacer le lien de son exercice
**Priorité : Must** · Réf. `EF4`, `EF5`, `EF6`, `RG6`, `RG11`, `RG12`, `RG14`

> **Décisions prises** : `POST /api/exercices` fonctionne en upsert. Le remplacement reste possible tant que le statut n'est pas `RELU` — y compris pendant que l'exercice est `EN_RELECTURE` (relecteur déjà assigné, note pas encore rendue). `409 EXERCICE_DEJA_DEPOSE` ne se déclenche qu'une fois la note effectivement rendue.
> **Priorité détaillée** : EF4 (premier dépôt) et EF6 (assignation) sont Must ; EF5 (remplacement) est Should — elle est portée par le même endpoint et donc livrée dans ce ticket, un dépôt sans remplacement rendant Q13 inapplicable (voir section 7 du cahier des charges).

**Acteur** : Étudiant

**Objectif métier**
L'étudiant doit pouvoir soumettre son travail même en dehors des horaires stricts du cours (Q12 : jusqu'à la clôture), et le système doit permettre à quelqu'un d'autre de le relire sans intervention manuelle du formateur.

**Description**
En tant qu'Étudiant,
je veux déposer le lien de mon exercice pour une session,
afin qu'il soit relu par un pair et noté.

**Chemin d'accès**
Écran Étudiant → Session → Action « Déposer mon exercice »

**Préconditions**
- L'étudiant est présent à la session (tranché en section 7 du cahier des charges : le dépôt exige une présence enregistrée — par code ou ajout formateur ; sans présence, refus `400 NON_PRESENT`, extension du contrat)
- Session non clôturée (`RG11`)

**Données d'entrée et contraintes de saisie**

| Champ | Obligatoire | Type | Contraintes | Message d'erreur attendu |
|---|---|---|---|---|
| `lien` | Oui | Texte (URI) | Format URL valide | « Lien invalide » |

**Règles de gestion spécifiques**
- `RG11` : dépôt possible jusqu'à la clôture, pas seulement jusqu'à la fin planifiée
- `RG6`/`RG14` : à la création de l'exercice, assignation immédiate d'un relecteur parmi les présents hors auteur ; sans candidat, statut `EN_ATTENTE_RELECTEUR`
- `RG12` : un nouvel appel `POST /api/exercices` sur `(sessionId, etudiantId)` remplace le lien existant (upsert) tant que le statut n'est pas `RELU` — y compris si un relecteur est déjà assigné (`EN_RELECTURE`) ; une fois la note rendue (`RELU`), `409 EXERCICE_DEJA_DEPOSE`

**Scénario nominal (premier dépôt)**
1. L'étudiant accède à l'écran de dépôt pour la session
2. Il colle le lien de son exercice
3. Le système valide le format du lien et vérifie qu'aucun exercice n'existe déjà pour `(sessionId, etudiantId)`
4. Le système enregistre l'exercice (`statut=DEPOSE`)
5. **Postcondition automatique (EF6)** : le système tente d'assigner un relecteur parmi les étudiants présents, hors auteur ; succès → `statut=EN_RELECTURE` ; échec → `statut=EN_ATTENTE_RELECTEUR`
6. Le système confirme le dépôt à l'étudiant (`201`)

**Scénario nominal (remplacement, upsert)**
1. L'étudiant revient sur l'écran de dépôt, un exercice existe déjà pour cette session, statut `DEPOSE`, `EN_ATTENTE_RELECTEUR` ou `EN_RELECTURE`
2. Il colle un nouveau lien et soumet
3. Le système retrouve l'exercice existant et vérifie que le statut n'est pas `RELU`
4. Le système remplace `lien`, conserve `id` et statut inchangés (si un relecteur est déjà assigné, il le reste — le remplacement ne relance pas d'assignation)
5. Le système confirme le remplacement (`201`, même contrat de réponse que la création)

**Scénarios alternatifs**
- **Lien invalide** : format non conforme → `400 LIEN_INVALIDE`, formulaire conservé, aucune écriture.
- **Étudiant absent** : condition déclenchante — aucune présence enregistrée pour (session, étudiant) au moment du dépôt ; contrôle — vérification avant toute écriture ; comportement — `400 NON_PRESENT`, aucune création ; impact données — aucun (décision section 7 du cahier des charges).
- **Note déjà rendue** : condition déclenchante — un `POST` arrive alors que le statut est `RELU` ; contrôle — vérification du statut avant tout remplacement ; comportement — `409 EXERCICE_DEJA_DEPOSE`, lien existant conservé tel quel, message informant que la relecture est terminée ; impact données — aucune modification, la note déjà rendue reste inchangée.

**Postconditions**
- Un seul exercice existe pour `(sessionId, etudiantId)` à tout instant (jamais de doublon créé par un remplacement)
- Le lien affiché est toujours le dernier soumis avant que la note ne soit rendue
- Un relecteur déjà assigné continue de voir et de noter le nouveau lien, sans réassignation

**Critères d'acceptation**
- Premier dépôt → `201` avec `id`, `statut`
- Lien invalide → `400 LIEN_INVALIDE`
- Remplacement en statut `DEPOSE`, `EN_ATTENTE_RELECTEUR` ou `EN_RELECTURE` → `201`, même `id`, `lien` mis à jour, pas de duplication en base, statut et relecteur assigné inchangés
- Remplacement en statut `RELU` → `409 EXERCICE_DEJA_DEPOSE`, lien non modifié
- L'assignation du relecteur ne désigne jamais l'auteur (`RG6`), vérifiable par test unitaire dédié
- Un remplacement réussi ne redéclenche jamais une nouvelle assignation

**Definition of Done**
Endpoint conforme au contrat, upsert testé sur les 3 statuts remplaçables + rejet sur `RELU` · test unitaire sur RG6 (jamais l'auteur) · test d'intégration couvrant création, remplacement (3 statuts), lien invalide, rejet après `RELU` · écran étudiant affichant l'état actuel (dépôt initial vs modification) sans dupliquer le formulaire.

---

## US-05 — [Modification] Noter et commenter un exercice assigné (avec correction possible)
**Priorité : Must** · Réf. `EF7`, `EF8`, `RG4`, `RG5`, `RG8`, `RG9`

**Acteur** : Relecteur (étudiant assigné)

**Objectif métier**
La relecture par les pairs n'a de valeur que si elle est réellement rendue et fiable. Le relecteur doit pouvoir se corriger avant que la session ne soit clôturée, pour que l'exigence de qualité (Q10) prime sur la peur de l'erreur définitive.

**Description**
En tant que Relecteur,
je veux modifier la note et le commentaire d'un exercice qui m'est assigné,
afin de rendre mon évaluation, et de la corriger si besoin avant la clôture de la session.

**Chemin d'accès**
Écran Relecteur → Liste « Exercices à relire » → Exercice → Formulaire de notation

**Préconditions**
- Rôle : Relecteur, désigné pour cet exercice précis (`RG5`)
- Le relecteur n'est pas l'auteur de l'exercice (`RG4`)
- La session n'est pas clôturée (`RG9`) — vaut pour le rendu initial (POST) comme pour la correction (PATCH)

**Données du formulaire de modification**

| Champ | Modifiable ? | Obligatoire | Type fonctionnel | Contraintes | Message d'erreur |
|---|---|---|---|---|---|
| `note` | Oui, jusqu'à clôture | Oui | Entier | 0 ≤ note ≤ 20, valeur entière | « Note invalide, doit être un entier entre 0 et 20 » |
| `commentaire` | Oui, jusqu'à clôture | Oui | Texte | Non vide après trim | « Le commentaire est requis » |

**Règles de gestion spécifiques**
- `RG4` : refus systématique si `relecteurId == exercice.etudiantId`
- `RG5` : un seul relecteur par exercice — pas de réassignation via ce formulaire
- `RG8` : note entière comprise entre 0 et 20, **bornes incluses** (0 et 20 sont des notes valides)
- `RG9` : modification possible tant que `Session.clotureeAt` est nul ; verrouillée définitivement après

**Scénario nominal**
1. Le relecteur ouvre l'exercice qui lui est assigné
2. Il saisit la note et le commentaire
3. Le système valide la note (entier 0–20) et le commentaire (non vide)
4. Le système enregistre la relecture (`rendueAt` mis à jour). Le rendu initial passe par `POST /api/relectures/{id}` (erreur imposée `409 RELECTURE_DEJA_RENDUE` si déjà rendue) ; **la correction passe par `PATCH /api/relectures/{id}`** (extension du contrat, EF8/RG9) — le POST reste strictement une création, ce qui garde l'erreur imposée du contrat atteignable
5. Le système confirme, l'exercice passe/reste au statut `RELU`

**Scénarios alternatifs**
- **Auto-relecture** : condition déclenchante — le relecteur assigné tente de noter son propre exercice (ne devrait jamais arriver via l'assignation automatique, mais contrôlé quand même en défense) ; contrôle — vérification `relecteurId ≠ auteurId` ; comportement — `403 AUTO_RELECTURE`, aucune persistance ; impact données — aucun.
- **Relecture déjà rendue (double POST)** : condition déclenchante — un second `POST` sur une relecture déjà enregistrée ; contrôle — vérification `rendueAt IS NULL` ; comportement — `409 RELECTURE_DEJA_RENDUE` (erreur imposée par le contrat), aucune modification ; impact données — aucun. La correction légitime passe par `PATCH` (EF8).
- **Note hors bornes ou non entière** : détectée à la soumission ; comportement — `400 NOTE_INVALIDE`, formulaire conservé, valeur saisie non perdue ; impact données — aucune persistance.
- **Session clôturée, tentative de rendu ou de correction** : détectée au moment de la soumission (pas seulement à l'affichage, pour couvrir le cas d'une clôture pendant la saisie) ; comportement — `409 SESSION_CLOTUREE` (code dédié, ajouté au contrat en extension), formulaire passé en lecture seule ; impact données — aucune modification, dernière valeur valide conservée.

**Postconditions**
- La relecture porte la dernière note/commentaire valides envoyés avant clôture
- La moyenne de l'étudiant relu, visible dans le tableau formateur, reflète immédiatement la mise à jour

**Critères d'acceptation**
- Une note hors 0–20 ou non entière est rejetée avec `400 NOTE_INVALIDE`
- Une tentative d'auto-relecture est rejetée avec `403 AUTO_RELECTURE`
- Une modification après clôture de session est rejetée, la dernière valeur valide reste en base
- La moyenne affichée au formateur change dès l'enregistrement d'une relecture valide

**Definition of Done**
Endpoint conforme au contrat · test unitaire sur RG4/RG8/RG9 · test d'intégration couvrant `400 NOTE_INVALIDE`, `403 AUTO_RELECTURE`, `409 RELECTURE_DEJA_RENDUE` (POST) et `409 SESSION_CLOTUREE` (PATCH après clôture) · écran relecteur avec formulaire réutilisable pour saisie initiale (POST) et correction (PATCH).

---

## US-06 — [Consultation d'un élément] Consulter la note et le commentaire de son exercice
**Priorité : Must** · Réf. `EF9`, `RG7`

**Acteur** : Étudiant (auteur de l'exercice)

**Objectif métier**
L'étudiant doit pouvoir suivre l'évaluation de son travail sans jamais connaître l'identité de son relecteur (Q8), pour préserver l'anonymat qui rend la relecture possible entre pairs.

**Description**
En tant qu'Étudiant,
je veux consulter la note et le commentaire reçus sur mon exercice,
afin de savoir où j'en suis, sans connaître l'identité de mon relecteur.

**Chemin d'accès**
Écran Étudiant → Mes exercices → Exercice → Action « Voir ma note »

**Préconditions**
- L'exercice appartient à l'étudiant identifié (sélection dans la liste, Q1 — pas de notion de « connexion »)
- L'exercice existe et n'a pas été supprimé

**Structure de l'affichage**
- Statut de l'exercice (`DEPOSE` / `EN_ATTENTE_RELECTEUR` / `EN_RELECTURE` / `RELU`)
- Si `RELU` : note (badge numérique /20) et commentaire (texte, avec gestion du texte long par troncature + affichage complet au clic)
- Aucun champ, à aucun moment, ne référence le relecteur (ID, nom)

**Règles de gestion spécifiques**
- `RG7` : l'identité du relecteur n'est jamais transmise au frontend pour cet écran — contrainte à respecter dès la conception du DTO de sortie, pas seulement côté affichage

**Scénario nominal**
1. L'étudiant accède à la liste de ses exercices
2. Il sélectionne un exercice au statut `RELU`
3. Le système affiche note et commentaire

**Scénarios alternatifs**
- **Exercice pas encore relu** : condition — statut ≠ `RELU` ; comportement — affichage d'un état « en attente de relecture », aucune note affichée ; impact données — aucun.

**Postconditions**
- Aucune donnée n'est modifiée par la consultation
- L'identité du relecteur reste non exposée dans la réponse API elle-même (pas seulement masquée à l'écran)

**Critères d'acceptation**
- Le DTO de réponse ne contient à aucun moment un champ `relecteurId` ou `relecteurNom`
- L'affichage distingue clairement « en attente » de « relu avec note X/20 »

**Definition of Done**
DTO de sortie vérifié sans fuite d'identité (test dédié) · écran étudiant avec état vide géré.

---

## US-07 — [Consultation de la liste des éléments] Consulter le tableau récapitulatif par étudiant
**Priorité : Must** · Réf. `EF10`, `RG10`

**Acteur** : Formateur

**Objectif métier**
Le formateur a besoin d'une vue unique, fiable, pour savoir qui est présent, qui a rendu son travail, et qui doit encore relire — sans recalcul manuel ni recoupement de plusieurs écrans.

**Description**
En tant que Formateur,
je veux consulter la liste récapitulative des étudiants de ma promotion,
afin de visualiser présence, dépôts, moyenne et relectures dues.

**Chemin d'accès**
Écran Formateur → Sélection de la promotion → Tableau récapitulatif

**Préconditions**
- Rôle : Formateur
- La promotion existe (`404 PROMOTION_INCONNUE` sinon)

**Structure de l'affichage**

| Colonne | Donnée | Représentation |
|---|---|---|
| Étudiant | `nom` | Texte |
| Présences | `presences` | Nombre |
| Exercices déposés | `exercicesDeposes` | Nombre |
| Moyenne | `moyenne` | Nombre /20, ou « — » si `null` (aucune note reçue) |
| Relectures en attente | `relecturesEnAttente` | Badge si > 0 |

Aucune pagination exigée (volumétrie NF1, promotion de l'ordre de 30 étudiants).

**Règles de gestion spécifiques**
- `RG10` : un exercice sans relecture rendue compte dans `relecturesEnAttente`, jamais ignoré silencieusement
- La moyenne affichée est **exactement** celle renvoyée par l'API, jamais recalculée côté frontend (contrainte F3)

**Scénario nominal**
1. Le formateur sélectionne sa promotion
2. Le système appelle `GET /api/tableau?promotionId=`
3. Le système affiche une ligne par étudiant avec les 5 colonnes

**Scénarios alternatifs**
- **Promotion inconnue** : `404 PROMOTION_INCONNUE` → message explicite, tableau non affiché.
- **Aucune donnée (promotion vide)** : liste vide retournée → message « Aucun étudiant dans cette promotion », pas une erreur.
- **Erreur réseau/chargement** : état de chargement affiché pendant l'appel, message d'erreur explicite et bouton « Réessayer » en cas d'échec.

**Postconditions**
- Aucune donnée modifiée par la consultation
- Le tableau reflète l'état réel à l'instant de l'appel (pas de cache trompeur)

**Critères d'acceptation**
- Chaque colonne du tableau correspond exactement à un champ du contrat, sans transformation métier côté frontend
- Une moyenne nulle s'affiche distinctement d'une moyenne de 0/20
- L'état de chargement et l'état d'erreur sont visuellement distincts

**Definition of Done**
Endpoint conforme au contrat (calcul serveur de la moyenne et de `relecturesEnAttente`) · test d'intégration sur promotion inconnue et promotion vide · écran formateur avec états de chargement/erreur gérés.

---

## US-08 — [Modification] Clôturer une session
**Priorité : Should** · Réf. `EF11`, `RG2`, `RG9`, `RG10`, `RG11`

**Acteur** : Formateur

**Objectif métier**
La clôture est le point de verrouillage qui rend les notes définitives (Q15) et arrête les dépôts tardifs — sans elle, aucune des règles de finalisation (RG9, RG11) n'a de déclencheur concret.

**Description**
En tant que Formateur,
je veux clôturer une session de cours,
afin de verrouiller définitivement les présences, dépôts et notes associés.

**Chemin d'accès**
Écran Formateur → Session → Action « Clôturer la session »

**Préconditions**
- Rôle : Formateur
- La session n'est pas déjà clôturée

**Données du formulaire de modification**

| Champ | Modifiable ? | Obligatoire | Type fonctionnel | Contraintes | Message d'erreur |
|---|---|---|---|---|---|
| `clotureeAt` | Écriture unique (non modifiable ensuite) | — (généré serveur) | Date/heure | Doit être postérieure à `ouvertureAt` | « Session déjà clôturée » |

**Règles de gestion spécifiques**
- `RG2` : après clôture, plus aucune présence acceptée (déjà couvert par US-02/US-03, revérifié ici comme conséquence)
- `RG9` : après clôture, plus aucune modification de note (revérifié comme conséquence dans US-05)
- `RG11` : après clôture, plus aucun dépôt d'exercice accepté

**Scénario nominal**
1. Le formateur ouvre sa session
2. Il déclenche « Clôturer la session », une confirmation est demandée (action irréversible)
3. Le système enregistre `clotureeAt = maintenant`
4. Le système confirme la clôture, les actions de dépôt/notation deviennent indisponibles pour cette session

**Scénarios alternatifs**
- **Session déjà clôturée** : condition déclenchante — nouvelle tentative de clôture sur une session déjà clôturée ; contrôle — vérification `clotureeAt IS NULL` avant écriture ; comportement — refus, message « Session déjà clôturée », bouton déjà désactivé côté écran ; impact données — aucune modification, `clotureeAt` inchangé (pas d'écrasement par une date plus tardive).

**Postconditions**
- La session est marquée clôturée de façon irréversible
- Toutes les relectures en cours restent au dernier état valide, désormais figées (`RG9`)
- Les exercices sans relecteur/relecture restent visibles comme « en attente » (`RG10`), la clôture ne les fait pas disparaître

**Critères d'acceptation**
- Une session déjà clôturée ne peut pas être re-clôturée (idempotence sans écrasement de date)
- Après clôture, toute tentative de présence/dépôt/notation sur cette session est refusée par le backend, pas seulement masquée côté écran
- L'action est confirmée avant exécution (irréversible)

**Definition of Done**
Endpoint dédié (hors des 5 imposés, à ajouter dans `api/contrat.yaml`) · test d'intégration vérifiant le refus des 3 actions après clôture · confirmation utilisateur avant l'action côté frontend.

---

## Ticket technique complémentaire (hors template — pas un user story client)

**US-09 — [Technique] Rendre paramétrable la durée d'expiration du code de présence**
Priorité : Should · Réf. section 9 du README

Extraire la durée de 15 minutes (`RG1`) dans une propriété de configuration (`presence.expiration-minutes`), surchargeable par variable d'environnement Docker, lue par le service au moment du calcul de `expirationAt`. Critère d'acceptation : changer la valeur de configuration modifie le comportement sans recompiler le code (vérifié par un test avec une configuration de test différente de la valeur par défaut).
