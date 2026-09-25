import { appelApi } from './client'

export interface PresenceEnregistree {
  id: number
  sessionId: number
  etudiantId: number
  source: string
}

export function marquerPresence(code: string, etudiantId: number): Promise<PresenceEnregistree> {
  return appelApi<PresenceEnregistree>('/api/presences', {
    method: 'POST',
    body: JSON.stringify({ code, etudiantId }),
  })
}

/** EF10 / RG13 : le formateur ajoute une présence manuelle (source FORMATEUR). */
export function ajouterPresenceFormateur(
  sessionId: number,
  etudiantId: number,
): Promise<PresenceEnregistree> {
  return appelApi<PresenceEnregistree>(`/api/sessions/${sessionId}/presences`, {
    method: 'POST',
    body: JSON.stringify({ etudiantId }),
  })
}