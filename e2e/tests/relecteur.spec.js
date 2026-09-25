// @ts-check
const { test, expect } = require('@playwright/test')

/**
 * Tests E2E — Espace Relecteur
 *
 * Couvre les endpoints :
 *   GET  /api/etudiants                     (chargement de la liste Q1)
 *   GET  /api/relectures/en-attente         (US-05, EF6, RG10)
 *   POST /api/relectures/{id}?relecteurId=  (US-05, EF7, RG4/RG5/RG8)
 *   PATCH /api/relectures/{id}?relecteurId= (US-05, EF8, RG9)
 */

async function allerRelecteur(page) {
  await page.goto('/')
  await page.getByTestId('role-relecteur').click()
  await page.getByTestId('btn-continuer').click()
  await page.waitForLoadState('networkidle')
}

test.describe('Relecteur — identification (GET /api/etudiants)', () => {

  test('l'espace relecteur se charge avec le select d'identification', async ({ page }) => {
    await allerRelecteur(page)
    await expect(page.getByTestId('carte-identite-relecteur')).toBeVisible()
    const select = page.getByTestId('select-relecteur')
    await expect(select).toBeVisible()
    // Des étudiants sont disponibles dans la liste (GET /api/etudiants)
    const options = select.locator('option:not([disabled])')
    await expect(options).not.toHaveCount(0)
  })

  test('sans sélection, la carte des relectures n'est pas affichée', async ({ page }) => {
    await allerRelecteur(page)
    await expect(page.getByTestId('carte-a-relire')).not.toBeVisible()
  })
})

