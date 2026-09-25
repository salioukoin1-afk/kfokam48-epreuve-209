import React, { useState } from 'react'
import EcranFormateur from './pages/Formateur.jsx'
import EcranEtudiant from './pages/Etudiant.jsx'
import EcranRelecteur from './pages/Relecteur.jsx'

/** F2 : trois écrans — formateur, étudiant, relecteur. */
export default function App() {
  const [ecran, setEcran] = useState('formateur')

  return (
    <main style={{ fontFamily: 'system-ui, sans-serif', maxWidth: 720, margin: '0 auto', padding: '1rem' }}>
      <h1>KFOKAM48 — Présences &amp; relectures</h1>
      <nav style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <button onClick={() => setEcran('formateur')} aria-current={ecran === 'formateur'}>Formateur</button>
        <button onClick={() => setEcran('etudiant')} aria-current={ecran === 'etudiant'}>Étudiant</button>
        <button onClick={() => setEcran('relecteur')} aria-current={ecran === 'relecteur'}>Relecteur</button>
      </nav>
      {ecran === 'formateur' && <EcranFormateur />}
      {ecran === 'etudiant' && <EcranEtudiant />}
      {ecran === 'relecteur' && <EcranRelecteur />}
    </main>
  )
}
