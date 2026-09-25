# D1 — Cas d'utilisation

> Acteurs et ce que chacun peut faire. Les cas « système » (assignation du relecteur, EF6) sont déclenchés par la postcondition du dépôt d'exercice, conformément à la note de modélisation du backlog. Les renvois EFx/RGx sont ceux du cahier des charges v1.1.

```mermaid
flowchart LR
    subgraph Systeme["Application KFOKAM48"]
        UC1(["Ouvrir une session de cours — EF1, RG1"])
        UC2(["Marquer sa présence avec un code — EF2, RG1–RG3, RG15"])
        UC3(["Ajouter une présence à la main — EF3, RG13, RG15"])
        UC4(["Déposer / remplacer le lien de son exercice — EF4, EF5, RG11, RG12"])
        UC5(["Assigner un relecteur — EF6, RG6, RG14 (système)"])
        UC6(["Rendre une relecture — EF7, RG4, RG5, RG8"])
        UC7(["Corriger sa relecture avant clôture — EF8, RG9"])
        UC8(["Consulter sa note et son commentaire — EF9, RG7"])
        UC9(["Consulter le tableau récapitulatif — EF10, RG10"])
        UC10(["Clôturer une session — EF11, RG2/RG9/RG11"])
    end

    Formateur(["👤 Formateur"])
    Etudiant(["👤 Étudiant"])
    Relecteur(["👤 Relecteur"])

    Formateur --> UC1
    Formateur --> UC3
    Formateur --> UC9
    Formateur --> UC10

    Etudiant --> UC2
    Etudiant --> UC4
    Etudiant --> UC8

    Relecteur --> UC6
    Relecteur --> UC7

    UC4 -. déclenche .-> UC5
    Relecteur -. est porté par .-> Etudiant
```

**Lecture :**
- Le **relecteur** n'est pas un compte distinct : c'est un étudiant dans un état (assigné sur un exercice précis, jamais le sien — RG4). D'où la relation pointillée « est porté par ».
- L'**assignation** (UC5) n'a pas d'acteur humain : elle est déclenchée automatiquement par la postcondition du dépôt (UC4 → UC5), décision documentée dans la note de modélisation du backlog.
- Le formateur n'accède pas directement à UC5/UC6/UC7 : le choix du relecteur appartient au système (Q7), la note appartient au relecteur.
