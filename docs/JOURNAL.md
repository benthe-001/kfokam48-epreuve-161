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

## Étape 7 — Ticket #4 : déposer son exercice (EF3, RG11, RG19)

Fait : `POST /api/exercices` en couches contrôleur / service / repository, avec la
table `exercice` déjà prévue par `V1__schema.sql` (contrainte
`uk_exercice_session_etudiant` = RG19) : aucune migration n'a été nécessaire.
Le service teste l'existence de la session (404 `SESSION_INCONNUE`), puis sa
clôture (409 `SESSION_CLOTUREE`), puis le dépôt déjà effectué (409
`EXERCICE_DEJA_DEPOSE`) ; le statut initial est `DEPOSE`, l'assignation d'un
relecteur (RG6) restant prévue pour le ticket #6. Le contrôleur documente les
4 statuts dans Swagger, et le contrat `api/contrat.yaml` gagne les 404 et 409
qui manquaient. Côté frontend : `api/exercices.ts`, l'onglet « Étudiant · Déposer
mon exercice » et le composant `DeposerExercice` (3e onglet). Le lien est validé
par `@Pattern` côté serveur et `type="url"` côté navigateur. 27 tests au total.

Bloqué : la compilation a échoué sur un `cannot find symbol: variable termee`
alors que la variable était déclarée trois lignes plus haut. Après avoir écarté
un problème d'encodage (un seul caractère accentué dans tout le fichier, et le
`pom.xml` hérite de `sourceEncoding=UTF-8` du parent Spring Boot), un `mvn clean
test` a reproduit l'erreur : la cause réelle était une coquille dans mon propre
code, la variable était déclarée `sessionTerminee` mais utilisée `termee`. Le
message du compilateur était donc correct ; c'est ma relecture initiale du
fichier qui avait été trompeuse, les deux identifiants ne se distinguaient pas
à l'œil sur la ligne de déclaration.

IA : m'a fourni le squelette des 6 classes, que j'ai recopié tel quel ; j'ai
ajouté le contrôleur annoté Swagger et les tests (5 service + 5 intégration). La
correction de la coquille ci-dessus a été trouvée par comparaison octet par
octet du fichier (scan des caractères de contrôle, fins de ligne, caractères
non-ASCII) après l'échec des vérifications plus évidentes. Vérifié en
lançant la suite complète (`mvnw.cmd test` : 27/27) et le build frontend
(`npm run build`, `npm run lint` : 0 erreur).

Limite : la vérification `SESSION_CLOTUREE` est écrite et couverte par le service,
mais aucun test ne la déclenche car l'endpoint de clôture (ticket #10) n'existe
pas encore : le scénario n'est atteignable que le jour où #10 existera. Le dépôt
après expiration du code (RG11), qui est le cœur du ticket, est en revanche
couvert par un test dédié.
## Étape 8 — Ticket #6 : assignation automatique d'un relecteur (RG4, RG5, RG6)

Fait : entité `Relecture` et son repository, plus une méthode de transition sur
`Exercice` (`marquerEnAttenteRelecture`). Le cœur du ticket est un service
dédié, `AssignationRelecteurService.tenterAssignerPourSession(sessionId)`, appelé
à **deux** moments : après chaque dépôt d'exercice et après chaque nouvelle
présence enregistrée sur la session. Il tire au sort un relecteur parmi les
présents, en excluant l'auteur de l'exercice ; le service est idempotent, donc un
second appel ne crée pas de doublon.

Choix : le service ne lève pas d'exception. Il ne fait que « tenter » — c'est ce
qui permet de le brancher sur deux déclencheurs sans alourdir les
contrôleurs. Le dépôt renvoie ensuite le statut réel de l'exercice, donc la
réponse reflète `EN_ATTENTE_RELECTURE` si un relecteur a pu être désigné, et
`DEPOSE` sinon.

Bloqué : les deux déclencheurs sont dans des transactions différentes. L'ordre
« écrire d'abord, tenter ensuite » est important : la présence ou l'exercice
doit être visible en base pour que le tirage parmi les présents fonctionne.

IA : m'a aidé à repérer un piège de persistance. Le changement de statut de
l'exercice se fait par *dirty checking* (entité managée renvoyée par la
requête), sans appel explicite à `save()` ; je l'ai vérifié en relisant la
transaction englobante plutôt qu'en supposant que l'assignation était écrite.

