import React from 'react'

/**
 * F3 : états de chargement et d'erreur explicites sur chaque écran.
 * Le code d'erreur du contrat (ex. CODE_EXPIRE) est affiché tel quel.
 */
export default function Etat({ chargement, erreur, onRetry, children }) {
  if (chargement) {
    return <p role="status">⏳ Chargement…</p>
  }
  if (erreur) {
    return (
      <div role="alert" style={{ border: '1px solid #c0392b', padding: '0.75rem', margin: '0.5rem 0' }}>
        <strong>{erreur.code}</strong>
        <p>{erreur.message}</p>
        {onRetry && <button onClick={onRetry}>Réessayer</button>}
      </div>
    )
  }
  return children || null
}
