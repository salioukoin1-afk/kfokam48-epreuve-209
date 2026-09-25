// @ts-check
const { test, expect } = require('@playwright/test')

/**
 * Tests E2E — Espace Formateur
 *
 * Couvre les endpoints :
 *   POST /api/sessions           (US-01, EF1, RG1)
 *   POST /api/sessions/{id}/cloture (US-08, EF11)
 *   POST /api/presences/formateur   (US-03, EF3, RG13)
 *   GET  /api/etudiants             (chargement de la liste pour sélection)
 *   GET  /api/tableau               (US-07, EF10)
 */

/** Navigue vers l'espace formateur depuis la page d'accueil */
async function allerFormateur(page) {
  await page.goto('/')
  await page.getByTestId('role-formateur').click()
  await page.getByTestId('btn-continuer').click()
  // Attendre que le tableau se charge (appel GET /api/tableau au montage)
  await page.waitForLoadState('networkidle')
}

test.describe('Formateur — tableau récapitulatif (GET /api/tableau)', () => {

  test('le tableau de la promotion se charge', async ({ page }) => {
    await allerFormateur(page)
    // Le composant de tableau doit être visible
    await expect(page.getByTestId('carte-tableau')).toBeVisible()
    // Le tableau contient des lignes d'étudiants
    await expect(page.getByTestId('tableau')).toBeVisible()
  })

  test('le bouton Actualiser recharge le tableau', async ({ page }) => {
    await allerFormateur(page)
    await expect(page.getByTestId('tableau')).toBeVisible()
    await page.getByTestId('btn-refresh-tableau').click()
    // Après refresh, le tableau est toujours visible (pas de régression)
    await expect(page.getByTestId('tableau')).toBeVisible()
  })
})

test.describe('Formateur — ouvrir une session (POST /api/sessions)', () => {

  test('le formulaire d'ouverture est présent', async ({ page }) => {
    await allerFormateur(page)
    await expect(page.getByTestId('carte-session')).toBeVisible()
    await expect(page.getByTestId('input-titre')).toBeVisible()
    await expect(page.getByTestId('btn-ouvrir')).toBeVisible()
  })

  test('le bouton Ouvrir est actif avec un titre', async ({ page }) => {
    await allerFormateur(page)
    await page.getByTestId('input-titre').fill('TP E2E — test playwright')
    await expect(page.getByTestId('btn-ouvrir')).toBeEnabled()
  })

  test('soumettre le formulaire crée une session et affiche le code', async ({ page }) => {
    await allerFormateur(page)
    await page.getByTestId('input-titre').fill('TP E2E — ' + Date.now())
    await page.getByTestId('btn-ouvrir').click()

    // L'encart avec le code doit apparaître
    await expect(page.getByTestId('encart-code')).toBeVisible({ timeout: 10_000 })

    // Le code est affiché (6 caractères alphanumériques)
    const code = page.getByTestId('code-session')
    await expect(code).toBeVisible()
    const texte = await code.textContent()
    expect(texte?.trim()).toMatch(/^[A-Z0-9]{6}$/)

    // Le badge d'expiration est visible
    await expect(page.getByTestId('badge-expiration')).toBeVisible()
  })

  test('ouvrir une session avec un titre vide est bloqué (HTML5 required)', async ({ page }) => {
    await allerFormateur(page)
    // Le champ est vide par défaut, le bouton est de type submit avec required
    const titre = page.getByTestId('input-titre')
    await expect(titre).toHaveValue('')
    // On ne peut pas soumettre sans remplir le champ requis (validation HTML5)
    // Vérifier que le formulaire ne part pas (encart-code absent)
    await page.getByTestId('btn-ouvrir').click({ force: true })
    await expect(page.getByTestId('encart-code')).not.toBeVisible()
  })
})

test.describe('Formateur — clôturer une session (POST /api/sessions/{id}/cloture)', () => {

  /** Ouvre une session et retourne la page */
  async function ouvrirSession(page) {
    await allerFormateur(page)
    await page.getByTestId('input-titre').fill('Session clôture E2E — ' + Date.now())
    await page.getByTestId('btn-ouvrir').click()
    await expect(page.getByTestId('encart-code')).toBeVisible({ timeout: 10_000 })
  }

  test('le bouton Clôturer est visible après ouverture', async ({ page }) => {
    await ouvrirSession(page)
    await expect(page.getByTestId('btn-cloturer')).toBeVisible()
  })

  test('la confirmation s'affiche avant la clôture', async ({ page }) => {
    await ouvrirSession(page)
    await page.getByTestId('btn-cloturer').click()
    await expect(page.getByTestId('zone-confirmation')).toBeVisible()
    await expect(page.getByTestId('btn-confirmer-cloture')).toBeVisible()
    await expect(page.getByTestId('btn-annuler-cloture')).toBeVisible()
  })

  test('annuler la clôture ferme la confirmation', async ({ page }) => {
    await ouvrirSession(page)
    await page.getByTestId('btn-cloturer').click()
    await page.getByTestId('btn-annuler-cloture').click()
    await expect(page.getByTestId('zone-confirmation')).not.toBeVisible()
    await expect(page.getByTestId('btn-cloturer')).toBeVisible()
  })

  test('confirmer la clôture affiche le badge clôturée', async ({ page }) => {
    await ouvrirSession(page)
    await page.getByTestId('btn-cloturer').click()
    await page.getByTestId('btn-confirmer-cloture').click()
    await expect(page.getByTestId('badge-cloturee')).toBeVisible({ timeout: 10_000 })
  })

  test('après clôture, le formulaire de présence manuelle est désactivé', async ({ page }) => {
    await ouvrirSession(page)
    await page.getByTestId('btn-cloturer').click()
    await page.getByTestId('btn-confirmer-cloture').click()
    await expect(page.getByTestId('badge-cloturee')).toBeVisible({ timeout: 10_000 })
    // Le bouton d'ajout de présence doit être désactivé
    await expect(page.getByTestId('btn-presence-formateur')).toBeDisabled()
  })
})

test.describe('Formateur — présence manuelle (POST /api/presences/formateur)', () => {

  test('la liste des étudiants est chargée dans le select', async ({ page }) => {
    await allerFormateur(page)
    // Ouvrir une session pour activer le formulaire
    await page.getByTestId('input-titre').fill('Session présence manuelle — ' + Date.now())
    await page.getByTestId('btn-ouvrir').click()
    await expect(page.getByTestId('encart-code')).toBeVisible({ timeout: 10_000 })

    // Le select doit avoir des options (étudiants de la promotion demo)
    const select = page.getByTestId('select-etudiant-formateur')
    const options = select.locator('option:not([disabled])')
    await expect(options).not.toHaveCount(0)
  })

  test('ajouter une présence manuelle → message de succès', async ({ page }) => {
    await allerFormateur(page)
    await page.getByTestId('input-titre').fill('Session présence manuelle — ' + Date.now())
    await page.getByTestId('btn-ouvrir').click()
    await expect(page.getByTestId('encart-code')).toBeVisible({ timeout: 10_000 })

    // Sélectionner le premier étudiant disponible
    const select = page.getByTestId('select-etudiant-formateur')
    const premiereOption = select.locator('option:not([disabled])').first()
    const valeur = await premiereOption.getAttribute('value')
    await select.selectOption(valeur ?? '')

    await page.getByTestId('btn-presence-formateur').click()

    // Un message de succès doit apparaître (source: FORMATEUR)
    await expect(page.locator('[role="status"]').filter({ hasText: /FORMATEUR/ })).toBeVisible({ timeout: 10_000 })
  })
})
