# Backlog — source des issues GitHub

Chaque ligne est une issue GitHub : `Bxx · #n` donne l'identifiant de backlog et le numéro réel de l'issue (créées le 25/09 à 13h15). Les critères d'acceptation détaillés, au format Gherkin, sont dans [SPECIFICATIONS_FONCTIONNELLES.md §4](SPECIFICATIONS_FONCTIONNELLES.md#4-user-stories). Le corps de chaque issue suit `.github/ISSUE_TEMPLATE/`.

Priorités : **Must** = `priority :: high` (milestone `v0.1`) · **Should** = `priority :: medium` (`v1.0`) · **Could** = `priority :: low`.

## Backlog restant après v1.0 — trié (25/09, 19h30)

Ordre de reprise, du plus utile au correcteur et au client au moins utile. Les tickets livrés sont fermés sur GitHub.

| Rang | Issue | Pourquoi à ce rang |
|---|---|---|
| 1 | #30 Présence ajoutée par le formateur | Demandée explicitement (Q14) ; seul scénario de recette non couvert (R3) |
| 2 | #32 Clôture de session | Fige la session (RG18) ; aujourd'hui seule la session de démonstration est clôturée |
| 3 | #59 Connexion et menus par rôle (frontend) | L'API v2 est prête et testée ; seuls les écrans manquent |
| 4 | #31 Exercices en attente d'une session | Visibilité du formateur sur RG11 (Q11) |
| 5 | #33 Remplacement du lien avant relecture | Confort (Q13, RG14) |
| 6 | #35 CI : lint du contrat | Backend et frontend déjà vérifiés en CI ; reste `redocly lint` |
| 7 | #36 Collection Bruno | Les codes d'erreur sont déjà couverts par les tests d'intégration |
| 8 | #62, #61, #60 CRUD sessions, promotions/étudiants, comptes | Sacrifiés pour la double relecture (cahier §7.2 ter) ; les données de démonstration suffisent |
| 9 | #63 Pièce jointe | Sacrifiée (§7.2 ter) ; demandera une migration **V5** |
| 10 | #37, #38 | Could, hors engagement |

## Cadre et analyse (étape 1)

| Id | Titre | Équipe | Type | Prio | Réf. | Critères d'acceptation |
|---|---|---|---|---|---|---|
| B01 · #1 | [KICKOFF] Cadre de travail commun — branches, commits, PR et preuves | analyse | documentation | high | CONTRIBUTING | Le ticket reprend CONTRIBUTING.md ; il est épinglé ; chaque ticket y renvoie |
| B02 · #2 | [DevOps] Le dépôt ignore les fichiers générés et les secrets | devops | chore | high | ENF8 | `.gitignore` Java + JS présent avant tout code ; `git status` n'affiche ni target/ ni node_modules/ |
| B03 · #3 | [Analyse] Le cahier des charges décrit le besoin et tranche les contradictions | analyse | documentation | high | SUJET 1a | 10 sections ; EF et RG numérotées ; Q10/Q15 tranchée ; trous HYP-1 à HYP-13 documentés |
| B04 · #4 | [Analyse] Les spécifications détaillent chaque fonctionnalité, ses flows et ses user stories | analyse | documentation | high | — | 14 fiches SF ; flows ; US en Gherkin ; matrice de traçabilité |
| B05 · #5 | [Analyse] Les diagrammes D1 à D4 décrivent acteurs, données, présence et cycle d'un exercice | analyse | documentation | high | SUJET 1b | Mermaid ; D2 = V1 ; D3 = codes du contrat |
| B06 · #6 | [Analyse] Le contrat d'API couvre toutes les opérations des écrans | analyse | documentation | high | SUJET 1d | 5 opérations imposées inchangées ; ajouts justifiés ; lint OK |
| B07 · #7 | [Analyse] Les conventions d'équipe et le plan technique sont écrits | analyse | documentation | medium | — | CONTRIBUTING, PLAN_TECHNIQUE_EQUIPES (stack justifiée, Sonar, tests), PLANNING_SPRINTS, COORDINATION_EQUIPES, modèles d'issue et de PR |
| B37 · #8 | [Maquette] Le système de design (shadcn via spartan/ui) et sa stack sont définis | design | documentation | high | ENF1 | tokens clair/sombre, composants et états, accessibilité, stack justifiée |
| B38 · #9 | [Maquette] Les templates visuels des écrans Formateur, Étudiant, Relecteur et Tableau sont disponibles | design | documentation | high | F2, ENF1 | 4 templates HTML ; états chargement / erreur / vide ; lisibles à 360 px et 1280 px |

