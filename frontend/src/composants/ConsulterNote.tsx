import { useState } from 'react'
import type { ExerciceDetail } from '../api/exercices'
import { consulterExercice } from '../api/exercices'
import { ErreurApi } from '../api/client'

/** EF8 / RG7 : l'étudiant relu consulte sa note et son commentaire. */
export function ConsulterNote() {
  const [exerciceId, setExerciceId] = useState(1)
  const [enCours, setEnCours] = useState(false)
  const [detail, setDetail] = useState<ExerciceDetail | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  function message(e: unknown): string {
    if (e instanceof ErreurApi) {
      return `${e.code} — ${e.message}`
    }
    return 'Erreur inattendue.'
  }

  async function consulter() {
    if (enCours || exerciceId < 1) return
    setEnCours(true)
    setDetail(null)
    setErreur(null)
    try {
      setDetail(await consulterExercice(exerciceId))
    } catch (e) {
      setErreur(message(e))
    } finally {
      setEnCours(false)
    }
  }

  const noteRecue = detail?.note !== null && detail?.note !== undefined

  return (
    <section className="onglet-contenu">
      <h1>Ma note et mon commentaire</h1>
      <p className="sous-titre">
        RG7 : l&apos;identité du relecteur n&apos;est jamais affichée · la note et le commentaire
        apparaissent dès que la relecture est rendue.
      </p>

      <form
        className="formulaire"
        onSubmit={(e) => {
          e.preventDefault()
          void consulter()
        }}
      >
        <label className="champ">
          Identifiant de mon exercice
          <input
            type="number"
            value={exerciceId}
            min={1}
            onChange={(e) => setExerciceId(Number(e.target.value))}
            required
          />
        </label>

        <button type="submit" className="action" disabled={enCours || exerciceId < 1}>
          {enCours ? 'Chargement…' : 'Consulter'}
        </button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {detail && (
        <section className="carte" aria-live="polite">
          <h2>Exercice {detail.id}</h2>
          <dl>
            <div>
              <dt>Statut</dt>
              <dd>{detail.statut}</dd>
            </div>
            <div>
              <dt>Note</dt>
              <dd className="note-finale">{noteRecue ? `${detail.note} / 20` : 'Pas encore notée'}</dd>
            </div>
            <div>
              <dt>Commentaire</dt>
              <dd>{detail.commentaire ?? 'Aucun commentaire pour le moment.'}</dd>
            </div>
          </dl>
          <p className="note">
            <a href={detail.lien} target="_blank" rel="noreferrer">
              Voir mon exercice
            </a>
          </p>
        </section>
      )}
    </section>
  )
}
