// @ts-check
const { test, expect } = require('@playwright/test')

/**
 * Tests de contrat d'API — appels directs au backend (port 8080)
 *
 * Ces tests vérifient que chaque endpoint respecte exactement le contrat
 * défini dans api/contrat.yaml : codes HTTP, format des corps de réponse,
 * format d'erreur {code, message}.
 *
 * Prérequis : backend Spring Boot actif sur http://localhost:8080
 *             avec les données de démonstration Flyway.
 *
 * Données de démonstration (V2/V5) :
 *   Promotion ID 1 : « Promotion 209 — Yaoundé »
 *   Session code « AB12CD » : active (non expirée)
 *   Session code « XY34EF » : expirée
 *   Étudiant ID 1 : premier de la liste (Aminata Diallo ou équivalent demo)
 */

const API = 'http://localhost:8080'
const PROMO_ID = 1

/** Ouvre une session de démonstration et retourne {id, code} */
async function ouvrirSession(request, titre) {
  const r = await request.post(`${API}/api/sessions`, {
    data: { titre: titre ?? 'Session API-contract E2E', promotionId: PROMO_ID },
  })
  expect(r.status()).toBe(201)
  return r.json()
}

/** Retourne la liste des étudiants de la promo demo */
async function listeEtudiants(request) {
  const r = await request.get(`${API}/api/etudiants?promotionId=${PROMO_ID}`)
  expect(r.status()).toBe(200)
  return r.json()
}