## v0.1 — stories Must (étape 2)

| Id | Titre | Équipe | Type | Prio | Réf. | Critères d'acceptation (résumé) |
|---|---|---|---|---|---|---|
| B08 · #10 | [DevOps] Le backend démarre avec PostgreSQL, Flyway et un schéma V1 conforme à D2 | devops/back-end | chore | high | B1, B5 | `./mvnw verify` vert sur un poste vierge ; `V1__init.sql` = D2 ; `ddl-auto=validate` |
| B09 · #11 | [Backend] Toute erreur renvoie `{code, message}`, jamais de stack trace | back-end | feature | high | B4, ENF3 | route inconnue → 404 RESSOURCE_INTROUVABLE ; JSON invalide → 400 ; exception inattendue → 500 ERREUR_INTERNE |
| B10 · #12 | [Backend] Des données de démonstration sont chargées au démarrage | back-end | feature | high | ENF5 | 2 promotions, ≥ 12 étudiants, une session clôturée notée, une session ouverte |
| B11 · #13 | [DevOps] Le frontend Angular est initialisé avec sa couche API dédiée | devops/front-end | chore | high | F1, F3 | script corrigé ; `npm run build` vert ; `core/api` + intercepteur d'erreurs |
| B12 · #14 | [Backend] L'étudiant retrouve sa promotion et son nom dans une liste | back-end | feature | high | EF1, RG19 | US-01 |
| B13 · #15 | [Frontend] L'étudiant s'identifie en choisissant promotion et nom | front-end | feature | high | EF1 | US-01 ; identité mémorisée |
| B14 · #16 | [Backend] Le formateur ouvre une session et obtient un code valable 15 min | back-end | feature | high | EF2, RG1, RG20 | US-02 |
| B15 · #17 | [Frontend] Le formateur ouvre une session et voit le code et son expiration | front-end | feature | high | EF2, F2 | code affiché ; erreur 400 affichée |
| B16 · #18 | [Backend] L'étudiant marque sa présence avec un code valide | back-end | feature | high | EF3, RG1, RG2, RG3, RG19 | US-03 : 201 / 400 / 409 / 410 ; **test d'intégration B6** |
| B17 · #19 | [Frontend] L'étudiant saisit le code depuis son téléphone | front-end | feature | high | EF3, ENF1 | 360 px ; message pour chaque code d'erreur |
| B18 · #20 | [Backend] L'étudiant dépose le lien de son exercice | back-end | feature | high | EF6, RG12, RG13, RG17 | US-06 |
| B19 · #21 | [Backend] Un relecteur présent, autre que l'auteur, est tiré au sort | back-end | feature | high | EF7, RG5, RG6, RG7 | US-07 ; **test unitaire B6 sur RG5/RG7** |
| B20 · #22 | [Frontend] L'étudiant dépose son exercice et voit son statut | front-end | feature | high | EF6, F2 | statut affiché ; erreurs 400/409 |
| B21 · #23 | [Backend] Le relecteur voit ses relectures et rend une note définitive | back-end | feature | high | EF8, EF9, RG5, RG9, RG10 | US-08, US-09 |
| B22 · #24 | [Frontend] Le relecteur note un exercice depuis son écran | front-end | feature | high | EF8, EF9, F2 | confirmation « définitif » ; état vide |
| B23 · #25 | [Backend] Le formateur obtient le tableau de sa promotion | back-end | feature | high | EF10, RG11, RG16 | US-10 ; < 2 s pour 60 étudiants |
| B24 · #26 | [Frontend] Le formateur consulte le tableau sans recalcul côté client | front-end | feature | high | EF10, F3 | moyenne `null` affichée « — » |
| B25 · #27 | [DevOps] L'application démarre en une commande avec des données de démonstration | devops | chore | high | ENF5 | `docker compose up` depuis un clone vierge ; README testé |
| B26 · #28 | [QA] Recette v0.1 : scénarios R1 à R7 rejoués et prouvés | qa | chore | high | — | rapport dans la PR du jalon |
| B39 · #48 | [DevOps] L'analyse SonarQube est branchée à l'éditeur et au cycle de ticket | devops | chore | high | ENF9 | projet créé ; binding `.vscode/settings.json` ; DoD = Quality Gate TEFO CBS |