Limite : le tirage est réellement aléatoire (`SecureRandom`), donc un test ne
peut pas prédire *qui* est désigné. Les tests vérifient donc des propriétés
(l'auteur n'est jamais choisi, un seul relecteur par exercice, l'exercice reste
`DEPOSE` quand personne n'est disponible) plutôt que le résultat exact.

## Étape 9 — Ticket #5 : remplacer le lien de son exercice (EF4, RG12)

Fait : `PUT /api/exercices/{id}`, un DTO de requête validé, une méthode
`ExerciceService.remplacerLien` et une transition de domaine
`Exercice.remplacerLien` qui met aussi à jour `modifie_at`. Le contrôle de RG12
porte sur l'**existence d'une relecture assignée** (`existsByExerciceId`), et
non sur le statut de l'exercice : c'est la réalité métier — « un relecteur a-t-il
été désigné ? » — qui compte, et elle reste vraie même si le statut n'a pas
encore bougé. Le refus est donc un 409 `RELECTEUR_DEJA_ASSIGNE`, y compris quand
la relecture n'est pas encore rendue. Côté frontend, l'écran de dépôt affiche
le bouton de remplacement et explique pourquoi il se verrouille.

Bloqué : rien de bloquant. Deux codes d'erreur déjà présents dans
`MessagesErreur` (`EXERCICE_INCONNU`, `RELECTEUR_DEJA_ASSIGNE`) ont évité d'en
inventer, et aucune migration n'a été nécessaire : `modifie_at` existait déjà
dans `V1__schema.sql`.

IA : m'a aidé sur le point de conception le plus subtil du ticket — rattacher
le verrou au statut de l'exercice serait plus lisible mais fragile. Vérifié par
des tests qui distinguent les deux cas (avec et sans relecteur assigné), puis par
le build complet du backend et du frontend.

Limite : l'effet du remplacement n'a pas été vérifié depuis un vrai navigateur,
le frontend n'ayant pas de tests automatisés à ce stade ; le build `tsc` et le
lint passent, et l'appel HTTP est couvert côté intégration.


## Étape 10 — Ticket #7 : le relecteur note et commente (EF6, RG5, RG8)

Fait : `POST /api/relectures/{id}`, un service `RelectureService`, le DTO validé
`RendreRelectureRequest` et la réponse `RelectureResponse`. La transition de domaine
`Relecture.rendre(...)` renseigne la note, le commentaire et `rendue_at`, ce qui
verrouille définitivement la relecture ; `Exercice.marquerNote()` fait passer
l'exercice à `NOTE`. Ordre des contrôles : 404 `RELECTURE_INCONNUE` → 403
`AUTO_RELECTURE` → 409 `RELECTURE_DEJA_RENDUE`. Le 404 `RELECTURE_INCONNUE` a été
ajouté au `contrat.yaml`, qui ne le documentait pas sur cette route. 17 tests ajoutés
(9 côté service, 8 côté contrôleur) — le total passe à 60, tous verts. Côté frontend,
un quatrième onglet *Relecteur · Rendre ma relecture* avec validation de la note.

Bloqué : trois erreurs de ma part, trouvées par les tests et non par la relecture du
code. La première est la plus instructive : j'avais écrit `@Digits(integer = 1,
fraction = 0)` pour rejeter les décimales, en croyant limiter la note à un seul
chiffre — la note 20 et même 15 se retrouvaient rejetées en `NOTE_INVALIDE`, ce qui
casse quatre tests d'un coup. Le paramètre `integer` compte les chiffres **avant** la
virgule, pas la valeur : il fallait `integer = 2`. La deuxième : mon test des bornes
rendait deux fois **la même** relecture, alors qu'une relecture ne se rend qu'une fois —
le 409 était le comportement correct, c'est le test qui était faux. La troisième :
pour fabriquer une auto-relecture, je créais une seconde relecture sur le même
exercice, et `uk_relecture_exercice` (RG5) l'a refusée ; il a fallu passer par un
autre exercice *et* un autre auteur, car `uk_exercice_session_etudiant` (RG19) bloque
aussi le doublon. Ces trois echecs ont au passage confirme que les contraintes RG5 et
RG19 sont bien actives en base.

IA : m'a aidé sur le choix du type du champ `note`. Un `Integer` aurait rejeté `12.5`
au moment de la désérialisation JSON, ce qui produit un 400 `REQUETE_INCONNUE` — un
code générique — alors que le ticket exige explicitement `NOTE_INVALIDE` pour toute
note non entière. Un `BigDecimal` validé par `@Digits` permet de rejeter la décimale
avec le bon code. Vérifié par un test dédié (`noteNonEntiereRenvoie400NOTE_INVALIDE`),
puis par la suite complète et le build frontend.

Limite : le contrôle d'auto-relecture ne peut pas se produire par le chemin normal,
puisque l'assignation exclut l'auteur (RG4) : le test le fabrique volontairement en
base. C'est un garde-fou, pas une règle atteignable en production tant que l'auto-
relecture est impossible en amont. Par ailleurs, l'endpoint identifie le relecteur par
l'assignation et non par une authentification, le projet n'en ayant pas encore ; le
`403` est donc cohérent mais ne protège pas d'un vrai appelant malveillant. Enfin,
comme pour les étapes précédentes, l'écran n'a pas été vérifié dans un navigateur :
build `tsc` et lint passent, l'appel HTTP est couvert côté intégration.


