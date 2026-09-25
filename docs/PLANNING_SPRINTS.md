# Planning des sprints — 25/09/2026

**Public :** toutes les équipes. **Échéance non négociable : dépôt de la soumission avant 18h00.**
Numéros d'issue : voir [BACKLOG.md](BACKLOG.md). Règle de travail : un ticket suit le cycle complet (branche → commits → analyse SonarQube → PR → CI → merge → test sur `main`) **avant** que le suivant commence ([CONTRIBUTING.md](CONTRIBUTING.md)).

## Vue d'ensemble

| Sprint | Début | Fin (livraison) | Objectif | Jalon posé |
|---|---|---|---|---|
| S0 Cadrage | 13h05 | 13h20 | Dépôt, labels, milestones, 38 issues | — |
| S1 Analyse & conception | 13h20 | 14h00 | Documentation, contrat figé, design, fusion par PR | **14h00 `[JALON] analyse`** |
| S2a Socle technique | 14h00 | 14h40 | Backend, frontend et CI démarrent | — |
| S2b Stories Must | 14h40 | 16h15 | Parcours complets présence → exercice → relecture → tableau | **16h15 `[JALON] v0.1`** + tag `v0.1.0` |
| S3 Enveloppe | 16h15 | 16h55 | Bug + changement de besoin, analyse mise à jour d'abord | — |
| S4 Version finale | 16h55 | 17h20 | Docker Compose, README testé, CHANGELOG, backlog trié | **17h20 `[JALON] v1.0`** + tag `v1.0.0` |
| S5 Épreuve Git | 17h20 | 17h45 | git-lab (dépôt séparé) | — |
| S6 Soumission | 17h45 | 17h55 | SOUMISSION.md déposé | avant 18h00 |

## Détail par équipe

### S0 — Cadrage (13h05 → 13h20)

| Heure | Équipe | Tâche | Ticket | Livrable |
|---|---|---|---|---|
| 13h05 | DevOps | Dépôt public, `.gitignore`, `.env` / `.env.example` | #2 | `main` poussé |
| 13h10 | Analyse | Labels, milestones, création des issues | #1 | 38 issues |
| 13h20 | Tous | Kickoff : lecture du cadre de travail | #1 | ticket épinglé |

### S1 — Analyse & conception (13h20 → 14h00)

| Heure de livraison | Équipe | Tâche | Ticket |
|---|---|---|---|
| 13h25 | DevOps | PR secrets : `.env.example` | #2 |
| 13h28 | Analyse | PR cadre de travail : CONTRIBUTING + modèles GitHub | #1 |
| 13h32 | Analyse | PR cahier des charges | #3 |
| 13h35 | Analyse | PR spécifications fonctionnelles | #4 |
| 13h38 | Analyse | PR diagrammes D1–D4 | #5 |
| 13h41 | Analyse | PR contrat d'API (figé ensuite) | #6 |
| 13h45 | Analyse | PR plan technique, planning, coordination, backlog | #7 |
| 13h50 | Design | PR système de design + stack | #8 |
| 13h58 | Design | PR templates visuels des 4 écrans | #9 |
| 14h00 | Tous | Revue de jalon → `[JALON] analyse` | — |

### S2a — Socle (14h00 → 14h40)

| Heure | Équipe | Tâche | Ticket |
|---|---|---|---|
| 14h15 | Backend/DevOps | Spring Boot 3.3 + Java 21, Flyway `V1__init.sql` = D2, `application.yml` lisant `.env` | #10 |
| 14h25 | Backend | Gestion centralisée des erreurs `{code, message}` | #11 |
| 14h30 | Backend | Données de démonstration `V2__donnees_demo.sql` | #12 |
| 14h40 | Frontend/DevOps | Angular 17 (script corrigé), Tailwind + tokens design, `core/api`, intercepteur | #13 |
| 13h45 | DevOps | SonarQube connecté : projet, binding VS Code, DoD (Quality Gate TEFO CBS) | #48 |
| 14h40 | DevOps | Workflow CI (build + tests + régression ; Sonar reste local, serveur privé) — livré avec #10 | #10 |

### S2b — Stories Must (14h40 → 16h15)

