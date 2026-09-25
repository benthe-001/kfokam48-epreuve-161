## Étape 1 — Analyse et conception
Fait : cahier des charges (11 EF, 16 RG), 4 diagrammes en Mermaid (D1 cas
d'utilisation, D2 modèle de données, D3 séquence présence, D4 bonus états
exercice), 12 issues créées, contrat d'API complété (5 opérations imposées
inchangées + 5 ajoutées pour EF4/EF7/EF8/EF9/EF10), commit `[JALON] analyse`
poussé.

Bloqué : contradiction entre Q10 et Q15 sur la correction de note après envoi,
tranchée en faveur de Q10 — Q11 suppose un état de relecture modifiable, Q15
ressemble davantage à une intention générale qu'à une règle opérationnelle
précise. Également un trou non couvert par les 16 questions : aucune
opération de clôture de session dans le contrat imposé, alors que 3 règles
(Q10, Q12, Q13) en dépendent — ajout de `POST /api/sessions/{id}/cloture`.

IA : m'a aidé à structurer le cahier des charges, identifier les
contradictions/trous, rédiger les diagrammes Mermaid et le backlog. Vérifié
en confrontant chaque décision à une question Qx précise (jamais de décision
sans source citée), et en relisant D2 face au contrat pour s'assurer que les
statuts (DEPOSE/EN_ATTENTE_RELECTURE/NOTE) sont cohérents entre le modèle de
données et les réponses API.