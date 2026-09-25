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