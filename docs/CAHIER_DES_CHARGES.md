# Cahier des charges — PRESENCE48 (présence et relecture par les pairs)

**Auteur :** nono leonel · matricule 230
**Version :** 1 · **Date :** 2026-09-25
**Frontend choisi :** Angular 17, parce que son architecture imposée (services injectables, `HttpClient`, intercepteurs) isole naturellement la couche d'appels API exigée par F3.

> Documents liés : [SPECIFICATIONS_FONCTIONNELLES.md](SPECIFICATIONS_FONCTIONNELLES.md) (fiches détaillées, flows, user stories) ·
> [diagrammes/](diagrammes/) · [../api/contrat.yaml](../api/contrat.yaml) · [PLAN_TECHNIQUE_EQUIPES.md](PLAN_TECHNIQUE_EQUIPES.md) · [CONTRIBUTING.md](CONTRIBUTING.md)
>
> **Convention de lecture.** Chaque décision cite sa source : `Qx` = question de `CLIENT.md`, `SUJET` = énoncé,
> `CONTRAT` = `api/contrat.yaml`. Une décision marquée **[HYP-x]** est une hypothèse prise à la place du client
> (section 7) : elle n'invente pas un besoin, elle comble un trou et reste discutable.

---

## 1. Contexte et objectif

La formation KFOKAM48 suit aujourd'hui la présence et les exercices de ses étudiants à la main. Trois problèmes en découlent :
la présence est déclarative et falsifiable, les exercices rendus ne sont relus par personne de façon traçable, et le formateur
n'a aucune vue consolidée de l'assiduité et du niveau de chaque étudiant.

PRESENCE48 est une application web qui :

- prouve la présence physique par un **code court à durée de vie limitée** affiché par le formateur ;
- collecte le **lien** de l'exercice de chaque étudiant pour une session ;
- organise une **relecture par un pair tiré au sort**, notée sur 20 et commentée ;
- donne au formateur un **tableau par promotion** : présences, exercices déposés, moyenne reçue, relectures en retard.

Valeur attendue : fiabiliser l'assiduité, faire pratiquer la revue de code entre pairs et donner au formateur une vue lui permettant d'agir sans ressaisir.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session et obtenir son code · ajouter une présence à la main · voir les exercices d'une session et leur statut · clôturer une session · consulter le tableau d'une promotion | Marquer une présence « ETUDIANT » à la place de l'étudiant · noter un exercice (il voit en revanche le nom du relecteur, [HYP-9]) |
| **Étudiant** | Choisir son nom dans une liste (Q1) · marquer sa présence avec un code · déposer puis remplacer le lien de son exercice · consulter la note et le commentaire reçus, sans le nom du relecteur (Q8) | Relire son propre exercice (Q5) · marquer sa présence après expiration du code (Q2, Q3) · déposer deux exercices pour la même session |
| **Relecteur** | *Ce n'est pas un acteur distinct* : c'est un **Étudiant** à qui le système a assigné une relecture. Il voit les relectures qui lui sont assignées et rend une note entière de 0 à 20 avec un commentaire | Choisir l'exercice qu'il relit (Q7) · modifier une relecture rendue (Q15, voir §7) · relire après clôture |
| **Système** | Générer le code · faire expirer le code · tirer le relecteur au sort · bloquer après 5 codes erronés · calculer la moyenne | — |

**Décision de modélisation :** le relecteur est un étudiant dans un état donné. Il n'y a pas de table `relecteur` : la table `relecture`
porte une clé étrangère `relecteur_id` vers `etudiant`. Conséquence : un même étudiant est à la fois auteur et relecteur au sein d'une même session.

## 3. Périmètre

**Inclus dans cette version :**

