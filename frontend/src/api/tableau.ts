import { appelApi } from './client'

/** EF11 : une ligne du tableau récapitulatif. RG15 : moyenne sur les seuls exercices notés. */
export interface LigneTableau {
  etudiantId: number
  nom: string
  presences: number
  exercicesDeposes: number
  moyenne?: number | null
  relecturesEnAttente: number
}

export function consulterTableau(promotionId: number): Promise<LigneTableau[]> {
  return appelApi<LigneTableau[]>(`/api/tableau?promotionId=${promotionId}`)
}
