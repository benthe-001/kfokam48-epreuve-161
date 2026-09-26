import { appelApi } from './client'

/** EF4 : l'étudiant dépose le lien de son exercice. */
export interface ExerciceDepose {
  id: number
  statut: string
}

/** EF8 : détail de l'exercice vu par l'étudiant. RG7 : jamais l'identité du relecteur. */
export interface ExerciceDetail {
  id: number
  sessionId: number
  etudiantId: number
  lien: string
  statut: string
  /** RG17 : moyenne des deux relectures rendues ; null si aucune ne l'a été. */
  note?: number | null
  /** RG18 : true quand un seul relecteur a rendu — la note est provisoire. */
  noteProvisoire: boolean
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
