import { api } from './client.js'

/** Un module par ressource du contrat v1.2 — les composants n'appellent jamais fetch. */
export const sessionsApi = {
  ouvrir: (titre, promotionId) => api.post('/api/sessions', { titre, promotionId }),
  cloturer: (sessionId) => api.post(`/api/sessions/${sessionId}/cloture`)
}

export const presencesApi = {
  marquer: (code, etudiantId) => api.post('/api/presences', { code, etudiantId }),
  ajouterFormateur: (sessionId, etudiantId) => api.post('/api/presences/formateur', { sessionId, etudiantId })
}

export const exercicesApi = {
  deposer: (sessionId, etudiantId, lien) => api.post('/api/exercices', { sessionId, etudiantId, lien }),
  monExercice: (sessionId, etudiantId) => api.get(`/api/exercices/miens?sessionId=${sessionId}&etudiantId=${etudiantId}`)
}

export const relecturesApi = {
  rendre: (relectureId, relecteurId, note, commentaire) =>
    api.post(`/api/relectures/${relectureId}?relecteurId=${relecteurId}`, { note, commentaire }),
  corriger: (relectureId, relecteurId, note, commentaire) =>
    api.patch(`/api/relectures/${relectureId}?relecteurId=${relecteurId}`, { note, commentaire }),
  enAttente: (etudiantId) => api.get(`/api/relectures/en-attente?etudiantId=${etudiantId}`)
}

export const tableauApi = {
  consulter: (promotionId) => api.get(`/api/tableau?promotionId=${promotionId}`)
}
