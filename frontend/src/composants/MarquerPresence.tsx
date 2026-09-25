import { useState } from 'react'
import type { PresenceEnregistree } from '../api/presences'
import { marquerPresence } from '../api/presences'
import { ErreurApi } from '../api/client'
import { ETUDIANTS } from '../donneesDemo'

/** EF2 : l'étudiant marque sa présence avec le code affiché par le formateur. */
export function MarquerPresence() {
  const [code, setCode] = useState('')
  const [etudiantId, setEtudiantId] = useState(ETUDIANTS[0].id)
  const [enCours, setEnCours] = useState(false)
  const [presence, setPresence] = useState<PresenceEnregistree | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  async function soumettre() {
    if (enCours) return
    setEnCours(true)
    setPresence(null)
    setErreur(null)
    try {
      setPresence(await marquerPresence(code.trim().toUpperCase(), etudiantId))
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
    <section className="onglet-contenu">
      <h1>Marquer ma présence</h1>
      <p className="sous-titre">
        RG1 : le code n&apos;est plus accepté après son expiration ·&nbsp;
        RG18 : une seule présence par étudiant et par session.
      </p>

      <form
        className="formulaire"
        onSubmit={(e) => {
          e.preventDefault()
          void soumettre()
        }}
      >
        <label className="champ">
          Code de présence
          <input
            type="text"
            className="saisie-code"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="ex. YGFQNM"
            required
            maxLength={6}
            autoComplete="off"
            autoFocus
          />
        </label>

        <label className="champ">
          Étudiant
          <select
            value={etudiantId}
            onChange={(e) => setEtudiantId(Number(e.target.value))}
          >
            {ETUDIANTS.map((et) => (
              <option key={et.id} value={et.id}>
                {et.nom}
              </option>
            ))}
          </select>
        </label>

        <button
          type="submit"
          className="action"
          disabled={enCours || code.trim() === ''}
        >
          {enCours ? 'Enregistrement…' : 'Valider ma présence'}
        </button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {presence && (
        <section className="carte" aria-live="polite">
          <h2>Présence enregistrée</h2>
          <dl>
            <div>
              <dt>Identifiant</dt>
              <dd>{presence.id}</dd>
            </div>
            <div>
              <dt>Session</dt>
              <dd>{presence.sessionId}</dd>
            </div>
            <div>
              <dt>Source</dt>
              <dd>{presence.source}</dd>
            </div>
          </dl>
        </section>
      )}
    </section>
  )
}
