# D3 — Séquence : « marquer sa présence »

> Cas nominal et cas d'erreur. **Les codes HTTP correspondent exactement au contrat** (`POST /api/presences` : 201 · 400 CODE_INCONNU · 409 DEJA_PRESENT · 410 CODE_EXPIRE · et l'extension 429 BLOCAGE_TENTATIVES, RG3).

```mermaid
sequenceDiagram
    actor E as Étudiant
    participant F as Front (écran Étudiant)
    participant PC as PresenceController
    participant PS as PresenceService
    participant DB as Repository / Base

    E->>F: sélectionne son nom (Q1) et saisit le code
    F->>F: validation locale (champ non vide)
    F->>PC: POST /api/presences { code, etudiantId }

    PC->>PS: marquerPresent(code, etudiantId)
    PS->>DB: charger (session, etudiant) + compteur RG3

    alt 6e tentative pendant un blocage RG3 (extension)
        DB-->>PS: bloque_jusqua > maintenant
        PS-->>PC: BlocageTentativesException
        PC-->>F: 429 { code: "BLOCAGE_TENTATIVES", message: "…" }
    else code inconnu
        DB-->>PS: aucune session pour ce code
        PS->>PS: incrémenter échecs (RG3) — 5e échec → bloque_jusqua = +2 min
        PS-->>PC: CodeInconnuException
        PC-->>F: 400 { code: "CODE_INCONNU" }
    else code expiré (RG1)
        DB-->>PS: session trouvée mais expirationAt dépassé
        PS-->>PC: CodeExpireException
        PC-->>F: 410 { code: "CODE_EXPIRE" }
    else session clôturée (RG2)
        DB-->>PS: clotureeAt non nul
        PS-->>PC: SessionClotureeException
        PC-->>F: 409 { code: "SESSION_CLOTUREE" }
    else déjà présent (RG15)
        DB-->>PS: présence existe déjà (session, étudiant)
        PS-->>PC: DejaPresentException
        PC-->>F: 409 { code: "DEJA_PRESENT" }
    else cas nominal
        DB-->>PS: tout est conforme
        PS->>DB: INSERT presence (source=ETUDIANT) + réinitialiser le compteur RG3
        DB-->>PS: présence enregistrée
        PS-->>PC: Presence
        PC-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
    end

    F-->>E: retour visuel : succès, ou message d'erreur du contrat
```

**Lecture :**
- Le contrôleur ne fait rien d'autre que déléguer et traduire l'exception métier en réponse HTTP (B3/B4) — la logique et les règles RG1/RG2/RG3/RG15 vivent dans le service ; les codes HTTP sont décidés dans le `@RestControllerAdvice` à partir du type d'exception.
- L'ordre des vérifications est important et testable : blocage RG3 d'abord (refus sans même regarder le code), puis existence du code (400), puis expiration (410), puis clôture (409), puis unicité (409). Un échec de code inconnu alimente le compteur RG3 ; les autres refus non plus.
- Ce diagramme correspond au flux détaillé du ticket US-02 et sera re-vérifié si l'enveloppe (étape 3) modifie le besoin.
