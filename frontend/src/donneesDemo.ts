// Données de démonstration fournies par le backend (migration V2__demo_data.sql).
// Elles sont figées ici tant qu'aucun endpoint de consultation n'existe (contrat : EF1/EF2
// n'exposent que des POST). À remplacer par un appel API le jour où GET /api/promotions
// et GET /api/etudiants existent.

export interface Promotion {
  id: number
  nom: string
}

export interface Etudiant {
  id: number
  nom: string
  promotionId: number
}

export const PROMOTIONS: Promotion[] = [
  { id: 1, nom: '2025-2026 B3 Développement' },
  { id: 2, nom: '2025-2026 B3 Cybersécurité' },
]

export const ETUDIANTS: Etudiant[] = [
  { id: 1, nom: 'Ndiaye Awa', promotionId: 1 },
  { id: 2, nom: 'Diallo Moussa', promotionId: 1 },
  { id: 3, nom: 'Kone Fatou', promotionId: 2 },
  { id: 4, nom: 'Traore Ibrahima', promotionId: 2 },
]
