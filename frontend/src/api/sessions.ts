import { appelApi } from './client'

export interface SessionOuverte {
  id: number
  code: string
  ouvertureAt: string
  expirationAt: string
}

export function ouvrirSession(titre: string, promotionId: number): Promise<SessionOuverte> {
  return appelApi<SessionOuverte>('/api/sessions', {
    method: 'POST',
    body: JSON.stringify({ titre, promotionId }),
  })
}

/** EF9 : clôture explicite de la session par le formateur (RG14). */
export interface SessionCloturee {
  id: number
  statut: string
  clotureAt: string
}

export function cloturerSession(sessionId: number): Promise<SessionCloturee> {
  return appelApi<SessionCloturee>(`/api/sessions/${sessionId}/cloture`, { method: 'POST' })
}