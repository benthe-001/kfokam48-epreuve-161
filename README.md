# KFOKAM48 — Plateforme de relectures entre pairs

Application de gestion des présences et des évaluations par les pairs pour la
formation KFOKAM48 : un formateur ouvre une session, les étudiants marquent leur
présence avec un code, puis déposent un exercice qui sera relu et noté par un pair.

- **Cahier des charges** : [`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md)
  (11 EF, 16 RG, ENF1‑ENF4, B1‑B6, F1‑F3)
- **Contrat d'API** : [`api/contrat.yaml`](api/contrat.yaml)
- **Diagrammes** : [`docs/diagrammes/`](docs/diagrammes/)
- **Journal de bord** : [`docs/JOURNAL.md`](docs/JOURNAL.md)

## Stack

| Couche | Choix | Réf |
|---|---|---|
| Backend | Spring Boot 3.3.4, Java 21, Maven (`mvnw`) | B1 |
| Persistance | PostgreSQL 16 + Spring Data JPA, schéma versionné par Flyway | B5 |
| Documentation API | springdoc-openapi (Swagger UI) | B2 |
| Frontend | React 19 + TypeScript + Vite | F1 |
| Couche API front | `fetch` encapsulé (client + modules par ressource) | F3 |
| Déploiement | Docker + Docker Compose | ENF4 |

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
démarrage (migrations Flyway `V1__schema.sql` puis `V2__demo_data.sql`).

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
./mvnw.cmd test                  # 72 tests : 71 unitaires/intégration + contexte

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

**EF1** (RG1, RG17) : la session créée expire 15 minutes après son ouverture et
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
nouvelle présence enregistrée, un service dédié tente d'assigner un relecteur
tiré au sort parmi les présents. L'auteur est exclu des candidats (RG4), un
exercice n'a qu'un seul relecteur (RG5), et si aucun autre étudiant n'est
présent l'exercice reste `DEPOSE` — l'assignation est retentée à la présence
suivante (RG6).

**EF4 — ticket #5** (RG12) : `PUT /api/exercices/{id}` remplace le lien de l'exercice.
Accepté tant qu'aucun relecteur n'est assigné ; refusé (409
`RELECTEUR_DEJA_ASSIGNE`) dès l'assignation, même si la relecture n'a pas encore
été rendue. La colonne `modifie_at` est mise à jour.

**EF6 — ticket #7** (RG5, RG8) : `POST /api/relectures/{id}` enregistre la note
(entière, de 0 à 20) et le commentaire du relecteur, et fait passer l'exercice au
statut `NOTE`. Refus : note hors bornes ou non entière (400 `NOTE_INVALIDE`),
auto-relecture (403 `AUTO_RELECTURE`), relecture déjà rendue (409
`RELECTURE_DEJA_RENDUE`), relecture inexistante (404 `RELECTURE_INCONNUE`).
L'identité du relecteur n'apparaît jamais dans la réponse (RG7), et une relecture
rendue est définitive.

**EF7 — ticket #8** (RG8, RG9) : `PUT /api/relectures/{id}` corrige la note et le
commentaire d'une relecture déjà rendue. La correction est acceptée tant que la session
n'est pas clôturée ; après clôture par le formateur, elle est refusée (409
`SESSION_CLOTUREE`) et la note reste figée. La date de premier rendu n'est pas modifiée.

Le frontend expose quatre écrans dans une navigation à onglets : *Formateur ·
Ouvrir une session*, *Étudiant · Marquer ma présence*, *Étudiant · Déposer mon
exercice* et *Relecteur · Rendre ma relecture*. L'écran de présence gère aussi le
blocage RG3 (bouton désactivé avec décompte du temps restant) ; l'écran de dépôt
permet de remplacer son lien, et affiche pourquoi le remplacement devient
indisponible dès qu'un relecteur est assigné ; l'écran de relecture valide la note
côté client, affiche la note obtenue sur 20 et permet de la corriger.

Restant à faire : EF8 à EF11 (consultation de sa note par l'étudiant, clôture de
session, présence manuelle du formateur, tableau récapitulatif).

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
