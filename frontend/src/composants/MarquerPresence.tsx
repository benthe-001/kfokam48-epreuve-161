import { useEffect, useState } from 'react'
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
  // RG3 : après 5 échecs, l'étudiant est bloqué 2 minutes -> on désactive le formulaire
  // et on décompte le temps restant plutôt que d'attendre un nouvel aller-retour serveur.
  const [bloqueJusqua, setBloqueJusqua] = useState(0)
  const [secondesRestantes, setSecondesRestantes] = useState(0)

  useEffect(() => {
    if (bloqueJusqua === 0) return
    const intervalle = setInterval(() => {
      const restant = Math.max(0, Math.ceil((bloqueJusqua - Date.now()) / 1000))
      setSecondesRestantes(restant)
      if (restant === 0) {
        setBloqueJusqua(0)
        setErreur(null)
      }
    }, 1000)
    return () => clearInterval(intervalle)
  }, [bloqueJusqua])

  const bloque = secondesRestantes > 0

  async function soumettre() {
    if (enCours || bloque) return
    setEnCours(true)
    setPresence(null)
    setErreur(null)
    try {
      setPresence(await marquerPresence(code.trim().toUpperCase(), etudiantId))
    } catch (e) {
      if (e instanceof ErreurApi) {
        setErreur(`${e.code} — ${e.message}`)
        if (e.statut === 429) {
          setBloqueJusqua(Date.now() + 2 * 60 * 1000)
          setSecondesRestantes(120)
        }
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
        RG18 : une seule présence par étudiant et par session ·&nbsp;
        RG3 : 5 codes errés bloquent 2 minutes.
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
          disabled={enCours || bloque || code.trim() === ''}
        >
          {bloque
            ? `Bloqué — réessayez dans ${secondesRestantes} s`
            : enCours
              ? 'Enregistrement…'
              : 'Valider ma présence'}
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
