import React, { useEffect, useState } from 'react'
import Etat from '../components/Etat.jsx'
import { sessionsApi, tableauApi, presencesApi } from '../api/modules.js'

const PROMOTION_DEMO = 1

export default function EcranFormateur() {
  const [session, setSession] = useState(null)
  const [titre, setTitre] = useState('')
  const [erreurSession, setErreurSession] = useState(null)

  const [lignes, setLignes] = useState(null)
  const [chargementTableau, setChargementTableau] = useState(false)
  const [erreurTableau, setErreurTableau] = useState(null)

  const chargerTableau = () => {
    setChargementTableau(true)
    setErreurTableau(null)
    tableauApi.consulter(PROMOTION_DEMO)
      .then(setLignes)
      .catch(setErreurTableau)
      .finally(() => setChargementTableau(false))
  }

  useEffect(chargerTableau, [])

  const ouvrirSession = async (e) => {
    e.preventDefault()
    setErreurSession(null)
    try {
      const s = await sessionsApi.ouvrir(titre.trim(), PROMOTION_DEMO)
      setSession(s)
      setTitre('')
    } catch (err) {
      setErreurSession(err)
    }
  }

  const cloturer = async () => {
    setErreurSession(null)
    try {
      await sessionsApi.cloturer(session.id)
      setSession({ ...session, clotureeAt: new Date().toISOString() })
    } catch (err) {
      setErreurSession(err)
    }
  }

  const marquerPresent = async (etudiantId) => {
    setErreurTableau(null)
    try {
      await presencesApi.ajouterFormateur(session?.id ?? 1, etudiantId)
      chargerTableau()
    } catch (err) {
      setErreurTableau(err)
    }
  }

  return (
    <section>
      <h2>Écran formateur</h2>

      <form onSubmit={ouvrirSession}>
        <input
          value={titre}
          onChange={(e) => setTitre(e.target.value)}
          placeholder="Titre de la session"
          minLength={3}
          required
        />
        <button type="submit">Ouvrir une session</button>
      </form>
      <Etat chargement={false} erreur={erreurSession} />

      {session && (
        <div style={{ border: '1px solid #888', padding: '0.75rem', margin: '0.75rem 0' }}>
          <p>
            Code de présence à communiquer :{' '}
            <strong style={{ fontSize: '1.4rem', letterSpacing: '0.15em' }}>{session.code}</strong>
          </p>
          <p>Expire à {new Date(session.expirationAt).toLocaleTimeString('fr-FR')}</p>
          {!session.clotureeAt
            ? <button onClick={cloturer}>Clôturer la session (irréversible)</button>
            : <p><em>Session clôturée ✓</em></p>}
        </div>
      )}

      <h3>Tableau de la promotion</h3>
      <Etat chargement={chargementTableau} erreur={erreurTableau} onRetry={chargerTableau}>
        {lignes && (
          <table style={{ borderCollapse: 'collapse', width: '100%' }}>
            <thead>
              <tr>
                <th>Étudiant</th><th>Présences</th><th>Exercices</th>
                <th>Moyenne</th><th>Relectures dues</th><th></th>
              </tr>
            </thead>
            <tbody>
              {lignes.map((l) => (
                <tr key={l.etudiantId}>
                  <td>{l.nom}</td>
                  <td>{l.presences}</td>
                  <td>{l.exercicesDeposes}</td>
                  {/* F3 : la moyenne vient de l'API — jamais recalculée ici. */}
                  <td>{l.moyenne == null ? '—' : `${l.moyenne.toFixed(2)} /20`}</td>
                  <td>{l.relecturesEnAttente > 0 ? `⚠ ${l.relecturesEnAttente}` : '0'}</td>
                  <td>
                    {session && !session.clotureeAt && (
                      <button onClick={() => marquerPresent(l.etudiantId)}>Marquer présent</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Etat>
    </section>
  )
}
