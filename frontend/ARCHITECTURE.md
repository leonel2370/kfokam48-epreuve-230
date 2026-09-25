# Architecture Angular 17

## Structure

Projet généré par `createAngularStructure.sh` (Angular 17, standalone), puis adapté au projet (#13) :

```text
src/app/
├── core/
│   ├── api/            api.models.ts (types du contrat) · api.service.ts (seul accès HTTP)
│   └── interceptors/   erreur.interceptor.ts : toute erreur devient { code, message } (ENF3)
├── features/           un dossier par écran : accueil, formateur, etudiant, relecteur
└── app.config.ts       HttpClient + intercepteur + XSRF (cookie XSRF-TOKEN, même origine)
```

Règles : aucune règle métier ni calcul côté client (F3) ; le frontend appelle `/api` en même origine
(`proxy.conf.json` en développement, nginx en production).

## Core

Le dossier core contient les éléments globaux de l'application.

Exemples :

- Services globaux
- Authentification
- Guards
- Interceptors HTTP
- Modèles communs
- Constantes


## Features

Le dossier features contient les fonctionnalités métier.

Exemple :

features/
├── users/
├── products/
└── orders/

Chaque fonctionnalité peut avoir sa propre structure :

users/
├── components/
├── models/
├── pages/
└── services/


## Shared

Le dossier shared contient les éléments réutilisables.

Exemples :

- Composants génériques
- Directives
- Pipes
- Modèles partagés


## Layout

Le dossier layout contient la structure visuelle globale.

Exemples :

- Header
- Footer
- Navbar
- Sidebar


## Principe général

CORE
    ↓
Infrastructure globale

FEATURES
    ↓
Fonctionnalités métier

SHARED
    ↓
Composants réutilisables

LAYOUT
    ↓
Structure visuelle
