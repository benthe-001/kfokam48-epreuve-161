import { appelApi } from './client'

/** EF4 : l'étudiant dépose le lien de son exercice. */
export interface ExerciceDepose {
  id: number
  statut: string
}

/** EF8 / RG7 : détail de l'exercice vu par l'étudiant. L'identité du relecteur n'est jamais renvoyée. */
export interface ExerciceDetail {
  id: number
  sessionId: number
  etudiantId: number
  lien: string
  statut: string
  note?: number | null
  commentaire?: string | null
}

export function deposerExercice(
  sessionId: number,
  etudiantId: number,
  lien: string,
): Promise<ExerciceDepose> {
  return appelApi<ExerciceDepose>('/api/exercices', {
    method: 'POST',
    body: JSON.stringify({ sessionId, etudiantId, lien }),
  })
}

/** EF5 / RG12 : remplace le lien, accepté tant qu'aucun relecteur n'est assigné. */
export function remplacerLienExercice(exerciceId: number, lien: string): Promise<ExerciceDepose> {
  return appelApi<ExerciceDepose>(`/api/exercices/${exerciceId}`, {
    method: 'PUT',
    body: JSON.stringify({ lien }),
  })
}

/** EF8 : consulte sa note et son commentaire. RG7 : jamais l'identité du relecteur. */
export function consulterExercice(exerciceId: number): Promise<ExerciceDetail> {
  return appelApi<ExerciceDetail>(`/api/exercices/${exerciceId}`)
}