- sessions de cours : ouverture avec code, clôture ;
- présence : par code (source `ETUDIANT`), manuelle (source `FORMATEUR`), blocage anti-devinette ;
- exercices : dépôt, remplacement du lien, statut ;
- relecture : assignation aléatoire, notation, consultation anonymisée par l'auteur ;
- tableau récapitulatif par promotion ;
- données de démonstration (1 formateur implicite, 2 promotions, ~12 étudiants) chargées au démarrage ;
- un frontend avec trois écrans : Formateur, Étudiant, Relecteur (F2).

**Explicitement exclu :**

- authentification et mots de passe (Q1) : on choisit son nom dans une liste ;
- gestion (CRUD) des promotions, des étudiants et des formateurs : ces données sont fournies par les données de démonstration ;
- plusieurs formateurs et droits différenciés ;
- notifications (e-mail, SMS, push) ;
- export (PDF, Excel) du tableau ;
- réassignation manuelle d'un relecteur par le formateur *(candidat pour une version ultérieure)* ;
- contestation d'une note, double relecture (Q6) ;
- application mobile native : le web responsive suffit (ENF1) ;
- rendu graphique soigné : le CSS n'est pas noté (SUJET).

## 4. Exigences fonctionnelles

Priorité MoSCoW. **Must** = requis pour `v0.1`. Le détail de chaque exigence (flux, erreurs, endpoint) est dans les fiches SF-x de [SPECIFICATIONS_FONCTIONNELLES.md](SPECIFICATIONS_FONCTIONNELLES.md).

