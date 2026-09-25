# Changelog

Toutes les évolutions notables de Présence48. Format : [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), versions : [SemVer](https://semver.org/lang/fr/).
Chaque entrée renvoie à son issue et à sa pull request : l'historique Git en est la source.

## [1.0.0] — 2026-09-25

### Ajouté
- **Double relecture** (enveloppe, changement de besoin #85). Deux pairs différents relisent chaque exercice, et la note retenue est leur moyenne. Une note seule reste **provisoire** (RG6 v3, RG16 v3, RG31) :
  - analyse mise à jour d'abord : #86, PR #89 ;
  - backend : #87, PR #90 ;
  - écran « Mes notes » : #88, PR #91.
- Migration Flyway **V4** `double_relecture` : `UNIQUE(exercice_id, relecteur_id)`, ajoutée sans modifier V1 et vérifiée sur une base remplie.
- `GET /api/etudiants/{id}/exercices` (contrat **2.1**) : `noteRetenue`, `provisoire`, `commentaires`, sans les relecteurs (RG8). Cela livre aussi #34.

### Corrigé
- Deux présences simultanées : l'une était perdue quand un exercice de la session attendait son relecteur (`409 CONFLIT`). Le tirage verrouille maintenant la ligne de l'exercice. Test de régression concurrent. #83, PR #84.

### Documentation
- Journal des étapes 3 et 4, backlog restant trié, README testé depuis un clone vierge (#92, #94).

## [0.1.0] — 2026-09-25

### Ajouté
- **Opérations imposées** du contrat :
  - ouverture de session avec un code valable 15 min : #16, PR #69 ;
  - présence par code, avec blocage après 5 codes erronés : #18, PR #70 ;
  - dépôt d'exercice : #20, PR #71 ;
  - tirage d'un relecteur présent autre que l'auteur, retenté à chaque présence : #21, PR #72 ;
  - note définitive du relecteur : #23, PR #73 ;
  - tableau calculé par le serveur : #25, PR #74.
- **Sécurité v2** (#54) :
  - connexion, déconnexion, profil et mot de passe, admin par défaut forcé à changer : #57, PR #67 ;
  - rôles ADMIN/FORMATEUR/ETUDIANT : #58 ;
  - les opérations imposées restent publiques (RG22).
- Référentiel promotions/étudiants (#14, PR #68), données de démonstration (#12, PR #66), erreurs au format `{code, message}` (#11, PR #53).
- **Frontend Angular 17** :
  - couche API typée : #13, PR #75 ;
  - écran formateur : #17, PR #76 ;
  - tableau : #26, PR #77 ;
  - écran étudiant, conçu pour mobile : #15, #19, #22, PR #78 ;
  - écran relecteur : #24, PR #79.
- Démarrage en une commande : `docker compose up --build` (#27, PR #82). CI GitHub Actions backend et frontend.

### Corrigé
- Les tests utilisaient la base de `.env` au lieu de H2 (#51, PR #52).

## [0.0.0] — 2026-09-25 — analyse

### Documentation
- Cahier des charges, spécifications fonctionnelles, diagrammes D1–D5, contrat OpenAPI, plan technique, conventions, système de design et templates (#1–#9, PR #39–#47 ; v2 : #55, #56, PR #64, #65).
