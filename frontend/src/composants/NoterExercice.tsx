import { useState } from 'react'
import type { RelectureRendue } from '../api/relectures'
import { rendreRelecture } from '../api/relectures'
import { ErreurApi } from '../api/client'

const NOTE_MIN = 0
const NOTE_MAX = 20
const RELECTURE_VIDE = ''

/** EF6 / RG8 : le relecteur note et commente l'exercice qui lui est assigné. */
export function NoterExercice() {
  const [relectureId, setRelectureId] = useState(1)
  const [note, setNote] = useState(15)
  const [commentaire, setCommentaire] = useState(RELECTURE_VIDE)
  const [enCours, setEnCours] = useState(false)
  const [relecture, setRelecture] = useState<RelectureRendue | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  // RG8 : la validation est refaite côté client pour un retour immédiat, mais c'est
  // le serveur qui fait foi (un client peut contourner le formulaire).
  const noteInvalide = !Number.isInteger(note) || note < NOTE_MIN || note > NOTE_MAX
  const formulaireInvalide = relectureId < 1 || noteInvalide || commentaire.trim() === ''

  function message(e: unknown): string {
    if (e instanceof ErreurApi) {
      return `${e.code} — ${e.message}`
    }
    return 'Erreur inattendue.'
  }

  async function soumettre() {
    if (enCours || formulaireInvalide) return
    setEnCours(true)
    setRelecture(null)
    setErreur(null)
    try {
      setRelecture(await rendreRelecture(relectureId, note, commentaire.trim()))
    } catch (e) {
      setErreur(message(e))
    } finally {
      setEnCours(false)
    }
  }

  return (
    <section className="onglet-contenu">
      <h1>Rendre ma relecture</h1>
      <p className="sous-titre">
        RG8 : note entière de 0 à 20 ·&nbsp;RG7 : l&apos;identité du relecteur n&apos;est jamais renvoyée ·
        une relecture rendue est définitive.
      </p>

      <form
        className="formulaire"
        onSubmit={(e) => {
          e.preventDefault()
          void soumettre()
        }}
      >
        <label className="champ">
          Identifiant de la relecture assignée
          <input
            type="number"
            value={relectureId}
            min={1}
            onChange={(e) => setRelectureId(Number(e.target.value))}
            required
          />
        </label>

        <label className="champ">
          Note (0 à 20)
          <input
            type="number"
            value={note}
            min={NOTE_MIN}
            max={NOTE_MAX}
            step={1}
            aria-invalid={noteInvalide}
            aria-describedby={noteInvalide ? 'erreur-note' : undefined}
            onChange={(e) => setNote(Number(e.target.value))}
            required
          />
        </label>
        {noteInvalide && (
          <p className="erreur" id="erreur-note">
            NOTE_INVALIDE — la note doit être un entier entre 0 et 20.
          </p>
        )}

        <label className="champ">
          Commentaire
          <textarea
            value={commentaire}
            rows={4}
            maxLength={2000}
            onChange={(e) => setCommentaire(e.target.value)}
            placeholder="Ce qui va bien, ce qui pourrait être amélioré…"
            required
          />
        </label>

        <button type="submit" className="action" disabled={enCours || formulaireInvalide}>
          {enCours ? 'Envoi…' : 'Rendre ma relecture'}
        </button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {relecture && (
        <section className="carte" aria-live="polite">
          <h2>Relecture enregistrée</h2>
          <dl>
            <div>
              <dt>Note</dt>
              <dd className="note-finale">{relecture.note} / 20</dd>
            </div>
            <div>
              <dt>Exercice</dt>
              <dd>{relecture.exerciceId}</dd>
            </div>
            <div>
              <dt>Rendue le</dt>
              <dd>{new Date(relecture.rendueAt).toLocaleString('fr-FR')}</dd>
            </div>
          </dl>
          <p className="note">L&apos;exercice est désormais au statut NOTE. La relecture ne peut plus être modifiée.</p>
        </section>
      )}
    </section>
  )
}