| Réf | Exigence | Critère d'acceptation (quand … alors …) | Priorité | RG |
|---|---|---|---|---|
| EF1 | L'utilisateur choisit sa promotion puis son nom dans une liste | Quand j'ouvre l'écran Étudiant et choisis la promotion « P1 », alors je vois la liste des étudiants de P1 et je peux m'identifier sans mot de passe | Must | RG19 |
| EF2 | Le formateur ouvre une session et obtient un code | Quand j'envoie `{titre, promotionId}` valides, alors je reçois `201` avec un `code` et un `expirationAt` égal à `ouvertureAt` + 15 min | Must | RG1 |
| EF3 | L'étudiant marque sa présence avec le code | Quand je saisis un code valide non expiré, alors je reçois `201` avec `source = ETUDIANT` et ma présence est comptée dans le tableau | Must | RG1, RG2, RG3, RG19 |
| EF4 | Les tentatives de code erronées sont limitées | Quand je saisis 5 codes inconnus d'affilée, alors la 6ᵉ tentative dans les 2 minutes est refusée avec `429 TROP_DE_TENTATIVES`, même si le code est bon | Should | RG4 |
| EF5 | Le formateur ajoute une présence à la main | Quand j'ajoute la présence d'un étudiant, alors elle apparaît avec `source = FORMATEUR`, même après expiration du code | Should | RG3, RG15 |
| EF6 | L'étudiant dépose le lien de son exercice | Quand je dépose une URL http(s) valide pour une session non clôturée, alors je reçois `201` avec un `statut` | Must | RG12, RG13, RG17 |
| EF7 | Le système assigne un relecteur au hasard | Quand un exercice est déposé et qu'au moins un autre étudiant est présent à la session, alors une relecture est créée pour un de ces étudiants et l'exercice passe à `EN_ATTENTE_RELECTURE` | Must | RG5, RG6, RG7 |
| EF8 | Le relecteur voit les relectures qui lui sont assignées | Quand j'ouvre l'écran Relecteur, alors je vois pour chaque relecture en attente le lien de l'exercice, sans le nom de l'auteur ([HYP-10]) | Must | RG7 |
| EF9 | Le relecteur rend une note et un commentaire | Quand j'envoie une note entière entre 0 et 20 et un commentaire, alors je reçois `200` et l'exercice passe à `RELU` | Must | RG5, RG9, RG10, RG18 |
| EF10 | Le formateur consulte le tableau d'une promotion | Quand je demande le tableau de P1, alors je reçois une ligne par étudiant : présences, exercices déposés, moyenne reçue (`null` s'il n'a aucune note), relectures en attente | Must | RG11, RG16 |
| EF11 | Le formateur voit les exercices d'une session et leur statut | Quand un exercice n'a pas été relu, alors il apparaît avec le statut « en attente » dans la vue de la session | Should | RG11 |
| EF12 | Le formateur clôture une session | Quand je clôture une session, alors plus aucun dépôt, remplacement, relecture ni présence n'est accepté pour cette session (`409 SESSION_CLOTUREE`) | Should | RG12, RG18 |
| EF13 | L'étudiant remplace le lien de son exercice | Quand mon exercice n'a pas encore été relu, alors je peux remplacer le lien ; une fois relu, je reçois `409 EXERCICE_DEJA_RELU` | Should | RG14 |
| EF14 | L'étudiant consulte la note reçue | Quand mon exercice est relu, alors je vois la note et le commentaire, jamais le nom du relecteur | Should | RG8 |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'écran Étudiant (présence, dépôt) est utilisable sur téléphone, à partir de 360 px de large | Chrome DevTools en mode appareil 360×640 : aucun défilement horizontal, champ du code et bouton accessibles |
| ENF2 | Le tableau répond en moins de 2 s pour une promotion de 60 étudiants et 30 sessions | Jeu de données de charge + mesure `curl -w %{time_total}` ; calcul par requêtes agrégées (pas de N+1) |
| ENF3 | Toute erreur renvoie le format `{code, message}` ; jamais de stack trace ni de page d'erreur Spring | Tests d'intégration sur chaque code d'erreur + test d'une URL inconnue et d'un JSON mal formé |
| ENF4 | Le code de présence ne se devine pas raisonnablement en 5 essais | 6 caractères parmi 32 symboles (≈ 10⁹ combinaisons) + RG4 |
| ENF5 | Démarrage chez un tiers en 3 commandes maximum ou `docker compose up`, avec données de démonstration | Procédure du README rejouée depuis un clone vierge |
| ENF6 | Le schéma est versionné et reproductible | Flyway ; `ddl-auto=validate` hors tests |
| ENF7 | Les dates sont échangées en ISO-8601 avec fuseau (UTC) | Test de sérialisation de `ouvertureAt` / `expirationAt` |
| ENF8 | Aucun secret n'est commité : toutes les informations sensibles (jetons GitHub et Sonar, mots de passe de base) vivent dans `.env` à la racine, local et ignoré | `git check-ignore .env` ; `.env.example` versionné avec des valeurs factices ; revue de `git log -p` avant chaque jalon |
| ENF9 | Maintenabilité : toute règle RGx est couverte par au moins un test qui la cite | Matrice de traçabilité (SPECIFICATIONS §6) |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session (`expirationAt = ouvertureAt + 15 min`) | Q2 |
| RG2 | Un étudiant ne peut marquer lui-même sa présence que tant que le code est valide. Après expiration, seule la présence manuelle du formateur est possible | Q2, Q3, Q14 |
| RG3 | Un étudiant a au plus une présence par session, quelle que soit la source → `409 DEJA_PRESENT` | CONTRAT, Q14 |
| RG4 | Après 5 codes inconnus consécutifs, l'étudiant est bloqué 2 minutes → `429 TROP_DE_TENTATIVES`. Une tentative réussie remet le compteur à zéro | Q4, [HYP-5] |
| RG5 | Un étudiant ne relit jamais son propre exercice → `403 AUTO_RELECTURE` | Q5, CONTRAT |
| RG6 | Un exercice a au plus un relecteur | Q6 |
| RG7 | Le relecteur est tiré au hasard par le système parmi les étudiants **présents à la session de l'exercice**, auteur exclu | Q7, [HYP-3] |
| RG8 | L'auteur voit la note et le commentaire, jamais l'identité du relecteur | Q8 |
| RG9 | Une note est un entier de 0 à 20 inclus → sinon `400 NOTE_INVALIDE` | Q9, CONTRAT |
| RG10 | Une relecture rendue est définitive : un deuxième envoi renvoie `409 RELECTURE_DEJA_RENDUE` | Q15, CONTRAT (contradiction avec Q10 tranchée en §7) |
| RG11 | Un exercice non relu garde le statut « en attente » et reste visible par le formateur | Q11 |
| RG12 | Le dépôt d'un exercice est possible, y compris après l'expiration du code, jusqu'à la clôture de la session | Q12 |
| RG13 | Un étudiant dépose au plus un exercice par session → `409 EXERCICE_DEJA_DEPOSE` | CONTRAT |
| RG14 | Le lien d'un exercice peut être remplacé tant que l'exercice n'est pas `RELU` et que la session n'est pas clôturée | Q13, [HYP-4] |
| RG15 | Une présence ajoutée par le formateur porte `source = FORMATEUR` ; elle est possible jusqu'à la clôture | Q14, CONTRAT |
| RG16 | La moyenne d'un étudiant = moyenne arithmétique des notes reçues sur les exercices relus de sa promotion, arrondie à 2 décimales, `null` s'il n'a aucune note. Elle est calculée par l'API uniquement | Q16, F3 |
| RG17 | Un lien d'exercice est une URL absolue `http` ou `https` → sinon `400 LIEN_INVALIDE` | CONTRAT |
| RG18 | Une session clôturée n'accepte plus aucune écriture (présence, dépôt, remplacement, relecture) → `409 SESSION_CLOTUREE` | Q10, Q12, [HYP-1] |
| RG19 | Un étudiant ne peut agir que sur les sessions de sa promotion → `400 ETUDIANT_HORS_PROMOTION` | [HYP-7] |
| RG20 | Le code est unique parmi les sessions dont le code n'a pas expiré | [HYP-6] |