## Changement de besoin v2 — sécurité, rôles, CRUD, pièce jointe (25/09, #54)

| Id | Titre | Équipe | Type | Prio | Réf. |
|---|---|---|---|---|---|
| B40 · #54 | [Evolution] Authentification, rôles, CRUD par profil et pièce jointe d'exercice (issue mère, analyse d'impact) | analyse | evolution | high | EF15–EF26 |
| B41 · #55 | [Analyse] Cahier, spécifications, diagrammes et contrat v2 intègrent la sécurité | analyse | documentation | high | — |
| B42 · #56 | [Maquette] Templates connexion, profil, administration des utilisateurs | design | documentation | high | EF15–EF21 |
| B43 · #57 | [Backend] Connexion, déconnexion, profil, changement de mot de passe, admin par défaut | back-end | feature | high | EF15–EF18, RG23, RG24 |
| B44 · #58 | [Backend] Chaque rôle n'accède qu'à ce que son rang autorise | back-end | feature | high | EF19, EF20, RG22, RG25, RG26 |
| B45 · #59 | [Frontend] Connexion, gardes de routes, menus par rôle | front-end | feature | high | EF15, EF16, EF19 |
| B46 · #60 | [Backend+Frontend] L'administrateur gère les comptes | back/front | feature | medium | EF21, RG27, RG28 |
| B47 · #61 | [Backend+Frontend] Promotions et étudiants gérés dans l'application | back/front | feature | medium | EF22, EF23 |
| B48 · #62 | [Backend+Frontend] Le formateur modifie ou supprime ses sessions | back/front | feature | medium | EF24, RG29 |
| B49 · #63 | [Backend+Frontend] Pièce jointe (fichier ou .zip) d'exercice | back/front | feature | medium | EF25, RG30 |

Re-priorisation écrite : voir [PLANNING_SPRINTS.md](PLANNING_SPRINTS.md#re-priorisation-du-2509-à-14h45--changement-de-besoin-54-sécurité). Audit : retiré par le PO.

## v1.0 — stories Should (étape 4)

| Id | Titre | Équipe | Prio | Réf. |
|---|---|---|---|---|
| B27 · #29 | [Backend] Blocage de 2 min après 5 codes erronés | back-end | medium | EF4, RG4 |
| B28 · #30 | [Backend+Frontend] Le formateur ajoute une présence marquée « ajoutée par le formateur » | back/front | medium | EF5, RG15 |
| B29 · #31 | [Backend+Frontend] Le formateur voit les exercices en attente d'une session | back/front | medium | EF11, RG11 |
| B30 · #32 | [Backend+Frontend] Le formateur clôture une session | back/front | medium | EF12, RG18 |
| B31 · #33 | [Backend+Frontend] L'étudiant remplace son lien tant qu'il n'est pas relu | back/front | medium | EF13, RG14 |
| B32 · #34 | [Backend+Frontend] L'étudiant consulte sa note sans connaître son relecteur | back/front | medium | EF14, RG8 |
| B33 · #35 | [DevOps] La CI GitHub Actions vérifie backend, frontend et contrat sur chaque PR | devops | medium | — |
| B34 · #36 | [QA] Collection Bruno couvrant chaque code d'erreur du catalogue | qa | medium | ENF3 |

## Could (hors engagement)

| Id | Titre | Réf. |
|---|---|---|
| B35 · #37 | Détail des présences session par session dans le tableau | HYP-11 |
| B36 · #38 | Réassignation manuelle d'un relecteur par le formateur | Exclu §3, à valider avec le client |

Une fois l'enveloppe ouverte (étape 3), ses tickets sont ajoutés ici avec le modèle `evolution.md` ou `bug.md`, et ce backlog est **re-priorisé par écrit**.
