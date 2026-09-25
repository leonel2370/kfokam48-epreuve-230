# D2 — Modèle de données

**Doit correspondre exactement à `backend/src/main/resources/db/migration/V1__init.sql`.** Toute migration qui change le schéma met ce diagramme à jour dans la même PR. Dictionnaire complet : [CAHIER_DES_CHARGES.md](../CAHIER_DES_CHARGES.md), annexe B.

```mermaid
erDiagram
    PROMOTION ||--o{ ETUDIANT : "regroupe"
    PROMOTION ||--o{ SESSION : "a"
    SESSION ||--o{ PRESENCE : "enregistre"
    ETUDIANT ||--o{ PRESENCE : "a"
    ETUDIANT ||--o| TENTATIVE_CODE : "compteur RG4"
    SESSION ||--o{ EXERCICE : "reçoit"
    ETUDIANT ||--o{ EXERCICE : "est auteur de"
    EXERCICE ||--o| RELECTURE : "a au plus une (RG6)"
    ETUDIANT ||--o{ RELECTURE : "est relecteur de"

    PROMOTION {
        bigint id PK
        varchar nom UK "NOT NULL"
    }
    ETUDIANT {
        bigint id PK
        varchar nom "NOT NULL"
        bigint promotion_id FK "NOT NULL"
    }
    SESSION {
        bigint id PK
        varchar titre "NOT NULL"
        bigint promotion_id FK "NOT NULL"
        varchar code "6 car., unique parmi les non expirés (RG20)"
        timestamptz ouverture_at "NOT NULL"
        timestamptz expiration_at "ouverture + 15 min (RG1)"
        varchar statut "OUVERTE | CLOTUREE"
        timestamptz cloture_at "NULL"
    }
    PRESENCE {
        bigint id PK
        bigint session_id FK "UK(session_id, etudiant_id) RG3"
        bigint etudiant_id FK
        varchar source "ETUDIANT | FORMATEUR"
        timestamptz marquee_at "NOT NULL"
    }
    TENTATIVE_CODE {
        bigint etudiant_id PK, FK
        int echecs_consecutifs "défaut 0"
        timestamptz bloque_jusqu_a "NULL"
    }
    EXERCICE {
        bigint id PK
        bigint session_id FK "UK(session_id, auteur_id) RG13"
        bigint auteur_id FK
        varchar lien "NOT NULL, http(s)"
        varchar statut "DEPOSE | EN_ATTENTE_RELECTURE | RELU"
        timestamptz depose_at "NOT NULL"
        timestamptz modifie_at "NULL"
    }
    RELECTURE {
        bigint id PK
        bigint exercice_id FK, UK "RG6"
        bigint relecteur_id FK "différent de l'auteur (RG5, service)"
        int note "NULL, CHECK 0..20 (RG9)"
        text commentaire "NULL"
        timestamptz assignee_at "NOT NULL"
        timestamptz rendue_at "NULL = à faire ; non NULL = définitive (RG10)"
    }
```

Contrainte non exprimable en SQL simple : `relecture.relecteur_id ≠ exercice.auteur_id` et relecteur présent à la session (RG5, RG7). Elle est garantie par `RelectureService` et couverte par un test unitaire.
