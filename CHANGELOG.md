# Changelog

Toutes les évolutions notables de Présence48. Format : [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), versions : [SemVer](https://semver.org/lang/fr/).
Chaque entrée renvoie à son issue et à sa pull request : l'historique Git en est la source.

## [1.1.0] — 2026-09-25/26

### Sécurité
- La liste des sessions ne révèle plus le code de présence aux étudiants (#98, PR #99) : le code est réservé au formateur.
- Un étudiant connecté ne peut plus savoir si un autre identifiant d'étudiant existe : le contrôle d'identité passe avant le contrôle d'existence (#109, PR #117) ; le contrat documente ces réponses (2.3).

### Corrigé
- Relecture : les listes se mettent à jour, les relectures rendues restent visibles, l'envoi est bloqué sans note valide (#101, PR #102).
- Espace étudiant : la liste des sessions se recharge après une présence ou un dépôt, avec une zone d'erreur dédiée (#107, PR #118).
- Les erreurs sont effacées après un rafraîchissement réussi et les boutons ne peuvent plus envoyer deux fois (#106, PR #119) ; les messages de succès s'effacent aussi (5 s).
- Libellés trompeurs : statuts et rôle en français, texte honnête après expiration du code, tableau titré avec sa promotion, adresse de promotion invalide gérée sans appel serveur (#108, PR #120).
- L'analyse (cahier, spécifications, D2, README) est à jour de la livraison réelle : migrations V1–V4, #59 livré, hypothèses remplacées en v2 signalées, contrat 2.3 (#110).

### Ajouté
- **Swagger UI** : documentation de l'API générée, annotée (descriptions, codes d'erreur réels, authentification cookieAuth), avec deux définitions (contrat de référence et implémentation) et guide `docs/GUIDE_API.md` (#96, PR #97 ; annotations #105, PR #121).
- **Connexion et menus par rôle** (frontend de la sécurité v2) : login/logout, gardes de routes, navigation par rôle (#59, PR #100).
- **Navigation par boutons et écran relecteur distinct** (#104, PR #116) : l'espace étudiant devient trois écrans (`presence`, `notes`, `relectures`), bouton « Ouvrir l'exercice », composants partagés `bouton-navigation`.
- Contrat d'API **2.3** : `x-livre` sur chaque opération (les non livrées préfixées « Non livré (backlog #n) »), réponses 400/401/403 manquantes, descriptions à jour (#111, PR #122).

### Refactoré
- Chaque composant a son template et ses styles dans des fichiers séparés (#103, PR #114).

### Sécurité
- Une fiche étudiant désactivée ne peut plus marquer sa présence ni déposer : `403 ETUDIANT_DESACTIVE` sur les opérations publiques `POST /api/presences` et `POST /api/exercices`, avant tout autre contrôle (RG28, décision PO — #113, contrat 2.4).

### Ajouté
- **CRUD des comptes** (ADMIN, contrat 2.6) : liste paginée, création (mot de passe provisoire à changer à la première connexion, RG23 ; login unique RG27), modification, désactivation RG28 avec protection du dernier admin actif, réinitialisation de mot de passe ; écran « Comptes » dans l'espace ADMIN (#60).
- **Gestion du référentiel** (contrat 2.7) : promotions par l'ADMIN — créer, renommer, supprimer (RG28 : refusée si étudiants ou sessions ; nom unique → 409 CONFLIT), rattachement des formateurs (RG26 : seuls des comptes FORMATEUR, le rattachement ouvre la promotion) ; fiches étudiants par l'ADMIN et le formateur de la promotion (RG26) — créer, modifier, supprimer sans historique ou désactiver (RG28, l'historique reste au tableau) ; volet « Gérer » dans l'écran ADMIN (#61).
- **Récapitulatif de l'étudiant connecté** : `GET /api/moi/recap` (contrat 2.5) renvoie sa ligne de tableau — présences, exercices, moyenne des notes retenues (RG16), relectures en attente ; encart « Récapitulatif » dans « Mes notes » (#112).

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
