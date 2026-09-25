export class ErreurApi extends Error {
  readonly statut: number
  readonly code: string
  constructor(statut: number, code: string, message: string) {
    super(message)
    this.statut = statut
    this.code = code
  }
}

export async function appelApi<T>(chemin: string, options: RequestInit = {}): Promise<T> {
  let reponse: Response
  try {
    reponse = await fetch(chemin, { ...options, headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) } })
  } catch {
    throw new ErreurApi(0, 'RESEAU', 'Impossible de joindre le serveur.')
  }
  if (!reponse.ok) {
    const corps = await reponse.json().catch(() => ({}))
    throw new ErreurApi(reponse.status, corps.code ?? 'ERREUR_INCONNUE', corps.message ?? `Erreur ${reponse.status}`)
  }
  const texte = await reponse.text()
  return (texte ? JSON.parse(texte) : undefined) as T
}