| Heure | Backend | Frontend | QA |
|---|---|---|---|
| 14h50 | #14 promotions et étudiants | — | — |
| 15h00 | #16 ouvrir une session (RG1, RG20) | #15 identification | — |
| 15h15 | #18 marquer sa présence (**test d'intégration B6**) | #17 ouvrir une session | — |
| 15h30 | #20 déposer un exercice · #21 tirage relecteur (**test unitaire B6**) | #19 saisir le code | — |
| 15h45 | #23 relectures | #22 déposer un exercice | — |
| 15h55 | #25 tableau | #24 écran relecteur | — |
| 16h05 | — | #26 tableau | — |
| 16h10 | — | — | #28 recette R1–R7 |
| 16h15 | **`[JALON] v0.1`**, tag `v0.1.0`, `./enveloppe` | | |

### S3 — Enveloppe (16h15 → 16h55)

Contenu inconnu à ce stade (le script `enveloppe` n'est pas encore dans le dossier). Déroulé fixé d'avance :

| Heure | Équipe | Tâche |
|---|---|---|
| 16h20 | Analyse | Deux issues séparées : `[Backend-Bug]` et `[Evolution]`, avec analyse d'impact et re-priorisation écrite |
| 16h30 | Analyse | PR `docs/` : cahier v2, diagrammes, contrat, journal des révisions |
| 16h40 | QA/Backend | Bug : test de régression rouge → correctif |
| 16h55 | Backend/Frontend | Évolution : migration `V3__…`, API, écran |

### S4 — Version finale (16h55 → 17h20)

| Heure | Équipe | Tâche | Ticket |
|---|---|---|---|
| 17h05 | DevOps | `docker-compose.yml` + README testé depuis un clone vierge | #27 |
| 17h15 | Tous | CHANGELOG, backlog restant trié, journal | — |
| 17h20 | Tous | **`[JALON] v1.0`**, tag `v1.0.0` | — |

### S5 et S6 — Épreuve Git et soumission (17h20 → 17h55)

| Heure | Tâche |
|---|---|
| 17h45 | git-lab : 5 situations, dépôt public `kfokam48-gitlab-230`, `git push origin --all` |
| 17h50 | SOUMISSION.md : les deux URL, hashes complets sur 40 caractères, vérification en navigation privée |
| 17h55 | Téléversement sur la plateforme |

## Re-priorisation du 25/09 à 14h45 — changement de besoin #54 (sécurité)

Le PO ajoute authentification, rôles, CRUD et pièce jointe. Décision écrite :

| Heure | Équipe | Tâche | Ticket |
|---|---|---|---|
| 14h55 → 15h20 | Analyse, Design | Documents v2 (cahier, specs, D1/D2/D5, contrat 2.0), puis templates connexion, profil, admin | #55, #56 |
| 15h20 → 15h40 | Backend | Données de démo (V2) | #12 |
| 15h40 → 16h10 | Backend | Connexion, déconnexion, profil, mot de passe, admin par défaut (V3) | #57 |
| 16h10 → 16h30 | Backend | Contrôle d'accès par rôle, routes imposées publiques | #58 |
| en parallèle | Frontend | Initialisation, connexion, gardes de routes, menus par rôle | #13, #59 |

**Conséquences assumées :**
- `[JALON] v0.1` glisse d'environ **16h15 à 16h45** ; S3 à S6 sont resserrés (enveloppe 30 min, version finale 20 min).
- Les parcours métier Must (#14 à #28) restent visés pour v0.1, **backend d'abord** ; les écrans frontend non terminés à 16h45 passent en v1.0, par une décision écrite dans le backlog.
- Les Should de sécurité (#60 à #63 : CRUD comptes, promotions/étudiants, sessions, pièce jointe) et les Should existants (#29 à #36) ne sont entamés qu'une fois `[JALON] v1.0` sécurisé.

## Règles de dérapage

- À 16h30, si `[JALON] v0.1` n'est pas posé : les tickets frontend restants passent en priorité Could, par une décision écrite dans le backlog, et le jalon est posé avec le backend complet.
- Les stories Should (#29 à #36) ne sont entamées que si S4 est terminé avant 17h15.
- Jamais sacrifiés : tests B6, journal, jalons, soumission.