## 7. Zones d'ombre, hypothèses et contradictions

### 7.1 Contradictions relevées

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** (« le relecteur peut corriger sa note tant que la session n'est pas clôturée ») ↔ **Q15** (« une fois validée, c'est fini, il ne peut plus y revenir ») | **Q15** : relecture définitive (RG10) | 1) Le contrat imposé, qui fait foi (B2), prévoit `409 RELECTURE_DEJA_RENDUE` sur `POST /api/relectures/{id}` : un second envoi est une erreur, il n'y a donc pas de correction. 2) Q15 est la réponse la plus explicite et motivée (« plus honnête pour tout le monde »). 3) Une note modifiable après consultation par l'auteur ouvre la porte aux pressions entre pairs. **Conséquence :** aucune opération de modification de relecture. Si le client revient sur ce point, il suffira d'ajouter `PUT /api/relectures/{id}` et de lever RG10 |

### 7.2 Trous et points que la demande ne tranche pas

| Id | Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|---|
| HYP-1 | **« Fin de session » et « clôture » ne sont définies nulle part** (Q3, Q10, Q12), et **aucune opération de clôture** n'existe dans la demande ni dans le contrat | *Trou identifié* | La session a deux états, `OUVERTE` puis `CLOTUREE`. La « fin » au sens de Q3 correspond à l'expiration du code (RG2). La clôture est une action explicite du formateur : `POST /api/sessions/{id}/cloture` | Nouvelle opération dans le contrat ; RG18 |
| HYP-2 | **Identité de l'appelant sur `POST /api/relectures/{id}`** : le corps imposé `{note, commentaire}` ne dit pas qui rend la relecture, alors que `403 AUTO_RELECTURE` suppose de connaître l'appelant | *Trou identifié dans le contrat* | L'identité choisie à l'écran (Q1) est transmise dans l'en-tête `X-Etudiant-Id`. `403 AUTO_RELECTURE` si cet étudiant est l'auteur de l'exercice ; `403 RELECTEUR_NON_ASSIGNE` s'il n'est pas le relecteur assigné. Le corps imposé reste inchangé | En-tête documenté dans le contrat. **À signaler :** sans authentification (Q1), cette identité est déclarative et ne protège pas contre la triche |
| HYP-3 | **Quand le relecteur est-il tiré ?** Et que faire si personne d'autre n'est présent ? (Q7, Q11) | *Non tranché* | Tirage **au dépôt**, parmi les présents de la session auteur exclu. Si aucun candidat : l'exercice reste `DEPOSE` (sans relecteur), il est visible « en attente » (Q11) et le tirage est retenté automatiquement à chaque nouvelle présence enregistrée pour cette session | Les statuts `DEPOSE` → `EN_ATTENTE_RELECTURE` → `RELU` (D4) |
| HYP-4 | **« Tant que personne n'a commencé à le relire » (Q13)** : l'application ne peut pas savoir qu'une lecture a commencé, puisqu'il n'y a pas de brouillon | *Non mesurable tel quel* | « Commencé » est assimilé à « relecture rendue ». Le lien est remplaçable tant que le statut ≠ `RELU` | **Risque signalé :** le relecteur peut lire une version puis noter la suivante. Acceptable en V1 |
| HYP-5 | **Q4 : que compte-t-on comme erreur, et par qui ?** | Q4 | Seul `CODE_INCONNU` compte, par `etudiantId`, sur 5 échecs **consécutifs** ; blocage de 2 min à partir du 5ᵉ échec ; code HTTP `429` ajouté au contrat | Table `tentative_code`. **À signaler :** sans authentification, un étudiant bloqué peut choisir un autre nom (limite connue) |
| HYP-6 | Format et unicité du code | *Non précisé* | 6 caractères parmi `A-Z` et `2-9`, sans `O/0/I/1` ; unique parmi les codes non expirés | RG20, ENF4 |
| HYP-7 | Un étudiant d'une autre promotion peut-il utiliser le code ? | *Non précisé* | Non : `400 ETUDIANT_HORS_PROMOTION`. Le contrat n'autorise que 400/409/410 sur cette opération, d'où l'emploi de 400 | RG19 |
| HYP-8 | Un étudiant **absent** peut-il déposer un exercice ? | Q12 laisse entendre que oui (« pas de connexion le soir même ») | Oui : le dépôt ne dépend pas de la présence. Seul le **relecteur** doit être présent (Q7) | Un absent peut être noté mais ne peut pas relire |
| HYP-9 | Le formateur voit-il le nom du relecteur ? | Q8 ne concerne que l'étudiant relu | Oui, dans la vue des exercices d'une session (EF11) : il doit savoir qui est en retard (Q16, « relectures qu'il doit encore faire ») | Seul le DTO « auteur » masque le relecteur |
| HYP-10 | Le relecteur voit-il le nom de l'auteur ? | *Non précisé* | Non : il ne voit que le lien et le titre de la session. C'est symétrique de Q8 et limite les biais | DTO relecteur sans auteur |
| HYP-11 | Q16 demande « sa présence **à chaque session** », alors que le contrat renvoie un simple entier `presences` | Q16 ↔ CONTRAT | Le contrat imposé fait foi : `presences` est un total. Le détail session par session est hors `v0.1` (Could : `GET /api/sessions/{id}/presences`) | Écart signalé au client |
| HYP-12 | Plusieurs formateurs ? Qui est le formateur ? | Q1 n'en parle pas | Un seul formateur implicite, sans identification | Pas de table `formateur` |
| HYP-13 | Plusieurs sessions ouvertes en même temps pour une promotion ? | *Non précisé* | Autorisé ; le code identifie la session | RG20 |

### 7.3 Questions du client peu utiles au développement

- **Q1** n'a qu'une conséquence négative (pas d'authentification) : elle réduit le périmètre.
- Toutes les autres réponses ont servi : chaque RG ci-dessus cite sa source.

## 8. Contraintes techniques

**Imposées par le sujet :**

| # | Contrainte |
|---|---|
| B1 | Java 17+, Maven, wrapper `mvnw` commité |
| B2 | `api/contrat.yaml` respecté à la lettre (chemins, verbes, codes, format d'erreur) |
| B3 | Couches contrôleur / service / repository ; aucune entité JPA exposée, uniquement des DTO |
| B4 | Validation des entrées (`jakarta.validation`) et erreurs centralisées (`@RestControllerAdvice`) |
| B5 | Schéma versionné par **Flyway** ; `ddl-auto=validate` (`create-drop` toléré en test uniquement) |
| B6 | Un test unitaire sur une règle métier réelle et un test d'intégration sur un endpoint, sans base locale |
| F1 | Framework déclaré et justifié dans le README ; `ng build` passe |
| F2 | Trois écrans : Formateur, Étudiant, Relecteur |
| F3 | Appels API dans une couche dédiée ; états de chargement et d'erreur ; aucune règle métier dupliquée |

**Choix complémentaires :**

- Spring Boot 3.x, Spring Data JPA, springdoc-openapi (comparaison visuelle avec le contrat) ;
- **PostgreSQL 16** en exécution (Docker), **H2 en mode PostgreSQL** pour les tests (pour tourner sur un poste vierge, B6) ;
- migrations `backend/src/main/resources/db/migration/V{n}__{description}.sql`, jamais modifiées une fois poussées ;
- Angular 17 en composants standalone, structure `core/ features/ shared/ layout/` ;
- `docker-compose.yml` à la racine : postgres, backend, frontend.

## 9. Livrables

| Livrable | Emplacement |
|---|---|
| Cahier des charges (ce document) | `docs/CAHIER_DES_CHARGES.md` |
| Spécifications fonctionnelles, flows et user stories | `docs/SPECIFICATIONS_FONCTIONNELLES.md` |
| Diagrammes D1 à D4 (Mermaid) | `docs/diagrammes/` |
| Plan technique (stack justifiée, qualité Sonar, tests), conventions | `docs/PLAN_TECHNIQUE_EQUIPES.md`, `docs/CONTRIBUTING.md` |
| Planning horodaté par sprint et par équipe | `docs/PLANNING_SPRINTS.md` |
| Coordination et communication entre équipes | `docs/COORDINATION_EQUIPES.md` |
| Système de design et templates visuels | `docs/design/` |
| Backlog | Issues GitHub (source : `docs/BACKLOG.md`) |
| Contrat d'API complété | `api/contrat.yaml` |
| Backend Spring Boot + migrations + tests | `backend/` |
| Frontend Angular | `frontend/` |
| Démarrage | `docker-compose.yml`, `README.md` |
| Historique | `CHANGELOG.md`, `docs/JOURNAL.md` |

## 10. Démarche prévue

| Étape | Objectif | Jalon |
|---|---|---|
| 0 | Dépôt public ; premier commit = `.gitignore` Java + JS | — |
| 1 | Ce cahier, spécifications, diagrammes, contrat figé, issues | `[JALON] analyse` |
| 2 | Stories **Must** uniquement : une branche par issue, une PR par branche | `[JALON] v0.1` |
| 3 | Enveloppe : une issue « bug » et une issue « évolution » séparées ; mise à jour de l'analyse **avant** le code | — |
| 4 | Stories Should restantes, README testé sur clone vierge, CHANGELOG | `[JALON] v1.0` |
| 5 | Épreuve git-lab (dépôt séparé) | — |
| 6 | Soumission | — |

Le détail heure par heure (25/09, sprints S0 à S6, tâche par équipe et heure de livraison) est dans [PLANNING_SPRINTS.md](PLANNING_SPRINTS.md).

**En cas de retard :** les stories Should/Could sont repoussées (label `prio`), jamais la qualité d'une story Must, ses tests ou le journal.

**Definition of Done — un ticket est terminé quand :**

- ses critères d'acceptation sont vérifiés par un test automatisé ou une preuve jointe à la PR (curl/Swagger ou capture) ;
- les RG qu'il cite sont couvertes par un test dont le nom cite la RG ;
- le contrat, les migrations et la documentation touchés sont à jour dans la même PR ;
- la CI est verte et la PR est fusionnée dans `main` avec `Closes #n` ;
- aucune erreur ne renvoie autre chose que `{code, message}`.

---

## Annexe A — Glossaire

| Terme | Définition |
|---|---|
| Promotion | Groupe d'étudiants suivant la formation ensemble |
| Session | Une séance de cours d'une promotion, ouverte par le formateur |
| Code de présence | Chaîne courte, valide 15 min, prouvant qu'on est dans la salle |
| Présence | Fait qu'un étudiant a assisté à une session ; `source` indique qui l'a enregistrée |
| Exercice | Lien (URL) vers le travail d'un étudiant pour une session |
| Relecture | Évaluation d'un exercice par un pair : note entière /20 et commentaire |
| Relecteur | Étudiant assigné à une relecture |
| Clôture | Action du formateur qui fige une session (plus aucune écriture) |
| Tableau | Récapitulatif par étudiant d'une promotion |

## Annexe B — Dictionnaire de données

Correspond à la migration `V1__init.sql` et au diagramme [D2](diagrammes/D2-modele-donnees.md).

| Table | Colonne | Type | Contraintes | Description |
|---|---|---|---|---|
| promotion | id | BIGINT | PK | |
| | nom | VARCHAR(100) | NOT NULL, UNIQUE | ex. « P1-2026 » |
| etudiant | id | BIGINT | PK | |
| | nom | VARCHAR(150) | NOT NULL | Nom affiché dans la liste (Q1) |
| | promotion_id | BIGINT | FK → promotion, NOT NULL | |
| session | id | BIGINT | PK | |
| | titre | VARCHAR(200) | NOT NULL | |
| | promotion_id | BIGINT | FK → promotion, NOT NULL | |
| | code | VARCHAR(6) | NOT NULL | RG20 |
| | ouverture_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| | expiration_at | TIMESTAMP WITH TIME ZONE | NOT NULL | = ouverture_at + 15 min (RG1) |
| | statut | VARCHAR(20) | NOT NULL, `OUVERTE`/`CLOTUREE` | HYP-1 |
| | cloture_at | TIMESTAMP WITH TIME ZONE | NULL | |
| presence | id | BIGINT | PK | |
| | session_id | BIGINT | FK → session | |
| | etudiant_id | BIGINT | FK → etudiant | |
| | source | VARCHAR(10) | `ETUDIANT`/`FORMATEUR` | Q14 |
| | marquee_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| | | | UNIQUE (session_id, etudiant_id) | RG3 |
| tentative_code | etudiant_id | BIGINT | PK, FK → etudiant | RG4 |
| | echecs_consecutifs | INT | NOT NULL, défaut 0 | |
| | bloque_jusqu_a | TIMESTAMP WITH TIME ZONE | NULL | |
| exercice | id | BIGINT | PK | |
| | session_id | BIGINT | FK → session | |
| | auteur_id | BIGINT | FK → etudiant | |
| | lien | VARCHAR(2048) | NOT NULL | RG17 |
| | statut | VARCHAR(25) | `DEPOSE`/`EN_ATTENTE_RELECTURE`/`RELU` | D4 |
| | depose_at, modifie_at | TIMESTAMP WITH TIME ZONE | | |
| | | | UNIQUE (session_id, auteur_id) | RG13 |
| relecture | id | BIGINT | PK | |
| | exercice_id | BIGINT | FK → exercice, UNIQUE | RG6 |
| | relecteur_id | BIGINT | FK → etudiant, NOT NULL | RG7 ; ≠ auteur vérifié par le service (RG5) |
| | note | INT | NULL, CHECK 0–20 | RG9 ; NULL = pas encore rendue |
| | commentaire | TEXT | NULL | |
| | assignee_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| | rendue_at | TIMESTAMP WITH TIME ZONE | NULL | non NULL = définitive (RG10) |

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 2026-09-25 | Version initiale (étape 1) |
