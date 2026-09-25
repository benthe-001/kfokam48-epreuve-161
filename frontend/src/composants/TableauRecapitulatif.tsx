import { useState } from 'react'
import type { LigneTableau } from '../api/tableau'
import { consulterTableau } from '../api/tableau'
import { ErreurApi } from '../api/client'
import { PROMOTIONS } from '../donneesDemo'

/** EF11 / RG10 / RG15 : récapitulatif par étudiant pour le formateur. */
export function TableauRecapitulatif() {
  const [promotionId, setPromotionId] = useState(PROMOTIONS[0].id)
  const [enCours, setEnCours] = useState(false)
  const [lignes, setLignes] = useState<LigneTableau[] | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  async function charger() {
    if (enCours) return
    setEnCours(true)
    setErreur(null)
    try {
      setLignes(await consulterTableau(promotionId))
    } catch (e) {
      setErreur(e instanceof ErreurApi ? `${e.code} — ${e.message}` : 'Erreur inattendue.')
      setLignes(null)
    } finally {
      setEnCours(false)
    }
  }

  return (
    <section className="onglet-contenu">
      <h1>Tableau récapitulatif</h1>
      <p className="sous-titre">
        RG15 : la moyenne porte sur les exercices notés uniquement ·&nbsp;RG10 : les relectures
        non rendues apparaissent dans « Relectures à faire ».
      </p>

      <form
        className="formulaire"
        onSubmit={(e) => {
          e.preventDefault()
          void charger()
        }}
      >
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

        <button type="submit" className="action" disabled={enCours}>
          {enCours ? 'Chargement…' : 'Consulter le tableau'}
        </button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      {lignes && (
        <section className="carte" aria-live="polite">
          <h2>Promotion {promotionId}</h2>
          {lignes.length === 0 ? (
            <p className="note">Aucun étudiant dans cette promotion.</p>
          ) : (
            <table className="tableau">
              <thead>
                <tr>
                  <th scope="col">Étudiant</th>
                  <th scope="col">Présences</th>
                  <th scope="col">Exercices déposés</th>
                  <th scope="col">Moyenne</th>
                  <th scope="col">Relectures à faire</th>
                </tr>
              </thead>
              <tbody>
                {lignes.map((l) => (
                  <tr key={l.etudiantId}>
                    <td>{l.nom}</td>
                    <td>{l.presences}</td>
                    <td>{l.exercicesDeposes}</td>
                    <td>{l.moyenne === null || l.moyenne === undefined ? '—' : `${l.moyenne} / 20`}</td>
                    <td>{l.relecturesEnAttente}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </section>
  )
}