import React, { useEffect, useState } from 'react'
import Etat from '../components/Etat.jsx'
import { presencesApi, exercicesApi } from '../api/modules.js'

export default function EcranEtudiant() {
  const [etudiantId, setEtudiantId] = useState('101')
  const [code, setCode] = useState('')
  const [messagePresence, setMessagePresence] = useState(null)
  const [erreurPresence, setErreurPresence] = useState(null)

  const [lien, setLien] = useState('')
  const [messageDepot, setMessageDepot] = useState(null)
  const [erreurDepot, setErreurDepot] = useState(null)

  const [exercice, setExercice] = useState(null)
  const [chargementExercice, setChargementExercice] = useState(false)
  const [erreurExercice, setErreurExercice] = useState(null)

  const chargerMonExercice = () => {
    setChargementExercice(true)
    setErreurExercice(null)
    exercicesApi.monExercice(1, etudiantId)
      .then(setExercice)
      .catch(setErreurExercice)
      .finally(() => setChargementExercice(false))
  }

  useEffect(chargerMonExercice, [etudiantId])

  const marquer = async (e) => {
    e.preventDefault()
    setErreurPresence(null)
    setMessagePresence(null)
    try {
      await presencesApi.marquer(code.trim(), Number(etudiantId))
      setMessagePresence('Présence enregistrée ✓')
      setCode('')
    } catch (err) {
      setErreurPresence(err)
    }
  }

  const deposer = async (e) => {
    e.preventDefault()
    setErreurDepot(null)
    setMessageDepot(null)
    try {
      const ex = await exercicesApi.deposer(1, Number(etudiantId), lien.trim())
      setMessageDepot(`Exercice déposé (statut : ${ex.statut})`)
      setLien('')
      chargerMonExercice()
    } catch (err) {
      setErreurDepot(err)
    }
  }

  return (
    <section>
      <h2>Écran étudiant</h2>

      <label>
        Je suis&nbsp;
        <select value={etudiantId} onChange={(e) => setEtudiantId(e.target.value)}>
          <option value="101">Awa Ndiaye (101)</option>
          <option value="102">Boubacar Traoré (102)</option>
          <option value="103">Chantal Mbeng (103)</option>
          <option value="104">Djibril Faye (104)</option>
          <option value="105">Estelle Kona (105)</option>
          <option value="106">Fodé Camara (106)</option>
        </select>
      </label>

      <h3>Marquer ma présence</h3>
      <form onSubmit={marquer}>
        <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Code du formateur" required />
        <button type="submit">Valider</button>
      </form>
      {messagePresence && <p role="status">{messagePresence}</p>}
      <Etat chargement={false} erreur={erreurPresence} />

      <h3>Déposer mon exercice</h3>
      <form onSubmit={deposer}>
        <input value={lien} onChange={(e) => setLien(e.target.value)} placeholder="https://… (lien du dépôt)" required />
        <button type="submit">Déposer / remplacer</button>
      </form>
      {messageDepot && <p role="status">{messageDepot}</p>}
      <Etat chargement={false} erreur={erreurDepot} />

      <h3>Ma note</h3>
      <Etat chargement={chargementExercice} erreur={erreurExercice} onRetry={chargerMonExercice}>
        {exercice && (
          <div style={{ border: '1px solid #888', padding: '0.75rem' }}>
            <p>Statut : <strong>{exercice.statut}</strong></p>
            {exercice.note != null ? (
              <p>
                Note : <strong style={{ fontSize: '1.3rem' }}>{exercice.note}/20</strong>
                <br />« {exercice.commentaire} »
              </p>
            ) : (
              <p><em>En attente de relecture…</em></p>
            )}
          </div>
        )}
      </Etat>
    </section>
  )
}
