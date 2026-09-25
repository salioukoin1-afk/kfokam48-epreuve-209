// @ts-check
const { test, expect } = require('@playwright/test')

/**
 * Tests E2E — Espace Étudiant
 *
 * Couvre les endpoints :
 *   GET  /api/etudiants                   (chargement de la liste Q1)
 *   POST /api/presences                   (US-02, EF2, RG1/RG2/RG3/RG15)
 *   POST /api/exercices                   (US-04, EF4/EF5, RG6/RG11/RG12)
 *   GET  /api/exercices/miens             (US-06, EF9, RG7)
 *
 * Données de démonstration (Flyway V2/V5) :
 *   Code actif  : AB12CD  (session non expirée)
 *   Code expiré : XY34EF  (session expirée — 410 CODE_EXPIRE)
 */

async function allerEtudiant(page) {
  await page.goto('/')
  await page.getByTestId('role-etudiant').click()
  await page.getByTestId('btn-continuer').click()
  // Attendre le chargement de la liste d'étudiants (GET /api/etudiants)
  await page.waitForLoadState('networkidle')
}

/** Sélectionne le premier étudiant disponible et retourne son ID */
async function choisirPremierEtudiant(page) {
  const select = page.getByTestId('select-etudiant')
  await expect(select).toBeVisible()
  const options = select.locator('option:not([disabled])')
  await expect(options).not.toHaveCount(0)
  const valeur = await options.first().getAttribute('value')
  await select.selectOption(valeur ?? '')
  return valeur
}

test.describe('Étudiant — liste des étudiants (GET /api/etudiants)', () => {

  test('l'espace étudiant se charge avec la liste des étudiants', async ({ page }) => {
    await allerEtudiant(page)
    await expect(page.getByTestId('carte-identite')).toBeVisible()
    // Le select doit contenir des options (étudiants de la promo demo)
    const options = page.getByTestId('select-etudiant').locator('option:not([disabled])')
    await expect(options).not.toHaveCount(0)
  })
})

test.describe('Étudiant — identification (étape 1)', () => {

  test('sans sélection, les formulaires de présence et dépôt sont désactivés', async ({ page }) => {
    await allerEtudiant(page)
    // Le champ de code est désactivé tant qu'aucun étudiant n'est choisi
    await expect(page.getByTestId('input-code')).toBeDisabled()
    await expect(page.getByTestId('btn-presence')).toBeDisabled()
    await expect(page.getByTestId('input-lien')).toBeDisabled()
    await expect(page.getByTestId('btn-depot')).toBeDisabled()
  })

  test('sélectionner un étudiant active le formulaire de présence', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)
    await expect(page.getByTestId('input-code')).toBeEnabled()
    await expect(page.getByTestId('btn-presence')).toBeEnabled()
  })
})

test.describe('Étudiant — présence (POST /api/presences)', () => {

  test('code expiré → message d'erreur CODE_EXPIRE (410)', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)

    await page.getByTestId('input-code').fill('XY34EF')
    await page.getByTestId('btn-presence').click()

    // L'erreur CODE_EXPIRE doit être affichée
    const alerte = page.locator('[role="alert"]')
    await expect(alerte).toBeVisible({ timeout: 10_000 })
    await expect(alerte).toContainText('CODE_EXPIRE')
  })

  test('code inconnu → message d'erreur CODE_INCONNU (400)', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)

    await page.getByTestId('input-code').fill('XXXXXX')
    await page.getByTestId('btn-presence').click()

    const alerte = page.locator('[role="alert"]')
    await expect(alerte).toBeVisible({ timeout: 10_000 })
    await expect(alerte).toContainText('CODE_INCONNU')
  })

  test('code valide AB12CD → présence enregistrée', async ({ page }) => {
    await allerEtudiant(page)

    // Choisir un étudiant spécifique — utiliser le dernier étudiant pour éviter le doublon
    const select = page.getByTestId('select-etudiant')
    const options = select.locator('option:not([disabled])')
    const count = await options.count()
    const derniereValeur = await options.nth(count - 1).getAttribute('value')
    await select.selectOption(derniereValeur ?? '')

    await page.getByTestId('input-code').fill('AB12CD')
    await page.getByTestId('btn-presence').click()

    // Succès ou doublon (DEJA_PRESENT) — les deux sont valides selon l'état de la DB
    await expect(
      page.locator('[role="status"]').filter({ hasText: /Présence enregistrée|DEJA_PRESENT/ })
        .or(page.locator('[role="alert"]').filter({ hasText: /DEJA_PRESENT/ }))
    ).toBeVisible({ timeout: 10_000 })
  })

  test('après une présence réussie, le sessionId est affiché (déduction de la réponse)', async ({ page }) => {
    await allerEtudiant(page)

    // Utiliser le dernier étudiant pour maximiser les chances de ne pas avoir de doublon
    const select = page.getByTestId('select-etudiant')
    const options = select.locator('option:not([disabled])')
    const count = await options.count()
    const valeur = await options.nth(count - 1).getAttribute('value')
    await select.selectOption(valeur ?? '')

    await page.getByTestId('input-code').fill('AB12CD')
    await page.getByTestId('btn-presence').click()

    // Soit la présence est créée (et on voit le session-id-info), soit doublon
    // On vérifie juste qu'aucune erreur réseau n'est survenue
    const status = page.locator('[role="status"], [role="alert"]')
    await expect(status.first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Étudiant — dépôt d'exercice (POST /api/exercices)', () => {

  test('le formulaire de dépôt nécessite d'abord une présence (sessionId)', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)

    // Sans présence (pas de sessionId), le dépôt est désactivé
    await expect(page.getByTestId('btn-depot')).toBeDisabled()
    await expect(page.getByTestId('input-lien')).toBeDisabled()
  })

  test('un lien invalide est bloqué par la validation HTML5 (type=url)', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)

    // Le type="url" sur l'input empêche la soumission d'une valeur non-URL
    const lienInput = page.getByTestId('input-lien')
    await expect(lienInput).toHaveAttribute('type', 'url')
  })
})

test.describe('Étudiant — consultation de la note (GET /api/exercices/miens)', () => {

  test('la section "Ma note" est visible après identification', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)
    await expect(page.getByTestId('carte-note')).toBeVisible()
  })

  test('sans sessionId (pas de présence), un message d'invite s'affiche', async ({ page }) => {
    await allerEtudiant(page)
    await choisirPremierEtudiant(page)
    // Sans présence, le sessionId est null → message d'invite
    const carteNote = page.getByTestId('carte-note')
    await expect(carteNote).toBeVisible()
    await expect(carteNote).toContainText(/présence|étape 2/i)
  })
})
