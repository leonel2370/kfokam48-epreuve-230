# Présence48 — suivi de présence et relecture par les pairs (KFOKAM48)

Application fullstack : le formateur ouvre une session et affiche un code, les étudiants marquent leur présence avec ce code, déposent le lien de leur exercice, un pair présent tiré au sort le note, et le formateur suit sa promotion dans un tableau.

**Framework frontend (F1) : Angular 17 (standalone)** — choisi car c'est la base fournie par l'épreuve, fortement typée (TypeScript strict) et structurée (core/features), ce qui garde la couche API unique et sans règle métier (F3).
**Backend :** Spring Boot 4.1 (Java 21), Spring Data JPA, Flyway, Spring Security 7, PostgreSQL 16. Choix détaillés : [docs/PLAN_TECHNIQUE_EQUIPES.md](docs/PLAN_TECHNIQUE_EQUIPES.md).

## Démarrer (une commande)

Prérequis : Docker avec Compose v2.

```bash
git clone https://github.com/leonel2370/kfokam48-epreuve-230.git && cd kfokam48-epreuve-230
docker compose up --build
```

Puis ouvrir **http://localhost:4200**. L'API est aussi exposée sur http://localhost:8080/api.

**Documentation de l'API (Swagger UI) : http://localhost:8080/swagger-ui.html**, accessible sans connexion. Le menu en haut à droite propose deux définitions :
- **Contrat (référence)** : `api/contrat.yaml` tel quel, le contrat qui fait foi ;
- **Implémentation (générée)** : ce que le backend expose réellement (`/v3/api-docs`).

Les routes protégées demandent une session : se connecter d'abord avec `POST /api/auth/login` dans la même fenêtre.

- Aucun fichier à créer : sans `.env`, les valeurs de démonstration de `docker-compose.yml` s'appliquent. Pour les changer : `cp .env.example .env`, puis éditer.
- La base PostgreSQL est créée par les migrations Flyway (`V1` schéma, `V2` données de démonstration, `V3` comptes et sécurité).
- Arrêt : `docker compose down` (ajouter `-v` pour repartir d'une base vide de démonstration).

## Données et comptes de démonstration

| Donnée | Contenu |
|---|---|
| Promotions | `P1-2026` (8 étudiants), `P2-2026` (4 étudiants) |
| Session passée | « TP Flyway et migrations » (P1, code `DEMO01`, clôturée) avec présences, exercices, notes et 2 relectures en attente |

| Compte | Mot de passe | Rôle | Remarque |
|---|---|---|---|
| `admin` | `admin` | ADMIN | Changement de mot de passe obligatoire à la première connexion (RG23) |
| `formateur` | `Formateur48` | FORMATEUR | Rattaché à `P1-2026` |
| `awa`, `paul`, `lina` | `Etudiant48` | ETUDIANT | Fiches Awa Ndiaye, Paul Mbarga, Lina Kamga (P1) |

Mots de passe de démonstration uniquement : à changer hors démonstration.

## Parcours de démonstration (écrans imposés F2)

1. **Formateur** (`/formateur`) : choisir `P1-2026`, saisir un titre, **Ouvrir** → le code s'affiche avec son expiration (15 min). Le lien **Voir le tableau** mène au tableau de la promotion.
2. **Étudiant** (`/etudiant`, conçu pour mobile) : choisir `P1-2026` puis `Paul Mbarga`, saisir le code → présence enregistrée. Refaire avec `Hugo Talla`, puis déposer un lien `https://…` : Paul, présent, est tiré au sort comme relecteur (statut `EN_ATTENTE_RELECTURE`).
3. **Relecteur** (`/relecteur`) : se connecter avec `paul` / `Etudiant48`, noter l'exercice, confirmer l'envoi définitif.
4. **Tableau** : présences, exercices, moyenne (calculée par le serveur, « — » sans note) et relectures en attente.

Les 5 opérations imposées par le contrat restent publiques (RG22). Les autres routes exigent une session (cookie HttpOnly, SameSite=Strict, jeton XSRF).

## Développement sans Docker

```bash
# Backend : PostgreSQL local attendu (variables DB_URL, DB_USERNAME, DB_PASSWORD, voir .env.example)
cd backend && ./mvnw spring-boot:run
# Frontend : relaie /api vers http://localhost:8080 (proxy.conf.json)
cd frontend && npm ci && npm start
```

## Tests et qualité

```bash
cd backend && ./mvnw verify                                   # unitaires + intégration (H2, sans base externe), JaCoCo
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless
```

- CI GitHub Actions sur chaque PR : backend (Maven verify) et frontend (build + tests).
- SonarQube (Quality Gate de l'équipe) : analyse locale avant chaque PR, résultat recopié dans la PR (le serveur est sur un réseau privé).

## Structure

```text
api/contrat.yaml      Contrat OpenAPI 3 (v2.0) — les opérations imposées sont inchangées
backend/              Spring Boot : controller / service / repository / entity / dto / exception / securite
frontend/             Angular 17 : core/api (seul accès HTTP), features (formateur, etudiant, relecteur)
docs/                 Cahier des charges, spécifications, diagrammes D1–D5, plans, backlog, journal
docker-compose.yml    PostgreSQL + backend + frontend (nginx, même origine)
```

Documentation d'analyse : [cahier des charges](docs/CAHIER_DES_CHARGES.md) · [spécifications fonctionnelles](docs/SPECIFICATIONS_FONCTIONNELLES.md) · [diagrammes](docs/diagrammes/) · [journal](docs/JOURNAL.md) · [contribuer](docs/CONTRIBUTING.md).
