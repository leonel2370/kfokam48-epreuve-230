# Contribuer à PRESENCE48 — branches, commits, pull requests et preuves

Ce fichier est le **cadre de travail commun** (repris dans le ticket épinglé `[KICKOFF]`). Il s'applique à tout le monde, même à une personne seule. Les raisons de ces choix sont dans [PLAN_TECHNIQUE_EQUIPES.md](PLAN_TECHNIQUE_EQUIPES.md).

> **Ne commencez rien sans ticket.** Si le travail n'a pas d'issue, créez-la d'abord. En cas de divergence entre un ticket et le [cahier des charges](CAHIER_DES_CHARGES.md), **le cahier fait foi** : commentez le ticket, ne tranchez pas seul.

## 1. Branches

`main` est la seule branche longue. Elle est protégée : pas de commit direct, pas de force-push. Toutes les autres branches sont courtes : créées pour un ticket, fusionnées par PR, puis supprimées.

**Nommage :** `<type>/gh-<numéro-issue>-<description-kebab-case>` (minuscules, sans accent, sans espace ni `_`).

| Préfixe | Usage | Part de | Cible | Exemple |
|---|---|---|---|---|
| `feature/` | Nouvelle fonctionnalité prévue | `main` | `main` | `feature/gh-12-marquer-presence` |
| `bugfix/` | Bug trouvé en développement, livré dans la prochaine version | `main` | `main` | `bugfix/gh-31-code-expire-fuseau` |
| `docs/` | Documentation uniquement (cahier, diagrammes, README, contrat seul) | `main` | `main` | `docs/gh-40-maj-cdc-enveloppe` |
| `chore/` | Dépendances, CI, configuration, outillage | `main` | `main` | `chore/gh-5-gitignore-ci` |
| `refactor/` | Restructuration **sans changement de comportement** (sinon c'est feature ou bugfix) | `main` | `main` | `refactor/gh-22-mapper-dto` |
| `hotfix-X.Y/` | Bug qui bloque une version en stabilisation | `release-X.Y` | `release-X.Y`, puis `cherry-pick -x` vers `main` | `hotfix-0.1/gh-45-note-null` |

Règles :

- une branche = un ticket ; pas de branche fourre-tout ;
- partir d'un `main` à jour : `git switch main && git pull --rebase && git switch -c feature/gh-12-marquer-presence` ;
- avant la PR, rattraper `main` par `git rebase main` sur **sa propre branche** seulement (`git push --force-with-lease` autorisé sur sa branche, jamais sur `main`).

## 2. Commits

Format **Conventional Commits**, description en français, à l'impératif, sans majuscule initiale ni point final :

```text
<type>(<scope>): <description>

<corps facultatif : le POURQUOI, pas le comment ; cite les RGx>

Refs: #12
```

- **Types :** `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`, `perf`, `build`, `ci`.
- **Scopes :** `session`, `presence`, `exercice`, `relecture`, `tableau`, `identite`, `common`, `api` (contrat), `db` (migrations), `ui`, `ci`, `cdc` (cahier), `diagrammes`.
- **Le pied `Refs: #n` est obligatoire** sur chaque commit. `Closes #n` va dans la description de la PR.
- Un commit = un changement cohérent qui compile. Interdits : « wip », « fix », « update » seuls ; plusieurs tickets dans un commit.
- Jalons de l'épreuve, **messages exacts**, commits vides sur `main` : `[JALON] analyse`, `[JALON] v0.1`, `[JALON] v1.0` — trois et seulement trois (SUJET §2) ; aucun autre commit ne commence par `[JALON]`.

Exemple :

```text
feat(presence): refuser un code expiré avec 410

Le code expire 15 minutes après l'ouverture (RG1). L'expiration est
vérifiée avant le doublon pour que l'étudiant sache qu'il doit
s'adresser au formateur (SF-3).

Refs: #12
```

## 3. Pull requests

**Titre :** `[#12] Marquer sa présence avec le code` (numéro de l'issue, puis le comportement obtenu).
**Cible :** `main` (ou `release-X.Y` pour un hotfix). **Description :** le modèle `.github/pull_request_template.md`, rempli entièrement.

Une PR est refusée si :

- elle ne référence aucune issue (`Closes #n`) ;
- elle ne contient pas de **preuves** : réponse curl/Bruno ou Swagger **avant et après** pour le backend, capture d'écran pour le frontend ;
- la CI est rouge ;
- elle modifie le comportement sans mettre à jour le contrat, la migration ou la documentation concernés ;
- elle modifie une migration Flyway déjà fusionnée.

**Fusion :** *merge commit* (`--no-ff`), pour garder les commits atomiques lisibles dans l'historique. Supprimer la branche ensuite.

## 4. Definition of Done

Un ticket est terminé seulement si **tous** ces points sont vrais :

- [ ] Les critères d'acceptation sont vérifiés par un test automatisé ou une preuve jointe à la PR.
- [ ] Chaque RG citée par le ticket a un test dont le nom la cite (`RG5_autoRelecture_refusee`).
- [ ] Aucune règle de gestion du cahier (§6) n'est enfreinte ; aucune erreur autre que `{code, message}`.
- [ ] Contrat, migration, diagrammes et cahier sont à jour si concernés.
- [ ] Aucune régression : suite de tests verte et parcours voisins vérifiés.
- [ ] La PR contient les preuves ; l'issue est fermée par la fusion.

## 5. Tickets

Utiliser les modèles `.github/ISSUE_TEMPLATE/`. Titre : `[Backend] …`, `[Backend-Bug] …`, `[Frontend] …`, `[Maquette] …`, `[DevOps] …`, `[QA] …`, `[Analyse] …`, suivi du **résultat** attendu, formulé pour que le client le comprenne.

**Labels :** `priority :: …`, `statut :: …`, `team :: …`, `type :: …` ([PLAN_TECHNIQUE_EQUIPES.md §4](PLAN_TECHNIQUE_EQUIPES.md#4-workflow-de-gestion-github)).

Passer le ticket en `statut :: in progress` au démarrage, puis `statut :: review` à l'ouverture de la PR. Un seul ticket à la fois.

**Bloqué ?** Pour un ticket ambigu, commentez le ticket. Pour une dépendance vers un autre ticket, signalez-la et passez au suivant.

## 6. Spécifique backend

- Aucune erreur technique ne remonte au client : code métier stable et message compréhensible.
- Toute règle vit dans un service ; le contrôleur ne fait que valider, déléguer et mapper.
- Migration : nouvelle version `V{n}__…sql`, testée sur une base vierge (`./mvnw verify`), mentionnée dans la PR.
- Tests d'intégration verts avant l'ouverture de la PR.

## 7. Spécifique frontend

- Tout appel HTTP passe par `core/api`. Aucune règle métier côté client : la moyenne vient de l'API.
- Chaque écran gère les états chargement / erreur / vide.
- `npm run build` vert avant la PR.
