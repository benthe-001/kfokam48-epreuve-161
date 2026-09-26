# Journal des versions

Ce fichier décrit ce qui a changé, en cohérence avec l'historique git.
Le détail du raisonnement (ce qui a été tenté, ce qui a bloqué, comment cela a été
vérifié) est dans [`docs/JOURNAL.md`](docs/JOURNAL.md).

## [1.0] — version finale

Première version livrable de la plateforme : les onze fonctionnalités du cahier des
charges, le correctif de concurrence et le changement de besoin « deux relecteurs ».

### Corrections

- **Perte de présence en concurrence** ([#25](https://github.com/benthe-001/kfokam48-epreuve-161/issues/25)).
  Deux étudiants marquant leur présence au même moment sur une session contenant un
  exercice en attente pouvaient faire perdre une présence : l'assignation de relecteur
  s'exécutait dans la transaction de l'étudiant, et son conflit annulait cette
  transaction entière. Corrigé par un verrou pessimiste sur la session, qui sérialise
  les assignations sans bloquer les marquages.
  Preuve : le test reproduit le défaut dans un commit distinct, avant le correctif.

### Changements de besoin

- **Deux relecteurs par exercice** ([#27](https://github.com/benthe-001/kfokam48-epreuve-161/issues/27)).
  Le client a constaté qu'avec un seul relecteur, un pair qui ne rend rien laisse
  l'étudiant sans note. Chaque exercice est désormais relu par **deux pairs distincts**,
  la note retenue est la **moyenne des deux**, et elle est marquée **provisoire** tant
  qu'un seul a rendu. Cela **contredit Q6** (« un seul relecteur »), que le client a
  explicitement révisée après test.
  Migration `V4__double_relecture.sql` : l'unicité passe de `exercice_id` au couple
  `(exercice_id, relecteur_id)`. Les migrations antérieures ne sont pas modifiées et
  les données existantes survivent — vérifié par test.

### Retiré du périmètre

- **EF7 — correction d'une note déjà envoyée.** Avec deux relecteurs, l'opération est
  ambiguë : corriger laquelle des deux notes, recalculer la moyenne, repasser la note
  en provisoire ? Ni le client ni le contrat n'y répondent. L'endpoint
  `PUT /api/relectures/{id}` est retiré du contrat, du code, du frontend et des tests,
  plutôt que d'être laissé à moitié défini.

### Fonctionnalités

| Réf | Fonctionnalité | Statut |
|---|---|---|
| EF1 | Ouvrir une session et obtenir un code | livrée |
| EF2 | Marquer sa présence avec un code | livrée |
| EF3 | Déposer le lien de son exercice | livrée |
| EF4 | Remplacer le lien de son exercice | livrée |
| EF5 | Assigner les relecteurs (deux, désormais) | livrée, révisée |
| EF6 | Rendre sa relecture (note + commentaire) | livrée |
| EF7 | Corriger une note déjà envoyée | **retirée** |
| EF8 | Consulter sa note et son commentaire | livrée |
| EF9 | Clôturer une session | livrée |
| EF10 | Ajouter une présence manuelle | livrée |
| EF11 | Consulter le tableau récapitulatif | livrée |

### Qualité

- **114 tests automatisés** au vert (unitaire, intégration et contexte applicatif).
- Frontend TypeScript : build `tsc` et lint sans avertissement.
- Erreurs normalisées `{ code, message }` sur toute l'API.
- Documentation de l'API générée par Swagger UI.
- Schéma versionné par migrations Flyway, appliqué automatiquement au démarrage.

## [0.1] — jalon intermédiaire

Version de travail ayant servi de support au retour du client : squelette Spring Boot
et React, contrat d'API, schéma de données, documentation, et les premières
fonctionnalités. Elle présentait un relecteur par exercice et n'évitait pas le défaut de concurrence décrit ci-dessus.
