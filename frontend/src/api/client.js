/**
 * Couche HTTP UNIQUE du frontend (F3) — tout appel réseau passe ici.
 * Le format d'erreur du contrat { code, message } est désérialisé une seule fois.
 */
export class ApiError extends Error {
  constructor(code, message, status) {
    super(message)
    this.code = code
    this.status = status
  }
}

async function request(chemin, { method = 'GET', body } = {}) {
  let reponse
  try {
    reponse = await fetch(chemin, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined
    })
  } catch {
    throw new ApiError('RESEAU', 'Serveur injoignable. Vérifiez que le backend tourne.', 0)
  }

  if (reponse.status === 204) return null

  const corps = await reponse.json().catch(() => ({}))

  if (!reponse.ok) {
    throw new ApiError(corps.code || 'ERREUR_INATTENDUE', corps.message || 'Erreur inconnue.', reponse.status)
  }
  return corps
}

export const api = {
  get: (chemin) => request(chemin),
  post: (chemin, body) => request(chemin, { method: 'POST', body }),
  patch: (chemin, body) => request(chemin, { method: 'PATCH', body })
}
