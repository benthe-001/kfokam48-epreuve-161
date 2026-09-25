import { useState } from 'react'
import { ConsulterNote } from './composants/ConsulterNote'
import { DeposerExercice } from './composants/DeposerExercice'
import { MarquerPresence } from './composants/MarquerPresence'
import { NoterExercice } from './composants/NoterExercice'
import { OuvrirSession } from './composants/OuvrirSession'
import './App.css'

type Onglet = 'formateur' | 'etudiant' | 'depot' | 'relecture' | 'note'

const ONGLETS: { cle: Onglet; libelle: string }[] = [
  { cle: 'formateur', libelle: 'Formateur · Ouvrir une session' },
  { cle: 'etudiant', libelle: 'Étudiant · Marquer ma présence' },
  { cle: 'depot', libelle: 'Étudiant · Déposer mon exercice' },
  { cle: 'note', libelle: 'Étudiant · Consulter ma note' },
  { cle: 'relecture', libelle: 'Relecteur · Rendre ma relecture' },
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
        ) : onglet === 'depot' ? (
          <DeposerExercice />
        ) : onglet === 'note' ? (
          <ConsulterNote />
        ) : (
          <NoterExercice />
        )}
      </div>
    </main>
  )
}

export default App
