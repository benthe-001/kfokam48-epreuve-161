## Cahier des charges — projet KFOKAM48

### 1. Contexte et objectif
La direction de la formation KFOKAM48 a besoin d'un outil pour gérer la présence des étudiants aux sessions de cours et l'évaluation par les pairs des exercices déposés, avec un tableau de suivi centralisé pour le formateur.

### 2. Acteurs et rôles
| Acteur | Ce qu'il peut faire |
|---|---|
| Formateur | Ouvre une session, obtient un code, ajoute une présence manuelle, clôture la session, consulte le tableau récapitulatif |
| Étudiant | Marque sa présence avec un code, dépose ou remplace le lien de son exercice, consulte sa note et le commentaire reçus |
| Relecteur | Rôle temporaire porté par un étudiant présent, assigné automatiquement à l'exercice d'un pair : note et commente |

### 3. Périmètre
**Inclus** : sessions et code de présence, présences (étudiant + ajout manuel formateur), dépôt/remplacement de lien d'exercice, assignation automatique de **deux relecteurs**, notation et commentaire, tableau récapitulatif par étudiant.
**Exclus** : authentification par mot de passe (Q1), gestion administrative des étudiants et des promotions (pré-chargés en données de démo — RG16), notifications, rendu visuel/CSS (non noté selon le sujet).

