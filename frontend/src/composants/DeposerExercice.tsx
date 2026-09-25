import { useState } from 'react'
import type { ExerciceDepose } from '../api/exercices'
import { deposerExercice } from '../api/exercices'
import { ErreurApi } from '../api/client'
import { ETUDIANTS } from '../donneesDemo'

const LIEN_VIDE = ''

/** EF4 : dépôt du lien de l'exercice. */
export function DeposerExercice() {
  const [sessionId, setSessionId] = useState(1)
  const [etudiantId, setEtudiantId] = useState(ETUDIANTS[0].id)
  const [lien, setLien] = useState(LIEN_VIDE)
  const [enCours, setEnCours] = useState(false)
  const [exercice, setExercice] = useState<ExerciceDepose | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  async function soumettre() {
    if (enCours) return
    setEnCours(true)
    setExercice(null)
    setErreur(null)
    try {
      setExercice(await deposerExercice(sessionId, etudiantId, lien.trim()))
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
      <h1>Déposer mon exercice</h1>
      <p className="sous-titre">
        RG11 : le dépôt reste possible après l&apos;expiration du code, tant que la session n&apos;est pas
        clôturée ·&nbsp;RG19 : un seul dépôt par étudiant et par session.
      </p>

      <form
        className="formulaire"
        onSubmit={(e) => {
          e.preventDefault()
          void soumettre()
        }}
      >
        <label className="champ">
          Session
          <input
            type="number"
            value={sessionId}
            min={1}
            onChange={(e) => setSessionId(Number(e.target.value))}
            required
          />
        </label>

        <label className="champ">
          Étudiant
          <select value={etudiantId} onChange={(e) => setEtudiantId(Number(e.target.value))}>
            {ETUDIANTS.map((et) => (
              <option key={et.id} value={et.id}>
                {et.nom}
              </option>
            ))}
          </select>
        </label>

        <label className="champ">
          Lien de l&apos;exercice
          <input
            type="url"
            value={lien}
            onChange={(e) => setLien(e.target.value)}
            placeholder="https://github.com/…"
            required
          />
        </label>

        <button type="submit" className="action" disabled={enCours || lien.trim() === ''}>
          {enCours ? 'Dépôt…' : 'Déposer mon exercice'}
        </button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {exercice && (
        <section className="carte" aria-live="polite">
          <h2>Exercice déposé</h2>
          <dl>
            <div>
              <dt>Identifiant</dt>
              <dd>{exercice.id}</dd>
            </div>
            <div>
              <dt>Statut</dt>
              <dd>{exercice.statut}</dd>
            </div>
          </dl>
          <p className="note">
            Il sera assigné à un relecteur lors d&apos;une prochaine étape ; en attendant, il reste en attente.
          </p>
        </section>
      )}
    </section>
  )
}