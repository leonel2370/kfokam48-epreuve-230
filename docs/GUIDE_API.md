# Guide de l'API — utiliser Swagger UI pas à pas

Swagger UI : **http://localhost:8080/swagger-ui.html** (public, sans connexion). Le menu en haut à droite
propose deux définitions :

- **Contrat (référence)** : `api/contrat.yaml` tel quel — le contrat qui fait foi ;
- **Implémentation (générée)** : `/v3/api-docs` — ce que le backend expose réellement, documenté depuis le
  code (tags, résumés, règles RGx, codes d'erreur métier avec exemples réels).

## Comment se connecter dans Swagger UI

1. Ouvrir http://localhost:8080/swagger-ui.html.
2. Chercher **POST /api/auth/login** → « Try it out » → saisir un compte de démonstration
   (voir le [README](../README.md#données-et-comptes-de-démonstration)), par exemple
   `{"login":"formateur","motDePasse":"Formateur48"}` → **Execute**.
3. La réponse 200 renvoie le profil. Le cookie de session `JSESSIONID` (HttpOnly) est maintenant posé
   **dans la même fenêtre** : toutes les exécutions suivantes sont authentifiées.
4. CSRF : sur les écritures protégées, Swagger UI recopie automatiquement le cookie `XSRF-TOKEN` dans
   l'en-tête `X-XSRF-TOKEN` (intercepteur injecté dans `swagger-initializer.js`, #105). Sans cela,
   « Try it out » renverrait 403 comme le frontend sans jeton (ENF11).

## Ce qui est public, ce qui ne l'est pas (RG22)

| Public sans session | Protégé (401 sans session, 403 hors rôle) |
|---|---|
| Les 5 opérations imposées : `POST /api/sessions`, `POST /api/presences`, `POST /api/exercices`, `POST /api/relectures/{id}`, `GET /api/tableau` | Tout le reste : profil, mot de passe, listes de sessions, exercices et relectures d'un étudiant |
| `POST /api/auth/login` | |

Avec une session, les opérations imposées appliquent en plus les contrôles d'identité et de rôle
(403 `IDENTITE_DIFFERENTE`, `ACCES_REFUSE`) — voir le cahier des charges §7.2 bis (RISQUE-1).

## Exemples de parcours

### Formateur : ouvrir une session puis lire son tableau

1. `POST /api/auth/login` (`formateur` / `Formateur48`).
2. `POST /api/sessions` avec `{"titre":"TP JPA","promotionId":1}` → 201, le code (ex. `K7MX4Q`) et
   `expirationAt` (RG1 : ouverture + 15 min).
3. `GET /api/tableau?promotionId=1` → une ligne par étudiant, moyenne calculée par le serveur (RG16).

### Étudiant : présence puis dépôt

1. `POST /api/presences` avec `{"code":"K7MX4Q","etudiantId":1}` → 201 `source: ETUDIANT`.
   Erreurs : 400 `CODE_INCONNU`, 409 `DEJA_PRESENT`, 410 `CODE_EXPIRE`, 429 `TROP_DE_TENTATIVES` (RG4).
2. `POST /api/exercices` avec `{"sessionId":…,"etudiantId":1,"lien":"https://github.com/awa/tp"}` → 201.
   Le tirage assigne deux relecteurs présents (RG6 v3, RG7).

### Relecteur : rendre une note

1. `GET /api/etudiants/{id}/relectures?statut=A_FAIRE` (connecté en tant que cet étudiant).
2. `POST /api/relectures/{id}` avec l'en-tête `X-Etudiant-Id` (HYP-2) et
   `{"note":15,"commentaire":"Clair et testé"}` → 200. Note entière 0–20 (RG9) ; un second envoi
   renvoie 409 `RELECTURE_DEJA_RENDUE` (RG10, Q15).

## Lire la documentation générée

- Chaque opération porte son **tag**, son **résumé**, sa **description** (règles RGx) et ses **réponses
  d'erreur** avec un exemple du vrai code métier (jamais inventé : les codes viennent du backend).
- Le schéma de sécurité `cookieAuth` (cookie `JSESSIONID`) est déclaré ; les opérations publiques
  n'exigent aucune sécurité, les autres affichent le cadenas fermé.
- Garde-fou automatisé : `SwaggerIntegrationTest` vérifie que **toute** opération a un tag, un résumé et
  une description — un futur endpoint non documenté fera échouer la CI.

## Vérification navigateur (recette #105)

1. Ouvrir Swagger UI, se connecter en `admin`/`admin` (changement de mot de passe imposé, RG23).
2. `PUT /api/moi/mot-de-passe` avec l'ancien et un nouveau mot de passe de 8 caractères → 204.
3. `POST /api/sessions` → 201 : les écritures protégées réussissent depuis Swagger UI grâce au jeton CSRF.
