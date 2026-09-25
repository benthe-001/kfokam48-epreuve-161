import { useState } from 'react'
import type { SessionOuverte } from './api/sessions'
import { ouvrirSession } from './api/sessions'
import { ErreurApi } from './api/client'
import './App.css'

interface Promotion {
  id: number
  nom: string
}

// Données de démonstration du backend (migration V2__demo_data.sql)
const PROMOTIONS: Promotion[] = [
  { id: 1, nom: '2025-2026 B3 Développement' },
  { id: 2, nom: '2025-2026 B3 Cybersécurité' },
]

function formaterDate(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function App() {
  const [titre, setTitre] = useState('')
  const [promotionId, setPromotionId] = useState(PROMOTIONS[0].id)
  const [enCours, setEnCours] = useState(false)
  const [session, setSession] = useState<SessionOuverte | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  async function soumettre() {
    if (enCours) return
    setEnCours(true)
    setSession(null)
    setErreur(null)
    try {
      const ouverte = await ouvrirSession(titre.trim(), promotionId)
      setSession(ouverte)
    } catch (e) {
      if (e instanceof ErreurApi) {
        setErreur(`${e.code} — ${e.message}`)
      } else {
        setErreur('Erreur inattendue.')
      }
    } finally {
      setEnCours(false)
    }
  }

  return (
    <main className="page">
      <h1>Ouvrir une session</h1>
      <p className="sous-titre">
        RG1 : la session expire 15 minutes après son ouverture ·&nbsp;
        RG17 : un code unique est généré.
      </p>

      <form
        className="formulaire"
        onSubmit={(e) => {
          e.preventDefault()
          void soumettre()
        }}
      >
        <label className="champ">
          Titre de la séance
          <input
            type="text"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            placeholder="ex. Révision examen"
            required
            maxLength={200}
            autoFocus
          />
        </label>

        <label className="champ">
          Promotion
          <select
            value={promotionId}
            onChange={(e) => setPromotionId(Number(e.target.value))}
          >
            {PROMOTIONS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nom}
              </option>
            ))}
          </select>
        </label>

        <button
          type="submit"
          className="action"
          disabled={enCours || titre.trim() === ''}
        >
          {enCours ? 'Ouverture…' : 'Ouvrir la session'}
        </button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {session && (
        <section className="session-ouverte" aria-live="polite">
          <h2>Session ouverte</h2>
          <p className="code">
            Code de présence : <strong>{session.code}</strong>
          </p>
          <dl>
            <div>
              <dt>Identifiant</dt>
              <dd>{session.id}</dd>
            </div>
            <div>
              <dt>Ouverture</dt>
              <dd>{formaterDate(session.ouvertureAt)}</dd>
            </div>
            <div>
              <dt>Expiration</dt>
              <dd>{formaterDate(session.expirationAt)}</dd>
            </div>
          </dl>
        </section>
      )}
    </main>
  )
}

export default App
