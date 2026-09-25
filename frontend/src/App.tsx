import { useState } from 'react'
import { DeposerExercice } from './composants/DeposerExercice'
import { MarquerPresence } from './composants/MarquerPresence'
import { OuvrirSession } from './composants/OuvrirSession'
import './App.css'

type Onglet = 'formateur' | 'etudiant' | 'depot'

const ONGLETS: { cle: Onglet; libelle: string }[] = [
  { cle: 'formateur', libelle: 'Formateur · Ouvrir une session' },
  { cle: 'etudiant', libelle: 'Étudiant · Marquer ma présence' },
  { cle: 'depot', libelle: 'Étudiant · Déposer mon exercice' },
]

function App() {
  const [onglet, setOnglet] = useState<Onglet>('formateur')

  return (
    <main className="page">
      <nav className="onglets" role="tablist" aria-label="Fonctionnalités">
        {ONGLETS.map(({ cle, libelle }) => (
          <button
            key={cle}
            type="button"
            role="tab"
            id={`onglet-${cle}`}
            aria-selected={onglet === cle}
            aria-controls="zone-contenu"
            className={onglet === cle ? 'onglet actif' : 'onglet'}
            onClick={() => setOnglet(cle)}
          >
            {libelle}
          </button>
        ))}
      </nav>

      <div
        id="zone-contenu"
        role="tabpanel"
        aria-labelledby={`onglet-${onglet}`}
        className="zone-contenu"
      >
        {onglet === 'formateur' ? (
          <OuvrirSession />
        ) : onglet === 'etudiant' ? (
          <MarquerPresence />
        ) : (
          <DeposerExercice />
        )}
      </div>
    </main>
  )
}

export default App
