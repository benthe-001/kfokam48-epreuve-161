# KFOKAM48 — Plateforme de relectures entre pairs

Application de gestion des présences et des évaluations par les pairs pour la
formation KFOKAM48 : un formateur ouvre une session, les étudiants marquent leur
présence avec un code, puis déposent un exercice qui sera relu et noté par un pair.

- **Cahier des charges** : [`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md)
  (11 EF, 18 RG, ENF1‑ENF4, B1‑B6, F1‑F3)
- **Contrat d'API** : [`api/contrat.yaml`](api/contrat.yaml)
- **Diagrammes** : [`docs/diagrammes/`](docs/diagrammes/)
- **Journal de bord** : [`docs/JOURNAL.md`](docs/JOURNAL.md)
- **Journal des versions** : [`CHANGELOG.md`](CHANGELOG.md)

## Stack

| Couche | Choix | Réf |
|---|---|---|
| Backend | Spring Boot 3.3.4, Java 21, Maven (`mvnw`) | B1 |
| Persistance | PostgreSQL 16 + Spring Data JPA, schéma versionné par Flyway | B5 |
| Documentation API | springdoc-openapi (Swagger UI) | B2 |
| Frontend | React 19 + TypeScript + Vite | F1 |
| Couche API front | `fetch` encapsulé (client + modules par ressource) | F3 |
| Déploiement | Docker + Docker Compose | ENF4 |

## Installation

### Prérequis

| Outil | Version | Pourquoi |
|---|---|---|
| Docker + Compose | récents | chemin le plus court : `docker compose up` |
| Java | **21** | `backend/pom.xml` impose `release 21` |
| Node.js | 20 ou plus | build Vite (testé sur Node 24) |

Avec Docker, Java et Node **ne sont pas nécessaires** : les images les embarquent.

### Vérifié depuis un clone vierge

Ces commandes ont été exécutées sur un `git clone` neuf, sans `node_modules` ni
répertoire de build :

| Commande | Résultat |
|---|---|
| `cd backend && ./mvnw.cmd test` | **114 tests, 0 échec** — BUILD SUCCESS |
| `cd frontend && npm install` | 27 paquets, 0 vulnérabilité |
| `cd frontend && npm run build` | 30 modules, build OK |
| `cd frontend && npm run lint` | 0 avertissement, 0 erreur |
| `docker compose config` | fichier valide (code de sortie 0) |

> ⚠️ `docker compose up` n'a **pas** pu être exécuté bout en boucle sur la machine
> de développement utilisée : le démon Docker y était arrêté. Seule la validité
> syntaxique du `docker-compose.yml` a été contrôlée. C'est le seul point de cette
> installation qui reste à confirmer sur un poste avec Docker démarré.

## Démarrage avec Docker (recommandé)

```bash
docker compose up --build -d
```

| Service | URL |
|---|---|
| Application (React via nginx) | <http://localhost:8081> |
| API | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> |
| PostgreSQL | `localhost:5433` — `relectures` / `relectures` |

Arrêt : `docker compose down` (ajouter `-v` pour supprimer le volume `pgdata`).

Le schéma et les données de démonstration sont créés automatiquement au premier
démarrage, par les migrations Flyway, appliquées dans l'ordre :

| Migration | Rôle |
|---|---|
| `V1__schema.sql` | schéma complet (promotions, sessions, présences, exercices, relectures) |
| `V2__demo_data.sql` | 2 promotions et 4 étudiants de démonstration |
| `V3__blocage_tentatives.sql` | compteur d'échecs de code (RG3) |
| `V4__double_relecture.sql` | unicité portée au couple (exercice, relecteur) — deux relecteurs par exercice |

Les migrations ne sont **jamais modifiées après coup** : chaque évolution en ajoute
une nouvelle, et une base déjà remplie se met à jour sans perte de données.

## Démarrage en local

Prérequis : Java 21+, Node 20+, PostgreSQL sur le port `5432` (base `relectures`).

```bash
# Backend
cd backend
./mvnw.cmd spring-boot:run       # http://localhost:8080

# Frontend (autre terminal)
cd frontend
npm install
npm run dev                      # http://localhost:5173
```

En développement, Vite proxifie `/api` vers `http://localhost:8080` : le frontend
appelle donc toujours des chemins relatifs, comme en production derrière nginx.

La connexion PostgreSQL se règle par variables d'environnement
(`DB_URL`, `DB_USER`, `DB_PASSWORD`), avec des valeurs par défaut dans
`application.properties`.

## Tests

```bash
cd backend
./mvnw.cmd test                  # 114 tests : 113 unitaires/intégration + contexte

cd frontend
npm run build                    # tsc -b && vite build
npm run lint                     # oxlint
```

Le profil de test (`src/test/resources/application-test.properties`) utilise une
base H2 en mémoire, alimentée par les mêmes migrations Flyway que la production —
aucun schéma n'est créé par Hibernate (`ddl-auto=validate`).

## Fonctionnalités livrées

| Ticket | Exigence | Endpoint | Statut |
|---|---|---|---|
| #1 | EF1 — le formateur ouvre une session et obtient un code | `POST /api/sessions` | ✅ |
| #2 | EF2 — l'étudiant marque sa présence avec un code | `POST /api/presences` | ✅ |

**EF1** (RG1, B3) : la session créée expire 15 minutes après son ouverture et
porte un code unique de 6 caractères.

**EF2** (RG1, RG2, RG13, RG18) : le marquage accepte un code connu et non expiré,
enregistre une présence `source=ETUDIANT`, et refuse un code inconnu (400), un
code expiré (410) ou un second marquage par le même étudiant (409).

**RG3 — ticket #3** : après 5 échecs `CODE_INCONNU` consécutifs, l'étudiant est
bloqué 2 minutes (429 `ETUDIANT_BLOQUE`), tous codes confondus ; le compteur est
remis à zéro dès la première présence réussie. `CODE_EXPIRE` et `DEJA_PRESENT`
ne comptent pas : ils prouvent que l'étudiant connaissait déjà un code valide.

**EF3** (RG11, RG19) : le dépôt d'un exercice accepte le lien `http`/`https`,
et le reste possible après l'expiration du code (RG11) tant que la session n'est
pas explicitement clôturée. Refus : lien non conforme (400 `LIEN_INVALIDE`),
session inexistante (404), session clôturée (409 `SESSION_CLOTUREE`) ou dépôt
déjà effectué par le même étudiant sur cette session (409
`EXERCICE_DEJA_DEPOSE`).

**EF5 — ticket #6** (RG4, RG5, RG6) : à chaque dépôt d'exercice **et** à chaque
nouvelle présence enregistrée, un service dédié assigne **deux relecteurs distincts**
tirés au sort parmi les présents. L'auteur est exclu des candidats (RG4) ; il ne
peut y en avoir plus de deux par exercice ; et si l'amphi est trop vide pour deux
candidats, l'exercice reste `EN_ATTENTE_RELECTURE` — un nouvel arrivant le complète
(RG6).
**EF4 — ticket #5** (RG12) : `PUT /api/exercices/{id}` remplace le lien de l'exercice.
Accepté tant qu'aucun relecteur n'est assigné ; refusé (409
`RELECTEUR_DEJA_ASSIGNE`) dès l'assignation, même si la relecture n'a pas encore
été rendue. La colonne `modifie_at` est mise à jour.

**EF6 — ticket #7** (RG5, RG8, RG17) : `POST /api/relectures/{id}` enregistre la note
(entière, de 0 à 20) et le commentaire du relecteur, et fait passer l'exercice au
statut `NOTE`. Refus : note hors bornes ou non entière (400 `NOTE_INVALIDE`),
auto-relecture (403 `AUTO_RELECTURE`), relecture déjà rendue (409
`RELECTURE_DEJA_RENDUE`), relecture inexistante (404 `RELECTURE_INCONNUE`).
L'identité du relecteur n'apparaît jamais dans la réponse (RG7), et une relecture
rendue est définitive.

**EF7 — ticket #8** : ~~correction d'une note déjà envoyée~~ **RETIRÉ du périmètre v1.0**
(issue #27). Avec deux relecteurs, la correction est ambiguë — laquelle des deux
notes ? moyenne à recalculer ? provisoire à repasser ? — et ni le client ni le
contrat ne tranchent. `PUT /api/relectures/{id}` est retiré plutôt que laissé à
moitié défini.
**EF8 — ticket #9** (RG7, RG17, RG18) : `GET /api/exercices/{id}` renvoie le
détail de l'exercice. RG17 : la note est la **moyenne des deux** relectures rendues.
RG18 : si un seul a rendu, la note est affichée mais marquée `noteProvisoire` et
l'exercice reste en attente. RG7 : l'identité du relecteur n'apparaît nulle part
dans la réponse — le DTO est un `record` rempli champ par champ, et un test vérifie
l'absence de tout champ le mentionnant dans le corps JSON renvoyé.

**EF9 — ticket #10** (RG11, RG14, RG10) : `POST /api/sessions/{id}/cloture` clôt
explicitement la session. La clôture est un acte volontaire du formateur,
distinct de l'expiration automatique du code au bout de 15 minutes (RG14) ; une
session déjà close est refusée (409 `SESSION_DEJA_CLOTUREE`). Après clôture, les
dépôts d'exercice, les remplacements de lien et les corrections de note sont
refusés en 409 `SESSION_CLOTUREE`. RG10 : les exercices sans relecture rendue
restent au statut `EN_ATTENTE_RELECTURE` et restent consultables — la clôture ne
supprime ni ne modifie aucun exercice.

**EF10 — ticket #11** (RG13, RG18) : `POST /api/sessions/{id}/presences` ajoute une
présence avec `source=FORMATEUR`. RG13 : l'ajout reste possible après l'expiration
du code de présence (pour rattraper un étudiant oublié), mais il est refusé après
la clôture de la session (409 `SESSION_CLOTUREE`). Un étudiant déjà présent ne peut
pas être ajouté deux fois (409 `DEJA_PRESENT`), et un identifiant inconnu renvoie
400 `ETUDIANT_INCONNU` — un 400 imposé par le contrat, à ne pas confondre avec un
404. Comme tout marquage de présence, l'ajout manuel déclenche une nouvelle
tentative d'assignation d'un relecteur (RG6).

**EF11 — ticket #12** (RG10, RG15, RG17) : `GET /api/tableau?promotionId=` renvoie une
ligne par étudiant de la promotion : présences, exercices déposés, moyenne et
relectures encore à rendre. RG15 : la moyenne ne porte que sur les exercices
notés, toutes sessions de la promotion confondues, et vaut `null` — jamais `0` —
si l'étudiant n'a aucun exercice noté. RG10 : les relectures non rendues sont
visibles via `relecturesEnAttente`. Une promotion inconnue renvoie 404
`PROMOTION_INCONNUE`. Le tableau est calculé par quatre requêtes d'agrégation,
sans boucle qui interroge la base.

Le frontend expose six écrans dans une navigation à onglets : *Formateur ·
Ouvrir une session* (avec le bouton de clôture et l'ajout manuel d'une présence),
*Formateur · Tableau récapitulatif*, *Étudiant · Marquer ma présence*, *Étudiant ·
Déposer mon exercice*, *Étudiant · Consulter ma note* et *Relecteur · Rendre ma
relecture*. L'écran de présence gère
aussi le blocage RG3 (bouton désactivé avec décompte du temps restant) ; l'écran de
dépôt permet de remplacer son lien, et affiche pourquoi le remplacement devient
indisponible dès qu'un relecteur est assigné ; l'écran de relecture valide la note
côté client, affiche la note obtenue sur 20 et permet de la corriger ; l'écran de
consultation affiche « Pas encore notée » tant que la relecture n'est pas rendue.

Backlog terminé : les onze fonctionnalités EF1 à EF11 du cahier des charges sont
implémentées, testées et documentées.

## Changement de besoin (issue #27)

Après test de la v0.1, le client a demandé que **chaque exercice soit relu par deux
pairs** et non un seul, la note retenue étant la moyenne des deux, provisoire tant
qu'un seul a rendu. Cela **contredit Q6** et a été traité comme un sujet à part
entière : analyse (CDC, diagrammes), migration `V4`, contrat, code et tests, sur une
branche et une pull request dédiées — le correctif de concurrence (issue #25) vit
dans une autre PR.

Conséquence assumée : **EF7 (correction d'une note) sort du périmètre de la v1.0**,
faute de réponse du client et du contrat sur la façon de corriger l'une des deux
notes. Détails dans `docs/JOURNAL.md` et dans l'issue #27.

## Backlog restant

Trié par priorité, et avec la raison du rang : ce qui n'a pas été fait n'est pas
seulement « ce qu'il reste à faire », c'est aussi ce qu'on a délibérément écarté.

### 1. À traiter en premier — bloquant en usage

| Sujet | Pourquoi c'est en tête |
|---|---|
| **Relancer une relecture restée sans réponse** | Avec deux relecteurs par exercice, la probabilité qu'un pair ne rende jamais augmente. Aujourd'hui la note reste provisoire et l'exercice reste en attente **indéfiniment** : rien ne le débloque. C'est le manque le plus visible de la v1.0, et il est directement issu du changement de besoin. Décision à prendre : délai d'expiration, relance automatique, ou alerte au formateur. |

### 2. Manques fonctionnels, non bloquants

| Sujet | Portée |
|---|---|
| **EF7 — correction d'une note** | Reportée, pas abandonnée. Reprise possible une fois la règle de correction tranchée avec deux relecteurs (laquelle ? moyenne ? provisoire ?). |
| **Authentification et autorisation** | Le projet n'a **ni authentification ni autorisation** : n'importe qui peut appeler les endpoints, consulter la note d'un autre, marquer une présence à la place d'un étudiant. Les règles RG4 et RG7 sont respectées côté *schéma de réponse*, pas côté *appelant*. |
| **Gestion des étudiants et promotions** | Hors périmètre assumé (RG16) : promotions et étudiants sont des données de démonstration, sans interface d'administration. Toute vraie utilisation en demande une. |
| **Revoir la note après clôture** | L'exercice fige sa note à la clôture (RG14). C'est le comportement voulu, mais il n'a pas été validé avec le formateur. |

### 3. Qualité et dette technique

| Sujet | Constat |
|---|---|
| **Tests du frontend** | Le frontend n'a aucun test automatisé : seule la compilation TypeScript et le lint sont vérifiés. Les écrans n'ont pas été testés dans un navigateur. |
| **Pas de `docker compose up` vérifié** | Le démon Docker était arrêté sur la machine de développement ; seule la validité syntaxique du `docker-compose.yml` a été contrôlée. |
| **Une requête SQL dépend d'un nom d'index H2** | `V4__double_relecture.sql` supprime `"uk_relecture_exercice_INDEX_5"`, nom généré par H2 à partir de V1. C'est stable tant que V1 ne change pas, mais c'est un couplage à surveiller. |
| **Notes décimales ignorées** | La moyenne peut être demi-entière (`13.5`). L'affichage est prévu pour, mais la question « une note provisoire compte-t-elle dans la moyenne de l'étudiant ? » mérite d'être confirmée par le formateur. |
| **Concurrence : portée du test** | Un seul scénario est couvert (deux marquages simultanés). Trois marquages simultanés, ou un dépôt pendant un marquage, ne le sont pas. |

## Format des erreurs

Toute erreur de l'API respecte le format imposé (B4) :

```json
{ "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }
```

| Code | Statut | Cas |
|---|---|---|
| `CHAMP_MANQUANT` | 400 | champ obligatoire absent |
| `CODE_INCONNU` | 400 | code de présence inexistant |
| `CODE_EXPIRE` | 410 | session expirée (RG1) |
| `DEJA_PRESENT` | 409 | présence déjà enregistrée (RG18) |
| `ETUDIANT_BLOQUE` | 429 | trop de codes errés, blocage temporaire (RG3) |
| `LIEN_INVALIDE` | 400 | lien d'exercice non conforme (http/https) |
| `SESSION_INCONNUE` | 404 | session inexistante |
| `EXERCICE_DEJA_DEPOSE` | 409 | exercice déjà déposé sur cette session (RG19) |
| `SESSION_CLOTUREE` | 409 | dépôt sur une session clôturée (RG11) |
| `REQUETE_INVALIDE` | 400 | corps de requête illisible |
| `RESSOURCE_INTROUVABLE` | 404 | adresse inconnue |
| `METHODE_NON_AUTORISEE` | 405 | méthode non supportée |
| `ERREUR_INTERNE` | 500 | erreur non prévue |

La traduction code → message est centralisée dans
`backend/src/main/java/com/kf48/backend/exception/MessagesErreur.java`.

## Arborescence

```
├── api/contrat.yaml          Contrat d'API
├── backend/                  API Spring Boot
│   └── src/main/java/com/kf48/backend/
│       ├── domain/           Entités JPA
│       ├── dto/              Objets de requête/réponse
│       ├── repository/       Accès données
│       ├── service/          Règles de gestion
│       ├── controller/       Endpoints
│       └── exception/        Erreurs {code, message}
├── docs/                     Cahier des charges, diagrammes, journal
└── frontend/                 Application React
    └── src/
        ├── api/              Couche d'appels API dédiée (F3)
        ├── composants/       Écrans
        └── App.tsx           Navigation par onglets
```