### 4. Exigences fonctionnelles
| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code de présence | La création retourne un code unique et une date d'expiration = ouverture + 15 min | Must |
| EF2 | L'étudiant marque sa présence avec le code | Code valide + session non expirée → présence enregistrée, source=ETUDIANT | Must |
| EF3 | L'étudiant dépose le lien de son exercice | Lien enregistré, visible tant que la session n'est pas clôturée (RG11) | Must |
| EF4 | L'étudiant remplace le lien de son exercice | Remplacement accepté tant qu'aucun relecteur n'est assigné (RG12), sinon 409 | Should |
| EF5 | Le système assigne **deux relecteurs distincts** à chaque exercice déposé | Deux relecteurs tirés au sort parmi les présents, jamais l'auteur (RG4, RG6) | Must |
| EF6 | Le relecteur note et commente l'exercice assigné | Note entière 0–20 + commentaire enregistrés (RG8) | Must |
| EF7 | ~~Le relecteur corrige une note déjà envoyée~~ | **Retiré du périmètre v1.0** (issue #27) : ambigu avec deux relecteurs | ~~Should~~ → **Hors périmètre** |
| EF8 | L'étudiant relu consulte sa note et le commentaire | Note et commentaire visibles, identité du relecteur jamais exposée (RG7) | Must |
| EF9 | Le formateur clôture une session | Après clôture : dépôts, remplacements et corrections de note refusés (409) | Must |
| EF10 | Le formateur ajoute une présence manuelle | Présence créée avec source=FORMATEUR, possible après expiration mais pas après clôture (RG13) | Should |
| EF11 | Le formateur consulte le tableau récapitulatif | Par étudiant : présences, exercices déposés, moyenne (hors non notés), relectures en attente (RG10, RG15) | Must |

### 5. Exigences non fonctionnelles
| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | Usage principalement mobile côté étudiant (marquage de présence en amphi) | Écrans testés en résolution mobile |
| ENF2 | Volumétrie modeste (quelques dizaines d'étudiants par promotion, quelques sessions par jour) | Pas d'optimisation particulière requise, requêtes simples suffisent |
| ENF3 | Format d'erreur JSON homogène sur toute l'API | Vérifié par le test d'intégration (B6) sur au moins un cas d'erreur |
| ENF4 | Démarrage reproductible chez un tiers | `docker compose up` ou 3 commandes max, testées depuis un clone vierge |

### 6. Règles de gestion
| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| RG2 | Impossible de marquer sa présence après expiration du code | Q2, Q3 |
| RG3 | 5 échecs CODE_INCONNU consécutifs bloquent l'étudiant 2 minutes, tous codes confondus ; le compteur est remis à zéro dès la première présence réussie | Q4 + décision révisée |
| RG4 | Un étudiant ne peut jamais relire son propre exercice | Q5 |
| RG5 | Un exercice est relu par **deux relecteurs distincts**, jamais son auteur | Q6 + **changement de besoin du client (issue #27)** |
| RG6 | Le relecteur est tiré au sort parmi les présents ; si aucun n'est disponible, l'assignation est retentée à chaque nouvelle présence | Q7 + décision |
| RG7 | L'élève relu voit note et commentaire, jamais l'identité du relecteur | Q8 |
| RG8 | La note est un entier de 0 à 20 | Q9 |
| RG9 | Le relecteur peut corriger sa note tant que la session n'est pas clôturée | **Sans effet en v1.0** : EF7 retiré (issue #27) |
| RG10 | Un exercice sans relecture rendue reste « en attente » et visible comme tel dans le tableau | Q11 |
| RG11 | Un exercice peut être déposé jusqu'à la clôture de la session | Q12 |
| RG12 | Le lien d'un exercice est modifiable tant qu'aucun relecteur n'est assigné | Q13 + décision |
| RG13 | Le formateur peut ajouter une présence après expiration du code mais pas après clôture ; source=FORMATEUR | Q14 |
| RG14 | La clôture d'une session est une action explicite du formateur, distincte de l'expiration du code | Trou comblé |
| RG15 | La moyenne d'un étudiant se calcule sur ses exercices notés, toutes sessions confondues ; les exercices non notés sont exclus | Trou comblé |
| RG16 | La gestion des étudiants et promotions est hors périmètre applicatif | Trou comblé |
| RG17 | La note retenue d'un exercice est la **moyenne des deux** relectures rendues | **Changement de besoin du client (issue #27)** |
| RG18 | Si un seul des deux relecteurs a rendu, sa note est affichée mais marquée **provisoire** ; l'exercice reste en attente de la seconde relecture | **Changement de besoin du client (issue #27)** |

### 7. Zones d'ombre, hypothèses et contradictions tranchées

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Pourquoi |
|---|---|---|---|
| **Correction de note après envoi** | Q10 vs Q15 (contradiction) | **Q10 l'emporte** : le relecteur peut corriger sa note tant que le formateur n'a pas clôturé la session | Q11 décrit un comportement opérationnel concret (un état « en attente » que le formateur doit voir évoluer), ce qui suppose un système où l'état d'une relecture reste modifiable. Q15 ressemble davantage à une intention générale (« c'est plus honnête pour tout le monde ») qu'à une contrainte opérationnelle précise. En cas de conflit entre une règle précise et datée (Q10, avec sa condition explicite) et un principe général (Q15), je retiens la règle précise |
| **Clôture de session** | Aucune Qx ne le dit, mais Q10/Q12/Q13 en dépendent toutes | J'introduis une **action explicite de clôture**, distincte de l'expiration du code (RG1, 15 min automatique). J'ajoute `POST /api/sessions/{id}/cloture` au contrat, hors des 5 opérations imposées | Trois règles de gestion (Q10, Q12, Q13) utilisent « clôturer » comme un événement déclenché par le formateur, jamais comme un synonyme de l'expiration automatique du code. Sans cette opération, ces trois règles sont incodables |
| **Blocage après 5 erreurs de code** (Q4) | Ambigu : bloqué par rapport à quoi, et jusqu'à quand | Le blocage est suivi par **étudiant seul**, et ne compte **que les échecs `CODE_INCONNU`** : c'est le seul cas qui correspond à une tentative de deviner un code. `DEJA_PRESENT` et `CODE_EXPIRE` prouvent au contraire que l'étudiant connaissait déjà un code valide, ce n'est pas du brute-force. Après 5 échecs `CODE_INCONNU` consécutifs, l'étudiant est bloqué 2 minutes, tous codes confondus ; le compteur est remis à zéro dès la première présence réussie | La première version de la décision rattachait le blocage au couple (étudiant, session), ce qui est **incodable** : la majorité des échecs de brute-force surviennent avec `CODE_INCONNU`, donc sans session identifiée (l'étudiant devine un code au hasard, sans savoir à quelle session il correspond). Sans session à rattacher, il n'y a rien sur quoi compter. Le seul signal fiable est donc l'identité de l'étudiant. Le blocage reste temporaire et étroit (2 min, remis à zéro au succès) : la crainte du client — « sinon ils vont deviner les codes entre eux » — porte sur la répétition de tentatives, pas sur une sanction durable. |
| **Tirage au sort du relecteur si personne n'est disponible** (Q7) | Trou : que se passe-t-il si aucun autre étudiant n'est présent au moment du dépôt ? | Le dépôt d'exercice **réussit toujours** (conforme à Q12) ; l'exercice reste dans l'état « en attente de relecteur », et une tentative d'assignation est **rejouée à chaque nouvelle présence enregistrée sur la session**, tant qu'aucun relecteur n'a été trouvé | Bloquer le dépôt violerait Q12 (« jusqu'à ce que je clôture la session »). J'étends simplement l'état « en attente » déjà prévu par Q11 pour couvrir ce cas — cohérent avec ce que le client attend déjà de voir dans son tableau |
| **« Commencé à relire »** (Q13) | Trou : aucun état ne modélise le début d'une relecture, seule la soumission existe dans le contrat | « Commencé à relire » = **un relecteur a été assigné** à l'exercice (pas besoin d'un état intermédiaire supplémentaire) | Ajouter un état « relecture en cours » suppose une action côté relecteur (« j'ouvre l'exercice ») qui n'est demandée nulle part. Assimiler « commencé » à « assigné » reste vérifiable avec les données déjà présentes (table `relecture` avec `relecteurId`), et respecte l'esprit de la règle : une fois qu'un pair est engagé sur l'exercice, l'étudiant ne peut plus changer la cible sous lui |
| **Ajout de présence par le formateur après expiration/clôture** (Q14) | Trou : le formateur est-il soumis à la même contrainte de temps que l'étudiant (Q3) ? | Le formateur peut ajouter une présence **après expiration du code**, mais **pas après clôture de la session** | Q14 répond à un problème matériel (souci de téléphone) qui n'a de sens que si le formateur peut agir après coup — sinon la fonctionnalité est inutile. Mais la clôture est un acte volontaire et définitif du formateur lui-même : il n'y a pas de raison de le laisser agir après sa propre décision de clore |
| **Calcul de la « moyenne »** (Q16 / `GET /api/tableau`) | Trou : la moyenne par exercice n'a pas de sens | La moyenne est calculée **sur l'ensemble des exercices notés d'un étudiant, toutes sessions confondues** ; les exercices sans les deux relectures rendues sont exclus du calcul (d'où `moyenne: nullable`) | Q16 demande « par étudiant », pas « par exercice » : le tableau sert au formateur à situer un étudiant dans sa promotion. Chaque exercice est noté par la moyenne de ses deux relectures (RG17), et c'est cette note par exercice qui alimente la moyenne par étudiant |
| **Deux relecteurs par exercice** (changement de besoin du client, issue #27) | Le client a essayé la v0.1 et constate qu'**un seul relecteur ne suffit pas** : quand il ne rend rien, l'étudiant n'a aucune note | Chaque exercice est relu par **deux relecteurs distincts** (RG5 révisée) ; la note retenue est la **moyenne des deux** (RG17) ; si un seul a rendu, la note est affichée mais marquée **provisoire** (RG18) | Le motif donné par le client est directement une observation de terrain, pas une préférence : le risque d'un pair qui ne rend rien n'est pas théorique, il s'est produit. Deux pairs réduisent ce risque sans l'annuler, d'où le traitement explicite du cas « un seul rendu ». Cela **contredit Q6** (« un seul relecteur »), mais Q6 décrivait la v0.1 et le client l'a explicitement révisée ; on suit le besoin le plus récent. Contrepartie assumée : l'exercice n'atteint le statut `NOTE` que lorsque les deux relectures sont rendues, donc plus lentement qu'avant |
| **Relance d'une relecture restée sans réponse** | Trou : avec deux relecteurs, la probabilité qu'un des deux ne rende jamais augmente | **Hors périmètre de la v1.0.** La note reste provisoire et l'exercice reste en attente (cohérent avec RG10) ; aucun mécanisme de relance ou d'échéance n'est développé | Le contrat ne décrit aucune relance, et la durée de validité d'une note provisoire est une décision pédagogique avant d'être technique : la fixer ici serait inventer un besoin |
| **EF7 — correction d'une note déjà envoyée** | Sort du périmètre de la v1.0 | L'endpoint `PUT /api/relectures/{id}` est **retiré** | Avec deux relecteurs, EF7 devient ambigu : corriger laquelle des deux notes ? Recalculer la moyenne ? Repasser la note en provisoire ? Aucune de ces questions n'est tranchée par le client ni par le contrat. Laisser l'endpoint en place reviendrait à exposer une opération à moitié définie ; on le retire et on le reporte |
| **Gestion des étudiants et promotions** | Trou : rien ne décrit qui crée la liste des étudiants ou les rattache à une promotion (Q1 la présuppose) | **Hors périmètre.** Les étudiants et promotions sont pré-remplis par les données de démonstration au démarrage ; aucune interface de gestion n'est développée | Le sujet n'en parle jamais, la demande initiale du client (les 5 points) n'inclut aucune gestion administrative, et le temps disponible doit aller aux fonctionnalités listées explicitement |
| **Note 0 vs absence de note** (Q9) | Ambigu : un 0/20 réel doit être distinguable d'un exercice pas encore noté | Le statut de l'exercice (`EN_ATTENTE` / `NOTE`) porte cette distinction, indépendamment de la valeur numérique ; `moyenne` n'agrège que les exercices au statut `NOTE` | Empêche qu'un exercice non relu soit compté comme un 0 et fausse la moyenne affichée au formateur |

### 8. Contraintes techniques
Reprises telles quelles du sujet : B1–B6 (Java 17+/Maven/mvnw, contrat respecté à la lettre, séparation contrôleur/service/repository + DTO, validation + gestion centralisée des erreurs, schéma versionné Flyway/Liquibase, deux tests significatifs) et F1–F3 (framework justifié + build qui passe, trois écrans formateur/étudiant/relecteur, couche d'appels API dédiée sans logique métier dupliquée).

### 9. Livrables
Dépôt GitHub public `kfokam48-epreuve-<matricule>` contenant `/docs`, `/api`, `/backend`, `/frontend` ; issues et PR sur ce dépôt ; `README` d'installation testé ; `CHANGELOG.md` ; second dépôt `kfokam48-gitlab-<matricule>` pour l'épreuve Git ; `SOUMISSION.md` déposé sur la plateforme.

### 10. Démarche prévue
Definition of Done : un ticket est terminé quand :

le code est fusionné dans main via une PR liée à l'issue (Closes #x),
il respecte le contrat api/contrat.yaml si l'endpoint concerné y figure,
la couche contrôleur ne contient aucune requête base de données, aucune entité JPA n'est exposée en JSON (B3),
les erreurs passent par le format JSON imposé, jamais de stack trace (B4),
au moins un test couvre la règle de gestion concernée si le ticket en introduit une,
le build frontend passe, pas de console.log ni de code mort laissé,
le README/CHANGELOG sont mis à jour si le ticket change le démarrage ou le comportement observable.

Découpage temporel. Les 6 étapes du sujet sont suivies dans l'ordre imposé, sans horaire figé, mais avec un repère personnel pour éviter de s'enliser : l'analyse (étape 1) ne doit pas dépasser le tiers du temps disponible avant 18h — c'est l'étape la plus lourde au barème (38 pts), mais elle reste un moyen, pas une fin. Si un point de l'analyse bloque plus de 15 minutes (comme la contradiction Q10/Q15), la décision est tranchée et documentée en section 7 plutôt que de chercher une réponse parfaite qui n'existe pas.

Rythme des commits. Un commit correspond à une unité de travail cohérente (une règle de gestion implémentée, un endpoint, un écran), pas à une fin de journée. Push après chaque ticket fermé, jamais tout accumulé en fin de parcours — c'est à la fois une exigence du barème (malus si l'historique est concentré sur la dernière heure) et une protection contre une panne matérielle en fin de journée.

Une branche, un ticket, une PR. Chaque story Must (étape 2) ou correctif/évolution de l'étape 3 vit sur sa propre branche, nommée feature/EF3-depot-exercice ou fix/RG9-correction-note par exemple — le préfixe renvoie directement à l'exigence ou à la règle concernée, pour que le lien soit visible sans ouvrir la PR.

Séparation stricte correctif / évolution à l'étape 3. Le bug signalé par le client et le changement de besoin révélés par l'enveloppe sont traités sur deux branches et deux PR distinctes, même s'ils touchent des fichiers proches — c'est un critère noté explicitement (« correctif et évolution séparés »).

Suivi du journal. JOURNAL.md est mis à jour à la fin de chaque étape, pas en une fois à 17h50 : ce que j'ai fait, ce qui m'a bloqué et combien de temps, ce que j'ai demandé à l'IA et comment j'ai vérifié sa réponse (relecture manuelle du code généré, exécution des tests, comparaison avec le contrat d'API).

Marge de sécurité. La soumission (SOUMISSION.md, hash des deux dépôts, vérification des liens en navigation privée) est prévue au moins 20 minutes avant 18h00, pour absorber un imprévu de dernière minute plutôt que de viser 17h58.