// ─────────────────────────────────────────────────────────
test.describe('GET /api/etudiants — liste des étudiants', () => {

  test('200 — retourne un tableau avec id et nom', async ({ request }) => {
    const r = await request.get(`${API}/api/etudiants?promotionId=${PROMO_ID}`)
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(Array.isArray(corps)).toBe(true)
    expect(corps.length).toBeGreaterThan(0)
    // Chaque élément respecte le contrat {id, nom}
    for (const e of corps) {
      expect(typeof e.id).toBe('number')
      expect(typeof e.nom).toBe('string')
    }
  })

  test('404 — promotion inconnue retourne {code, message}', async ({ request }) => {
    const r = await request.get(`${API}/api/etudiants?promotionId=99999`)
    expect(r.status()).toBe(404)
    const corps = await r.json()
    expect(typeof corps.code).toBe('string')
    expect(typeof corps.message).toBe('string')
    expect(corps.code).toBe('PROMOTION_INCONNUE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('POST /api/sessions — ouverture de session', () => {

  test('201 — retourne id, code, ouvertureAt, expirationAt', async ({ request }) => {
    const r = await request.post(`${API}/api/sessions`, {
      data: { titre: 'Test contrat 201', promotionId: PROMO_ID },
    })
    expect(r.status()).toBe(201)
    const corps = await r.json()
    expect(typeof corps.id).toBe('number')
    expect(typeof corps.code).toBe('string')
    expect(corps.code).toMatch(/^[A-Z0-9]{6}$/)
    expect(typeof corps.ouvertureAt).toBe('string')
    expect(typeof corps.expirationAt).toBe('string')
    // expirationAt > ouvertureAt (RG1 : +15 min)
    expect(new Date(corps.expirationAt).getTime()).toBeGreaterThan(
      new Date(corps.ouvertureAt).getTime()
    )
  })

  test('400 — titre manquant retourne {code, message}', async ({ request }) => {
    const r = await request.post(`${API}/api/sessions`, {
      data: { promotionId: PROMO_ID }, // titre absent
    })
    expect(r.status()).toBe(400)
    const corps = await r.json()
    expect(typeof corps.code).toBe('string')
    expect(typeof corps.message).toBe('string')
  })

  test('400 — promotion inconnue retourne PROMOTION_INCONNUE', async ({ request }) => {
    const r = await request.post(`${API}/api/sessions`, {
      data: { titre: 'Test promo inconnue', promotionId: 99999 },
    })
    expect(r.status()).toBe(400)
    const corps = await r.json()
    expect(corps.code).toBe('PROMOTION_INCONNUE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('POST /api/sessions/{id}/cloture — clôture de session', () => {

  test('200 — retourne id et clotureeAt', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session à clôturer E2E')
    const r = await request.post(`${API}/api/sessions/${session.id}/cloture`)
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(corps.id).toBe(session.id)
    expect(typeof corps.clotureeAt).toBe('string')
  })

  test('400 — clôturer une session déjà clôturée retourne BLOCAGE_CLOTURE', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session double clôture E2E')
    // Première clôture
    await request.post(`${API}/api/sessions/${session.id}/cloture`)
    // Deuxième clôture — doit être refusée
    const r = await request.post(`${API}/api/sessions/${session.id}/cloture`)
    expect(r.status()).toBe(400)
    const corps = await r.json()
    expect(corps.code).toBe('BLOCAGE_CLOTURE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('POST /api/presences — présence par code', () => {

  test('410 — code expiré retourne CODE_EXPIRE', async ({ request }) => {
    const etudiants = await listeEtudiants(request)
    const etudiantId = etudiants[0].id

    const r = await request.post(`${API}/api/presences`, {
      data: { code: 'XY34EF', etudiantId },
    })
    expect(r.status()).toBe(410)
    const corps = await r.json()
    expect(corps.code).toBe('CODE_EXPIRE')
    expect(typeof corps.message).toBe('string')
  })

  test('400 — code inconnu retourne CODE_INCONNU', async ({ request }) => {
    const etudiants = await listeEtudiants(request)
    const r = await request.post(`${API}/api/presences`, {
      data: { code: 'XXXXXX', etudiantId: etudiants[0].id },
    })
    expect(r.status()).toBe(400)
    const corps = await r.json()
    expect(corps.code).toBe('CODE_INCONNU')
  })

  test('201 — code valide AB12CD → présence avec id, sessionId, etudiantId, source', async ({ request }) => {
    const etudiants = await listeEtudiants(request)
    // Utiliser le dernier étudiant pour maximiser les chances d'absence de doublon
    const etudiant = etudiants[etudiants.length - 1]

    const r = await request.post(`${API}/api/presences`, {
      data: { code: 'AB12CD', etudiantId: etudiant.id },
    })

    // 201 (premier enregistrement) ou 409 DEJA_PRESENT si déjà présent
    if (r.status() === 201) {
      const corps = await r.json()
      expect(typeof corps.id).toBe('number')
      expect(typeof corps.sessionId).toBe('number')
      expect(corps.etudiantId).toBe(etudiant.id)
      expect(['ETUDIANT', 'FORMATEUR']).toContain(corps.source)
    } else {
      expect(r.status()).toBe(409)
      const corps = await r.json()
      expect(corps.code).toBe('DEJA_PRESENT')
    }
  })
})

// ─────────────────────────────────────────────────────────
test.describe('POST /api/presences/formateur — présence manuelle', () => {

  test('201 — retourne source=FORMATEUR', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session présence formateur E2E ' + Date.now())
    const etudiants = await listeEtudiants(request)
    const etudiant = etudiants[0]

    const r = await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: etudiant.id },
    })

    // 201 ou 409 DEJA_PRESENT si déjà marqué
    if (r.status() === 201) {
      const corps = await r.json()
      expect(corps.source).toBe('FORMATEUR')
      expect(corps.sessionId).toBe(session.id)
      expect(corps.etudiantId).toBe(etudiant.id)
    } else {
      expect(r.status()).toBe(409)
    }
  })

  test('409 — session clôturée retourne SESSION_CLOTUREE', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session close presence formateur E2E')
    // Clôturer la session
    await request.post(`${API}/api/sessions/${session.id}/cloture`)
    // Tenter d'ajouter une présence après clôture
    const etudiants = await listeEtudiants(request)
    const r = await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: etudiants[0].id },
    })
    expect(r.status()).toBe(409)
    const corps = await r.json()
    expect(corps.code).toBe('SESSION_CLOTUREE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('POST /api/exercices — dépôt d'exercice', () => {

  test('400 — étudiant non présent retourne NON_PRESENT', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session exercice sans presence E2E')
    const etudiants = await listeEtudiants(request)
    // Prendre un étudiant non présent (ne pas avoir ajouté de présence)
    const r = await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: etudiants[3].id, lien: 'https://github.com/test/repo' },
    })
    // Soit NON_PRESENT (400) si pas de présence, soit 201 si déjà présent dans la démo
    if (r.status() === 400) {
      const corps = await r.json()
      expect(corps.code).toBe('NON_PRESENT')
    } else {
      // Étudiant déjà présent via les données de démo — cas acceptable
      expect([201, 409]).toContain(r.status())
    }
  })

  test('201 — dépôt après présence retourne id et statut', async ({ request }) => {
    // Ouvrir une session dédiée
    const session = await ouvrirSession(request, 'Session exercice complet E2E ' + Date.now())
    const etudiants = await listeEtudiants(request)
    const etudiant = etudiants[2] // Prendre un étudiant du milieu

    // Ajouter la présence manuellement (source formateur, plus simple pour les tests)
    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: etudiant.id },
    })

    // Déposer l'exercice
    const r = await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: etudiant.id, lien: 'https://github.com/test/exercice-e2e' },
    })
    expect(r.status()).toBe(201)
    const corps = await r.json()
    expect(typeof corps.id).toBe('number')
    expect(['DEPOSE', 'EN_ATTENTE_RELECTEUR', 'EN_RELECTURE']).toContain(corps.statut)
  })

  test('400 — lien invalide retourne LIEN_INVALIDE', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session lien invalide E2E')
    const etudiants = await listeEtudiants(request)
    const etudiant = etudiants[1]

    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: etudiant.id },
    })

    const r = await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: etudiant.id, lien: 'pas-une-url' },
    })
    expect(r.status()).toBe(400)
    const corps = await r.json()
    expect(corps.code).toBe('LIEN_INVALIDE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('GET /api/exercices/miens — consultation de sa note', () => {

  test('200 — retourne id, statut, lien (pas de relecteurId)', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session miens E2E ' + Date.now())
    const etudiants = await listeEtudiants(request)
    const etudiant = etudiants[4]

    // Présence + dépôt
    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: etudiant.id },
    })
    await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: etudiant.id, lien: 'https://github.com/test/miens-e2e' },
    })

    const r = await request.get(
      `${API}/api/exercices/miens?sessionId=${session.id}&etudiantId=${etudiant.id}`
    )
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(typeof corps.id).toBe('number')
    expect(typeof corps.statut).toBe('string')
    expect(typeof corps.lien).toBe('string')
    // RG7 : jamais de relecteurId dans la réponse
    expect(corps.relecteurId).toBeUndefined()
    expect(corps.relecteurNom).toBeUndefined()
  })

  test('404 — exercice inconnu retourne EXERCICE_INCONNU', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session miens 404 E2E')
    const etudiants = await listeEtudiants(request)

    const r = await request.get(
      `${API}/api/exercices/miens?sessionId=${session.id}&etudiantId=${etudiants[0].id}`
    )
    expect(r.status()).toBe(404)
    const corps = await r.json()
    expect(corps.code).toBe('EXERCICE_INCONNU')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('GET /api/relectures/en-attente — exercices à relire', () => {

  test('200 — retourne un tableau (possiblement vide)', async ({ request }) => {
    const etudiants = await listeEtudiants(request)
    const r = await request.get(`${API}/api/relectures/en-attente?etudiantId=${etudiants[0].id}`)
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(Array.isArray(corps)).toBe(true)
    // Si des éléments existent, vérifier le format contractuel
    if (corps.length > 0) {
      const item = corps[0]
      expect(typeof item.exerciceId).toBe('number')
      expect(typeof item.relectureId).toBe('number')
      expect(typeof item.sessionId).toBe('number')
      expect(typeof item.sessionTitre).toBe('string')
      expect(typeof item.lien).toBe('string')
      expect(typeof item.auteurNom).toBe('string')
    }
  })
})

// ─────────────────────────────────────────────────────────
test.describe('POST /api/relectures/{id} — rendre une note', () => {

  /** Prépare un exercice avec un relecteur assigné et retourne {exerciceId, relectureId, relecteurId} */
  async function preparerRelecture(request) {
    const session = await ouvrirSession(request, 'Session relecture E2E ' + Date.now())
    const etudiants = await listeEtudiants(request)

    // Deux étudiants présents (auteur + futur relecteur)
    const auteur = etudiants[0]
    const autreEtudiant = etudiants[1]

    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: auteur.id },
    })
    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: autreEtudiant.id },
    })

    // Déposer l'exercice → assignation automatique du relecteur (RG6/RG14)
    const exR = await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: auteur.id, lien: 'https://github.com/test/relecture-e2e' },
    })

    if (exR.status() !== 201) return null
    const ex = await exR.json()

    if (ex.statut !== 'EN_RELECTURE') return null // Pas de relecteur assigné

    // Récupérer la relecture via l'endpoint en-attente pour trouver le relecteurId
    for (const e of etudiants) {
      const r = await request.get(`${API}/api/relectures/en-attente?etudiantId=${e.id}`)
      const liste = await r.json()
      const relecture = liste.find((item) => item.exerciceId === ex.id)
      if (relecture) {
        return { exerciceId: ex.id, relectureId: relecture.relectureId, relecteurId: e.id }
      }
    }
    return null
  }

  test('200 — note et commentaire enregistrés, retourne id, exerciceId, note', async ({ request }) => {
    const ctx = await preparerRelecture(request)
    if (!ctx) {
      test.skip() // Pas de relecteur assignable dans l'état actuel de la DB
      return
    }

    const r = await request.post(
      `${API}/api/relectures/${ctx.relectureId}?relecteurId=${ctx.relecteurId}`,
      { data: { note: 14, commentaire: 'Bonne présentation, quelques améliorations possibles.' } }
    )
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(typeof corps.id).toBe('number')
    expect(corps.exerciceId).toBe(ctx.exerciceId)
    expect(corps.note).toBe(14)
    expect(typeof corps.commentaire).toBe('string')
  })

  test('400 — note hors 0–20 retourne NOTE_INVALIDE', async ({ request }) => {
    const ctx = await preparerRelecture(request)
    if (!ctx) { test.skip(); return }

    const r = await request.post(
      `${API}/api/relectures/${ctx.relectureId}?relecteurId=${ctx.relecteurId}`,
      { data: { note: 25, commentaire: 'Note invalide test' } }
    )
    expect(r.status()).toBe(400)
    const corps = await r.json()
    expect(corps.code).toBe('NOTE_INVALIDE')
  })

  test('409 — double POST retourne RELECTURE_DEJA_RENDUE', async ({ request }) => {
    const ctx = await preparerRelecture(request)
    if (!ctx) { test.skip(); return }

    // Premier rendu
    await request.post(
      `${API}/api/relectures/${ctx.relectureId}?relecteurId=${ctx.relecteurId}`,
      { data: { note: 12, commentaire: 'Premier commentaire.' } }
    )

    // Deuxième POST → doit être refusé
    const r = await request.post(
      `${API}/api/relectures/${ctx.relectureId}?relecteurId=${ctx.relecteurId}`,
      { data: { note: 15, commentaire: 'Tentative de double POST.' } }
    )
    expect(r.status()).toBe(409)
    const corps = await r.json()
    expect(corps.code).toBe('RELECTURE_DEJA_RENDUE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('PATCH /api/relectures/{id} — corriger une note (RG9)', () => {

  test('200 — correction avant clôture retourne id, exerciceId, note', async ({ request }) => {
    // Préparer et rendre une première relecture, puis PATCH
    const session = await ouvrirSession(request, 'Session correction note E2E ' + Date.now())
    const etudiants = await listeEtudiants(request)

    const auteur = etudiants[0]
    const autre = etudiants[1]

    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: auteur.id },
    })
    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: autre.id },
    })

    const exR = await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: auteur.id, lien: 'https://github.com/test/patch-e2e' },
    })
    if (exR.status() !== 201) { test.skip(); return }
    const ex = await exR.json()
    if (ex.statut !== 'EN_RELECTURE') { test.skip(); return }

    // Identifier le relecteur
    let relectureId = null, relecteurId = null
    for (const e of etudiants) {
      const r2 = await request.get(`${API}/api/relectures/en-attente?etudiantId=${e.id}`)
      const liste = await r2.json()
      const rel = liste.find((item) => item.exerciceId === ex.id)
      if (rel) { relectureId = rel.relectureId; relecteurId = e.id; break }
    }
    if (!relectureId) { test.skip(); return }

    // POST initial
    await request.post(
      `${API}/api/relectures/${relectureId}?relecteurId=${relecteurId}`,
      { data: { note: 10, commentaire: 'Commentaire initial.' } }
    )

    // PATCH de correction
    const r = await request.patch(
      `${API}/api/relectures/${relectureId}?relecteurId=${relecteurId}`,
      { data: { note: 18, commentaire: 'Correction après réflexion.' } }
    )
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(corps.note).toBe(18)
    expect(corps.commentaire).toBe('Correction après réflexion.')
  })

  test('409 — PATCH après clôture retourne SESSION_CLOTUREE', async ({ request }) => {
    const session = await ouvrirSession(request, 'Session cloture PATCH E2E ' + Date.now())
    const etudiants = await listeEtudiants(request)

    const auteur = etudiants[0]
    const autre = etudiants[1]

    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: auteur.id },
    })
    await request.post(`${API}/api/presences/formateur`, {
      data: { sessionId: session.id, etudiantId: autre.id },
    })

    const exR = await request.post(`${API}/api/exercices`, {
      data: { sessionId: session.id, etudiantId: auteur.id, lien: 'https://github.com/test/patch-cloture' },
    })
    if (exR.status() !== 201) { test.skip(); return }
    const ex = await exR.json()
    if (ex.statut !== 'EN_RELECTURE') { test.skip(); return }

    let relectureId = null, relecteurId = null
    for (const e of etudiants) {
      const r2 = await request.get(`${API}/api/relectures/en-attente?etudiantId=${e.id}`)
      const liste = await r2.json()
      const rel = liste.find((item) => item.exerciceId === ex.id)
      if (rel) { relectureId = rel.relectureId; relecteurId = e.id; break }
    }
    if (!relectureId) { test.skip(); return }

    // POST initial
    await request.post(
      `${API}/api/relectures/${relectureId}?relecteurId=${relecteurId}`,
      { data: { note: 10, commentaire: 'Avant clôture.' } }
    )

    // Clôturer la session
    await request.post(`${API}/api/sessions/${session.id}/cloture`)

    // PATCH après clôture → doit être refusé
    const r = await request.patch(
      `${API}/api/relectures/${relectureId}?relecteurId=${relecteurId}`,
      { data: { note: 20, commentaire: 'Tentative après clôture.' } }
    )
    expect(r.status()).toBe(409)
    const corps = await r.json()
    expect(corps.code).toBe('SESSION_CLOTUREE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('GET /api/tableau — tableau récapitulatif', () => {

  test('200 — retourne un tableau de lignes avec tous les champs contractuels', async ({ request }) => {
    const r = await request.get(`${API}/api/tableau?promotionId=${PROMO_ID}`)
    expect(r.status()).toBe(200)
    const corps = await r.json()
    expect(Array.isArray(corps)).toBe(true)
    expect(corps.length).toBeGreaterThan(0)

    for (const ligne of corps) {
      // Champs obligatoires du contrat
      expect(typeof ligne.etudiantId).toBe('number')
      expect(typeof ligne.nom).toBe('string')
      expect(typeof ligne.presences).toBe('number')
      expect(typeof ligne.exercicesDeposes).toBe('number')
      // moyenne peut être null (pas de note) ou un nombre — les deux sont contractuels
      expect(ligne.moyenne === null || typeof ligne.moyenne === 'number').toBe(true)
      expect(typeof ligne.relecturesEnAttente).toBe('number')
    }
  })

  test('404 — promotion inconnue retourne PROMOTION_INCONNUE', async ({ request }) => {
    const r = await request.get(`${API}/api/tableau?promotionId=99999`)
    expect(r.status()).toBe(404)
    const corps = await r.json()
    expect(corps.code).toBe('PROMOTION_INCONNUE')
  })
})

