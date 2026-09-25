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

## Étape 2 — Ticket #1 : ouvrir une session (EF1)

Fait : squelette Spring Boot 3.3.4 / Java 21 avec wrapper Maven (B1), schéma
versionné `V1__schema.sql` par Flyway couvrant les 5 tables du modèle D2 avec les
contraintes d'unicité et de domaine (B5, RG5, RG8, RG13, RG14, RG17, RG18, RG19),
puis `POST /api/sessions` en couches contrôleur / service / repository avec DTO
(B3). Le code généré est de 6 caractères alphanumériques, unique en base, et
l'expiration vaut ouverture + 15 min (RG1). Format d'erreur `{code, message}`
centralisé dans un `@RestControllerAdvice` (B4) : plus aucune page d'erreur Spring
ne peut fuir. Tests : durée de 15 min, unicité du code, 400 sur champ manquant.

Bloqué : aucun blocage bloquant. Deux obstacles techniques contournés : la
compilation échouait d'abord sur 22 erreurs car les déclarations de `package`
donnaient `com.kf48.relectures` et les imports `com.kfokam48.relectures`, alors
que les fichiers vivaient sous `com/kf48/backend` — j'ai aligné le tout sur
`com.kf48.backend`, seul préfixe cohérent avec l'arborescence réelle. Ensuite les
tests échouaient en essayant de joindre PostgreSQL : ajout de
`@ActiveProfiles("test")` et d'un profil H2 alimenté par les mêmes migrations
Flyway, conformément à B5 (le schéma vient de Flyway, jamais de Hibernate).

IA : m'a aidé à poser le squelette, les couches et le gestionnaire d'erreurs, puis
à diagnostiquer les deux pannes ci-dessus à partir des logs du build. Vérifié en
lançant réellement les tests, puis en interrogeant l'API en HTTP réel (`POST`
valide → 201 avec code et expiration à +15 min ; titre absent → 400
`CHAMP_MANQUANT`).

## Étape 3 — Ticket #2 : marquer sa présence (EF2)

Fait : `POST /api/presences` en couches, avec l'entité `Presence` et sa contrainte
d'unicité `(session_id, etudiant_id)`. Le service applique l'ordre des règles du
modèle D3 : code inconnu → 400 `CODE_INCONNU` (RG2), code expiré → 410
`CODE_EXPIRE` (RG1), déjà présent → 409 `DEJA_PRESENT` (RG18), sinon création en
`source=ETUDIANT` (RG13). RG3 (blocage après 5 échecs) est volontairement laissé de
côté : c'est le ticket #3, à faire après. 7 tests ajoutés (4 côté service, 3 côté
contrôleur) — le total passe à 12, tous verts.

Bloqué : les 6 nouveaux tests échouaient d'abord tous en `Unique index violation`
sur `uk_session_code`. La cause n'était pas dans le code métier : la base H2 de
test est partagée entre les classes (`DB_CLOSE_DELAY=-1`) et
`PresenceControllerIntegrationTest` n'était pas `@Transactional`, donc sa session
de test était commitée et entrait en collision avec les autres tests. Ajout de
`@Transactional` sur cette classe, avec un commentaire expliquant pourquoi.
IA : m'a écrit le ticket à partir du code que je lui avais fourni, puis a proposé
les tests. Vérifié en relisant chaque règle mappée dans le service une par une, en
comparant les codes d'erreur au contrat `api/contrat.yaml`, et en constatant que
la table `presence` existait déjà dans `V1__schema.sql` — aucune migration
supplémentaire n'était donc nécessaire, contrairement à ce que je supposais.

## Étape 4 — Frontend (EF1 + EF2)

Fait : squelette React 19 + TypeScript + Vite, et surtout une **couche d'appels API
dédiée** (F3) : `api/client.ts` centralise `fetch`, les en-têtes JSON et la
conversion des erreurs de l'API en `ErreurApi` (statut, code, message), chaque
ressource ayant son propre module (`api/sessions.ts`, `api/presences.ts`). Aucun
`fetch` n'est appelé depuis un composant, et aucune règle métier n'est dupliquée
côté front. L'interface est une navigation à onglets : *Formateur · Ouvrir une
session* et *Étudiant · Marquer ma présence*, ce latter avec saisie du code en
majuscules, sélection de l'étudiant, états de chargement et d'erreur. Un proxy
Vite `/api` → `localhost:8080` a été ajouté, sans quoi les chemins relatifs
n'auraient rien atteint.

