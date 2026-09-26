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
        RG17 : la note est la moyenne de mes deux relecteurs ·&nbsp;RG18 : si un seul a rendu,
        la note est provisoire ·&nbsp;RG7 : l&apos;identité du relecteur n&apos;est jamais affichée.
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
              <dd className={noteRecue && detail.noteProvisoire ? 'note-provisoire' : 'note-finale'}>
                {noteRecue ? `${detail.note} / 20` : 'Pas encore notée'}
              </dd>
            </div>
            {noteRecue && detail.noteProvisoire && (
              <div>
                <dt>Provisoire</dt>
                <dd>
                  Un seul de mes deux relecteurs a rendu. La note sera définitive dès que
                  l&apos;autre répondra.
                </dd>
              </div>
            )}
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
