import { appelApi } from './client'

/** EF4 : l'étudiant dépose le lien de son exercice. */
export interface ExerciceDepose {
  id: number
  statut: string
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