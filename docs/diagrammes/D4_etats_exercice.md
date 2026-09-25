# D4 (bonus +3) — États-transitions du cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> DEPOSE : POST /api/exercices (EF4)

    DEPOSE --> EN_ATTENTE_RELECTEUR : assignation immédiate sans candidat (RG6, RG14)
    DEPOSE --> EN_RELECTURE : assignation immédiate, relecteur trouvé (RG6, RG14)

    EN_ATTENTE_RELECTEUR --> EN_RELECTURE : un étudiant devient éligible et est assigné (rattrapage, hors contrat imposé)

    DEPOSE --> DEPOSE : remplacement du lien, upsert POST (RG12, EF5)
    EN_ATTENTE_RELECTEUR --> EN_ATTENTE_RELECTEUR : remplacement du lien, upsert POST (RG12, EF5)
    EN_RELECTURE --> EN_RELECTURE : remplacement du lien, upsert POST — autorisé même relecteur déjà assigné, tant que la note n'est pas rendue (RG12, EF5)

    EN_RELECTURE --> RELU : POST /api/relectures/{id} (EF7)
    RELU --> RELU : correction de la note tant que session non clôturée (RG9, EF8)

    RELU --> [*] : clôture de la session - note verrouillée définitivement, remplacement du lien définitivement impossible (409 EXERCICE_DEJA_DEPOSE)
    EN_ATTENTE_RELECTEUR --> [*] : clôture de la session - reste "en attente", visible dans relecturesEnAttente (RG10)
```

**Lecture :** le seuil de blocage du remplacement (RG12) est le passage à `RELU`, pas l'assignation du relecteur. Un exercice `EN_RELECTURE` reste donc remplaçable — l'assignation étant immédiate (RG14), bloquer dès `EN_RELECTURE` aurait rendu Q13 inapplicable en pratique. Le remplacement ne modifie ni `id` ni le statut de l'exercice ; si un relecteur était déjà assigné, il le reste et continue de voir le nouveau lien.
