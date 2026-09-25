import { appelApi } from './client'

/** EF6 : le relecteur rend sa note et son commentaire. RG7 : l'identité du relecteur n'est pas exposée. */
export interface RelectureRendue {
  id: number
  exerciceId: number
  note: number
  commentaire: string
  rendueAt: string
}

export function rendreRelecture(
  relectureId: number,
  note: number,
  commentaire: string,
): Promise<RelectureRendue> {
  return appelApi<RelectureRendue>(`/api/relectures/${relectureId}`, {
    method: 'POST',
    body: JSON.stringify({ note, commentaire }),
  })
}

/** EF7 / RG9 : corrige une note déjà rendue ; refusé après clôture de la session. */
export function corrigerRelecture(
  relectureId: number,
  note: number,
  commentaire: string,
): Promise<RelectureRendue> {
  return appelApi<RelectureRendue>(`/api/relectures/${relectureId}`, {
    method: 'PUT',
    body: JSON.stringify({ note, commentaire }),
  })
}