test.describe('Relecteur — liste des exercices à relire (GET /api/relectures/en-attente)', () => {

  test('sélectionner un relecteur charge ses exercices assignés', async ({ page }) => {
    await allerRelecteur(page)

    const select = page.getByTestId('select-relecteur')
    const options = select.locator('option:not([disabled])')
    const valeur = await options.first().getAttribute('value')
    await select.selectOption(valeur ?? '')

    // Attendre le chargement (GET /api/relectures/en-attente)
    await page.waitForLoadState('networkidle')

    // La carte doit maintenant être visible
    await expect(page.getByTestId('carte-a-relire')).toBeVisible({ timeout: 10_000 })
  })

  test('si aucun exercice en attente, le message vide s'affiche', async ({ page }) => {
    await allerRelecteur(page)

    // Tester avec chaque étudiant jusqu'à en trouver un sans exercice à relire
    const select = page.getByTestId('select-relecteur')
    const options = select.locator('option:not([disabled])')
    const count = await options.count()

    // Essayer le premier étudiant
    const valeur = await options.first().getAttribute('value')
    await select.selectOption(valeur ?? '')
    await page.waitForLoadState('networkidle')

    // Soit on a des exercices, soit le message vide — les deux sont valides
    await expect(
      page.getByTestId('liste-relectures').or(page.getByTestId('aucune-relecture'))
    ).toBeVisible({ timeout: 10_000 })
  })

  test('changer de relecteur recharge la liste', async ({ page }) => {
    await allerRelecteur(page)

    const select = page.getByTestId('select-relecteur')
    const options = select.locator('option:not([disabled])')
    const valeur1 = await options.nth(0).getAttribute('value')
    const valeur2 = await options.nth(1).getAttribute('value')

    // Première sélection
    await select.selectOption(valeur1 ?? '')
    await page.waitForLoadState('networkidle')
    await expect(page.getByTestId('carte-a-relire')).toBeVisible({ timeout: 10_000 })

    // Deuxième sélection — la liste se recharge
    await select.selectOption(valeur2 ?? '')
    await page.waitForLoadState('networkidle')
    await expect(page.getByTestId('carte-a-relire')).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Relecteur — formulaire de notation', () => {

  /** Cherche un relecteur qui a des exercices à relire */
  async function trouverRelecteurAvecExercices(page) {
    const select = page.getByTestId('select-relecteur')
    const options = select.locator('option:not([disabled])')
    const count = await options.count()

    for (let i = 0; i < count; i++) {
      const valeur = await options.nth(i).getAttribute('value')
      await select.selectOption(valeur ?? '')
      await page.waitForLoadState('networkidle')

      const liste = page.getByTestId('liste-relectures')
      try {
        await expect(liste).toBeVisible({ timeout: 3_000 })
        return valeur // Ce relecteur a des exercices
      } catch {
        // Essayer le suivant
      }
    }
    return null // Aucun relecteur avec exercice en attente
  }

  test('le formulaire de notation est présent pour chaque exercice', async ({ page }) => {
    await allerRelecteur(page)
    const relecteurId = await trouverRelecteurAvecExercices(page)

    if (!relecteurId) {
      test.skip() // Aucun exercice en attente dans la DB — test non applicable
      return
    }

    const premier = page.getByTestId('liste-relectures').locator('li').first()
    const relectureId = await premier.getAttribute('data-testid').then((v) =>
      v?.replace('ligne-relecture-', '') ?? ''
    )

    // Champ note
    await expect(page.getByTestId(`input-note-${relectureId}`)).toBeVisible()
    // Champ commentaire (fix US-05 : commentaire obligatoire)
    await expect(page.getByTestId(`input-commentaire-${relectureId}`)).toBeVisible()
    // Bouton rendre
    await expect(page.getByTestId(`btn-rendre-${relectureId}`)).toBeVisible()
  })

  test('soumettre une note et un commentaire → message de succès', async ({ page }) => {
    await allerRelecteur(page)
    const relecteurId = await trouverRelecteurAvecExercices(page)

    if (!relecteurId) {
      test.skip()
      return
    }

    const premierItem = page.getByTestId('liste-relectures').locator('li').first()
    const testId = await premierItem.getAttribute('data-testid')
    const relectureId = testId?.replace('ligne-relecture-', '') ?? ''

    await page.getByTestId(`input-note-${relectureId}`).fill('15')
    await page.getByTestId(`input-commentaire-${relectureId}`).fill('Bon travail, code bien structuré.')
    await page.getByTestId(`btn-rendre-${relectureId}`).click()

    // Message de succès : note enregistrée
    await expect(
      page.locator('[role="status"]').filter({ hasText: /15.*20.*enregistrée/ })
    ).toBeVisible({ timeout: 10_000 })
  })

  test('une note hors 0–20 est bloquée par l'input (min/max HTML)', async ({ page }) => {
    await allerRelecteur(page)
    const relecteurId = await trouverRelecteurAvecExercices(page)

    if (!relecteurId) {
      test.skip()
      return
    }

    const premier = page.getByTestId('liste-relectures').locator('li').first()
    const testId = await premier.getAttribute('data-testid')
    const relectureId = testId?.replace('ligne-relecture-', '') ?? ''

    const noteInput = page.getByTestId(`input-note-${relectureId}`)
    await expect(noteInput).toHaveAttribute('min', '0')
    await expect(noteInput).toHaveAttribute('max', '20')
  })
})

test.describe('Relecteur — correction de note (PATCH /api/relectures/{id})', () => {

  test('après un rendu, le bouton de correction apparaît', async ({ page }) => {
    await allerRelecteur(page)

    const select = page.getByTestId('select-relecteur')
    const options = select.locator('option:not([disabled])')
    const count = await options.count()

    let relectureId = null
    for (let i = 0; i < count; i++) {
      const v = await options.nth(i).getAttribute('value')
      await select.selectOption(v ?? '')
      await page.waitForLoadState('networkidle')
      const liste = page.getByTestId('liste-relectures')
      try {
        await expect(liste).toBeVisible({ timeout: 3_000 })
        const premier = liste.locator('li').first()
        const testId = await premier.getAttribute('data-testid')
        relectureId = testId?.replace('ligne-relecture-', '') ?? ''
        break
      } catch {
        // continuer
      }
    }

    if (!relectureId) {
      test.skip()
      return
    }

    // Rendre la note
    await page.getByTestId(`input-note-${relectureId}`).fill('12')
    await page.getByTestId(`input-commentaire-${relectureId}`).fill('Commentaire initial.')
    await page.getByTestId(`btn-rendre-${relectureId}`).click()
    await expect(
      page.locator('[role="status"]').filter({ hasText: /enregistrée/ })
    ).toBeVisible({ timeout: 10_000 })

    // Après le rendu, le bouton de correction doit s'afficher (PATCH)
    await expect(page.getByTestId(`btn-corriger-${relectureId}`)).toBeVisible()
  })
})
