// @ts-check
const { test, expect } = require('@playwright/test')

/**
 * Tests E2E — Page de choix de rôle (ChoixRole)
 *
 * Couvre : navigation initiale, sélection de rôle, bouton continuer,
 *          redirection vers l'espace correspondant, retour au choix.
 */
test.describe('ChoixRole — sélection du rôle', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('la page de choix de rôle s'affiche au démarrage', async ({ page }) => {
    await expect(page.getByTestId('page-choix-role')).toBeVisible()
    await expect(page.getByTestId('grille-roles')).toBeVisible()
    await expect(page.getByTestId('btn-continuer')).toBeVisible()
  })

  test('le bouton Continuer est désactivé tant qu'aucun rôle n'est choisi', async ({ page }) => {
    await expect(page.getByTestId('btn-continuer')).toBeDisabled()
  })

  test('les trois cartes de rôle sont présentes', async ({ page }) => {
    await expect(page.getByTestId('role-formateur')).toBeVisible()
    await expect(page.getByTestId('role-etudiant')).toBeVisible()
    await expect(page.getByTestId('role-relecteur')).toBeVisible()
  })

  test('cliquer sur une carte la sélectionne (aria-pressed=true)', async ({ page }) => {
    const carte = page.getByTestId('role-etudiant')
    await carte.click()
    await expect(carte).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByTestId('btn-continuer')).toBeEnabled()
  })

  test('une seule carte peut être sélectionnée à la fois', async ({ page }) => {
    await page.getByTestId('role-formateur').click()
    await expect(page.getByTestId('role-formateur')).toHaveAttribute('aria-pressed', 'true')

    await page.getByTestId('role-etudiant').click()
    await expect(page.getByTestId('role-etudiant')).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByTestId('role-formateur')).toHaveAttribute('aria-pressed', 'false')
  })

  for (const role of ['formateur', 'etudiant', 'relecteur']) {
    test(`choisir ${role} puis Continuer → espace ${role} affiché`, async ({ page }) => {
      await page.getByTestId(`role-${role}`).click()
      await page.getByTestId('btn-continuer').click()

      // La page de choix de rôle disparaît
      await expect(page.getByTestId('page-choix-role')).not.toBeVisible()

      // Le bouton « Changer de rôle » dans le header est visible
      await expect(page.getByTestId('changer-role')).toBeVisible()
    })
  }

  test('double-cliquer sur une carte navigue directement', async ({ page }) => {
    await page.getByTestId('role-formateur').dblclick()
    await expect(page.getByTestId('page-choix-role')).not.toBeVisible()
    await expect(page.getByTestId('changer-role')).toBeVisible()
  })

  test('le bouton « Changer de rôle » ramène à la page de choix', async ({ page }) => {
    await page.getByTestId('role-formateur').click()
    await page.getByTestId('btn-continuer').click()
    await expect(page.getByTestId('changer-role')).toBeVisible()

    await page.getByTestId('changer-role').click()
    await expect(page.getByTestId('page-choix-role')).toBeVisible()
  })

  test('le toggle dark mode est présent sur la page de choix', async ({ page }) => {
    await expect(page.getByTestId('toggle-dark')).toBeVisible()
  })

  test('le toggle dark mode ajoute/retire la classe dark sur <html>', async ({ page }) => {
    // Mode clair par défaut
    const html = page.locator('html')
    await expect(html).not.toHaveClass(/dark/)

    await page.getByTestId('toggle-dark').click()
    await expect(html).toHaveClass(/dark/)

    await page.getByTestId('toggle-dark').click()
    await expect(html).not.toHaveClass(/dark/)
  })
})