Bloqué : `npm create vite@latest` restait bloqué sur la confirmation « Ok to
proceed? », et le dossier utilisateur accentué (`C:\Users\Aïssa\`) faisait échouer
l'invocation du CLI. J'ai donc reproduit le template officiel `create-vite` à
l'identique (mêmes fichiers, mêmes scripts, renommage des fichiers `_gitignore` et
`_oxlintrc.json`). Une première installation npm a aussi produit un
`node_modules` corrompu (fichier `cli.js` de Vite manquant), résolu en
réinstallant à partir d'un `package-lock.json` propre.

IA : m'a généré la couche API et les composants. Vérifié par `npm run build`
(`tsc -b` puis build Vite) et `npm run lint`, tous deux verts. Le comportement
réel des écrans n'a pas pu être validé ici, faute de navigateur dans
l'environnement — il reste à tester à la main une fois l'application lancée.

## Étape 5 — Documentation API, données de démonstration et Docker

Fait : ajout de springdoc-openapi pour exposer Swagger UI
(`http://localhost:8080/swagger-ui/index.html`), les opérations étant annotées en
référence aux règles (RG1, RG13, RG18). Ajout de la migration `V2__demo_data.sql`
(2 promotions, 4 étudiants) pour respecter l'exigence de données pré-chargées
(RG16). Dockerisation complète : `docker compose up --build` démarre PostgreSQL,
le backend et le frontend, avec healthchecks et ordre de démarrage dépendant
(ENF4). README d'installation écrit.

Bloqué : en testant l'endpoint contre du vrai HTTP, j'ai découvert que `POST
/api/sessions` renvoyait 500 — la table `promotion` était vide, faute de données
de démonstration, et la clé étrangère `fk_session_promotion` rejetait l'insertion.
C'est ce qui a motivé `V2__demo_data.sql`. Cette migration a dû être écrite sans
`ALTER SEQUENCE`, syntaxe PostgreSQL non supportée par H2 en test. Par ailleurs, le
démon Docker n'était pas démarré dans mon environnement : les images n'ont donc
pas pu être construites, seules la configuration Compose et les commandes de build
ont été validées.

IA : m'a aidé à cadrer le Dockerfile multi-étapes et le healthcheck du backend
(sonde TCP sur le port 8080, pour éviter d'ajouter actuator). Vérifié en lançant
réellement l'application et en interrogeant l'API en HTTP, puis en confiant les
tests comme garde-fou après chaque modification.

## Étape 6 — Ticket #3 : blocage après 5 échecs (EF2, RG3)

Fait : nouvelle migration `V3__blocage_tentatives.sql` (table `tentative_blocage` :
`etudiant_id`, `echecs`, `bloque_jusqua`), entité `TentativeBlocage` et son
repository. `PresenceService` applique désormais l'ordre complet : blocage en cours
→ 429 `ETUDIANT_BLOQUE`, code inconnu → 400 `CODE_INCONNU` **en incrémentant le
compteur**, code expiré → 410, déjà présent → 409, sinon remise à zéro du compteur
puis création de la présence. Nouveau code d'erreur ajouté au contrat d'API
(`429`) et au `MessagesErreur`, avec la réponse 429 documentée dans Swagger.
Côté frontend, `MarquerPresence` détecte le `statut === 429` et désactive le
bouton avec un décompte du temps restant. 5 tests ajoutés (4 côté service, 1 côté
contrôleur) — le total passe à 17, tous verts.

Bloqué : la spécification prévoyait `V2__blocage_tentatives.sql`, mais la version 2
était déjà prise par `V2__demo_data.sql` ; deux migrations de même version font
échouer le démarrage de Flyway. J'ai donc nommé la migration **V3**. Par ailleurs,
la version initiale de RG3 (blocage rattaché au couple étudiant/session) s'est
révélée **incodable** : la majorité des échecs de brute-force surviennent avec
`CODE_INCONNU`, donc sans session identifiée, et il n'y a donc rien sur quoi
compter. La règle a été révisée : suivi par étudiant seul, seuls les `CODE_INCONNU`
comptent (`CODE_EXPIRE` et `DEJA_PRESENT` prouvent au contraire que l'étudiant
connaissait un code valide), remise à zéro dès la première présence réussie.

IA : m'a aidé à implémenter le service et les tests à partir de la règle révisée,
et à repérer le conflit de numérotation des migrations. Vérifié en confrontant la
révision de RG3 au modèle de données avant de coder, puis en lançant les 17 tests,
et en corrigeant deux erreurs trouvées au passage : un `import` manquant dans
`PresenceService`, et une assertion de test de ma propre rédaction qui vérifiait
l'absence de ligne en base alors que le comportement correct est de conserver une
ligne à `echecs = 0`.

Limite : le cas « le blocage lève après 2 minutes » n'est pas couvert par un test
automatique (il faudrait 2 minutes d'attente ou une horloge injectable) ; la logique
de `estBloque()` n'est donc vérifiée qu'indirectement.
