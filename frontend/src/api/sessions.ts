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