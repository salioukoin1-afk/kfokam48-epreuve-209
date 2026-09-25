// @ts-check
const { defineConfig, devices } = require('@playwright/test')

/**
 * Configuration Playwright — kfokam48-epreuve-209
 *
 * Prérequis : application démarrée via `docker compose up` ou manuellement :
 *   - Frontend Vite   : http://localhost:5173
 *   - Backend Spring  : http://localhost:8080
 *
 * Le backend doit avoir les données de démonstration chargées par Flyway :
 *   - Promotion ID 1 : « Promotion 209 — Yaoundé »
 *   - Sessions : code « AB12CD » (active), « XY34EF » (expirée)
 *
 * Lancer les tests : cd e2e && npm test
 * Rapport HTML     : npx playwright show-report
 */
module.exports = defineConfig({
  testDir: './tests',

  /* Durée max d'un test (en ms) */
  timeout: 30_000,

  /* Une seule tentative de plus en cas d'échec (flakiness de CI) */
  retries: 1,

  /* Tous les tests dans le même process pour éviter la surcharge Docker */
  workers: 1,

  /* Rapport : liste en console + HTML hors-ligne (ne pas ouvrir automatiquement) */
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  /* Artefacts de test */
  outputDir: 'test-results',

  use: {
    /** URL de base du frontend */
    baseURL: 'http://localhost:5173',

    /** Capturer une trace au premier retry pour faciliter le débogage */
    trace: 'on-first-retry',

    /** Screenshot lors d'un échec */
    screenshot: 'only-on-failure',

    /** Vidéo lors d'un échec */
    video: 'retain-on-failure',

    /** Locale française pour les messages d'interface */
    locale: 'fr-FR',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* PAS de webServer : le serveur est démarré indépendamment (docker compose up).
     Voir docs/BACKLOG_TICKETS.md US-10 et README.md section 4 pour le démarrage. */
})
