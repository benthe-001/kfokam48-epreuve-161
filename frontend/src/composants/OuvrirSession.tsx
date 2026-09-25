import { useState } from 'react'
import type { SessionOuverte } from '../api/sessions'
import { cloturerSession, ouvrirSession } from '../api/sessions'
import { ErreurApi } from '../api/client'
import { PROMOTIONS } from '../donneesDemo'

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

/** EF1 : le formateur ouvre une session de cours (POST /api/sessions). */
export function OuvrirSession() {
  const [titre, setTitre] = useState('')
  const [promotionId, setPromotionId] = useState(PROMOTIONS[0].id)
  const [enCours, setEnCours] = useState(false)
  const [session, setSession] = useState<SessionOuverte | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [cloturee, setCloturee] = useState(false)

  async function soumettre() {
    if (enCours) return
    setEnCours(true)
    setSession(null)
    setErreur(null)
    setCloturee(false)
    try {
      setSession(await ouvrirSession(titre.trim(), promotionId))
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

  /**
   * EF9 : la clôture est un acte volontaire distinct de l'expiration du code (RG14).
   * Elle est définitive : une fois close, la session refuse dépôts, remplacements
   * de lien et corrections de note.
   */
  async function cloturer() {
    if (enCours || !session || cloturee) return
    setEnCours(true)
    setErreur(null)
    try {
      await cloturerSession(session.id)
      setCloturee(true)
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
      <h1>Ouvrir une session</h1>
      <p className="sous-titre">
        A savoir : la session expire 15 minutes après son ouverture ·&nbsp;
        NB : un code unique est généré.
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
        <section className="carte" aria-live="polite">
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

          <h3>Clôturer la session</h3>
          <p className="note">
            EF9 / RG14 : la clôture est un acte volontaire, distinct de l&apos;expiration
            automatique du code au bout de 15 minutes. Une fois clôturée, la session refuse
            les dépôts d&apos;exercice, les remplacements de lien et les corrections de note.
            Les exercices non encore relus restent visibles comme « en attente ».
          </p>
          <button
            type="button"
            className="action secondaire"
            onClick={() => void cloturer()}
            disabled={enCours || cloturee}
          >
            {cloturee ? 'Session clôturée' : enCours ? 'Clôture…' : 'Clôturer la session'}
          </button>
          {cloturee && (
            <p className="succes" role="status">
              Session clôturée : plus aucune modification n&apos;est possible.
            </p>
          )}
        </section>
      )}
    </section>
  )
}
