import { useState } from 'react'
import type { ExerciceDepose } from '../api/exercices'
import { deposerExercice, remplacerLienExercice } from '../api/exercices'
import { ErreurApi } from '../api/client'
import { ETUDIANTS } from '../donneesDemo'

const LIEN_VIDE = ''

/** EF4 : dépôt du lien de l'exercice. EF5 : remplacement du lien tant qu'aucun relecteur n'est assigné. */
export function DeposerExercice() {
  const [sessionId, setSessionId] = useState(1)
  const [etudiantId, setEtudiantId] = useState(ETUDIANTS[0].id)
  const [lien, setLien] = useState(LIEN_VIDE)
  const [enCours, setEnCours] = useState(false)
  const [exercice, setExercice] = useState<ExerciceDepose | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [lienRemplace, setLienRemplace] = useState(false)

  // RG12 : le remplacement se verrouille côté serveur dès qu'un relecteur est assigné.
  // On reflète ce verrou dans l'interface dès que le statut le signale, pour expliquer
  // au student pourquoi le bouton devient indisponible.
  const verrouille = exercice?.statut === 'EN_ATTENTE_RELECTURE' || exercice?.statut === 'NOTE'

  function message(e: unknown): string {
    if (e instanceof ErreurApi) {
      return `${e.code} — ${e.message}`
    }
    return 'Erreur inattendue.'
  }

  async function soumettre() {
    if (enCours) return
    setEnCours(true)
    setExercice(null)
    setErreur(null)
    setLienRemplace(false)
    try {
      setExercice(await deposerExercice(sessionId, etudiantId, lien.trim()))
    } catch (e) {
      setErreur(message(e))
    } finally {
      setEnCours(false)
    }
  }

  async function remplacer() {
    if (enCours || !exercice) return
    setEnCours(true)
    setErreur(null)
    try {
      setExercice(await remplacerLienExercice(exercice.id, lien.trim()))
      setLienRemplace(true)
    } catch (e) {
      setErreur(message(e))
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

          <h3>Remplacer mon lien</h3>
          <p className="note">
            RG12 : accepté tant qu&apos;aucun relecteur n&apos;est assigné. Dès qu&apos;un relecteur l&apos;est,
            le remplacement est refusé — même s&apos;il n&apos;a pas encore rendu sa relecture.
          </p>
          <button
            type="button"
            className="action secondaire"
            onClick={() => void remplacer()}
            disabled={enCours || verrouille || lien.trim() === ''}
          >
            {enCours ? 'Remplacement…' : 'Remplacer le lien'}
          </button>
          {verrouille && (
            <p className="note">Un relecteur est assigné : le lien n&apos;est plus modifiable.</p>
          )}
          {lienRemplace && (
            <p className="succes" role="status">
              Lien mis à jour.
            </p>
          )}
        </section>
      )}
    </section>
  )
}