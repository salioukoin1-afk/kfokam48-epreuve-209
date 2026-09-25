import React, { useEffect, useState } from 'react'
import Etat from '../components/Etat.jsx'
import { relecturesApi } from '../api/modules.js'

export default function EcranRelecteur() {
  const [etudiantId, setEtudiantId] = useState('102')
  const [liste, setListe] = useState(null)
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState(null)

  const [note, setNote] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [message, setMessage] = useState(null)
  const [erreurEnvoi, setErreurEnvoi] = useState(null)
  const [enCours, setEnCours] = useState(null)

  const charger = () => {
    setChargement(true)
    setErreur(null)
    relecturesApi.enAttente(etudiantId)
      .then(setListe)
      .catch(setErreur)
      .finally(() => setChargement(false))
  }

  useEffect(charger, [etudiantId])

  const envoyer = async (e) => {
    e.preventDefault()
    setErreurEnvoi(null)
    setMessage(null)
    const valeur = Number(note)
    try {
      if (enCours.dejaRendue) {
        await relecturesApi.corriger(enCours.relectureId, Number(etudiantId), valeur, commentaire.trim())
        setMessage('Correction enregistrée ✓')
      } else {
        await relecturesApi.rendre(enCours.relectureId, Number(etudiantId), valeur, commentaire.trim())
        setMessage('Relecture rendue ✓')
      }
      setEnCours(null); setNote(''); setCommentaire('')
      charger()
    } catch (err) {
      setErreurEnvoi(err)
    }
  }

  return (
    <section>
      <h2>Écran relecteur</h2>

      <label>
        Je suis&nbsp;
        <select value={etudiantId} onChange={(e) => setEtudiantId(e.target.value)}>
          <option value="101">Awa Ndiaye (101)</option>
          <option value="102">Boubacar Traoré (102)</option>
          <option value="103">Chantal Mbeng (103)</option>
          <option value="104">Djibril Faye (104)</option>
          <option value="105">Estelle Kona (105)</option>
        </select>
      </label>

      <h3>Exercices à relire</h3>
      <Etat chargement={chargement} erreur={erreur} onRetry={charger}>
        {liste && liste.length === 0 && <p>Aucun exercice à relire pour le moment ✓</p>}
        {liste && liste.length > 0 && (
          <ul>
            {liste.map((r) => (
              <li key={r.relectureId}>
                Exercice de <strong>{r.auteurNom}</strong> — session « {r.sessionTitre} »{' '}
                <button onClick={() => setEnCours({ ...r, dejaRendue: false })}>Relire</button>
              </li>
            ))}
          </ul>
        )}
      </Etat>

      {enCours && (
        <form onSubmit={envoyer} style={{ border: '1px solid #888', padding: '0.75rem' }}>
          <p>Relier l'exercice de {enCours.auteurNom} : <a href={enCours.lien}>{enCours.lien}</a></p>
          <input
            type="number" min={0} max={20} step={1} required
            value={note} onChange={(e) => setNote(e.target.value)}
            placeholder="Note /20 (entière)"
          />
          <textarea
            required value={commentaire} onChange={(e) => setCommentaire(e.target.value)}
            placeholder="Commentaire"
          />
          <button type="submit">Envoyer la note</button>
          <button type="button" onClick={() => setEnCours(null)}>Annuler</button>
        </form>
      )}
      {message && <p role="status">{message}</p>}
      <Etat chargement={false} erreur={erreurEnvoi} />
    </section>
  )
}