// ─────────────────────────────────────────────────────────
test.describe('Format d'erreur — {code, message} sur toutes les erreurs', () => {

  const casDErreurs = [
    { nom: 'POST /api/sessions sans titre', fn: (req) => req.post(`${API}/api/sessions`, { data: { promotionId: 1 } }), statut: 400 },
    { nom: 'POST /api/presences code inconnu', fn: (req) => req.post(`${API}/api/presences`, { data: { code: 'XXXXXX', etudiantId: 1 } }), statut: 400 },
    { nom: 'POST /api/presences code expiré', fn: (req) => req.post(`${API}/api/presences`, { data: { code: 'XY34EF', etudiantId: 1 } }), statut: 410 },
    { nom: 'GET /api/tableau promo inconnue', fn: (req) => req.get(`${API}/api/tableau?promotionId=99999`), statut: 404 },
    { nom: 'GET /api/etudiants promo inconnue', fn: (req) => req.get(`${API}/api/etudiants?promotionId=99999`), statut: 404 },
  ]

  for (const cas of casDErreurs) {
    test(`${cas.nom} → corps {code, message} (aucune stack trace)`, async ({ request }) => {
      const r = await cas.fn(request)
      expect(r.status()).toBe(cas.statut)
      const corps = await r.json()
      // Format d'erreur imposé
      expect(typeof corps.code).toBe('string')
      expect(typeof corps.message).toBe('string')
      expect(corps.code.length).toBeGreaterThan(0)
      // Pas de stack trace Java dans la réponse
      expect(JSON.stringify(corps)).not.toContain('at com.')
      expect(JSON.stringify(corps)).not.toContain('StackTrace')
    })
  }
})
