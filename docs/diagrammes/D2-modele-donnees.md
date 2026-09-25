# D2 — Modèle de données

**Doit correspondre exactement aux migrations Flyway** (`V1__init.sql`, puis **v2** : `V3__securite.sql` pour `UTILISATEUR`, `FORMATEUR_PROMOTION` et `etudiant.actif`, `V4__piece_jointe.sql` pour les colonnes `fichier_*`). Toute migration qui change le schéma met ce diagramme à jour dans la même PR. Dictionnaire complet : [CAHIER_DES_CHARGES.md](../CAHIER_DES_CHARGES.md), annexe B.

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
    ETUDIANT |o--o| UTILISATEUR : "a un compte (v2)"
    UTILISATEUR ||--o{ FORMATEUR_PROMOTION : "est rattaché (v2)"
    PROMOTION ||--o{ FORMATEUR_PROMOTION : "a pour formateurs (v2)"

    PROMOTION {
        bigint id PK
        varchar nom UK "NOT NULL"
    }
    ETUDIANT {
        bigint id PK
        varchar nom "NOT NULL"
        bigint promotion_id FK "NOT NULL"
        boolean actif "v2, défaut TRUE (RG28)"
    }
    UTILISATEUR {
        bigint id PK "v2"
        varchar login UK "RG27"
        varchar mot_de_passe_hash "BCrypt (ENF10)"
        varchar role "ADMIN | FORMATEUR | ETUDIANT"
        varchar nom_affiche "NOT NULL"
        bigint etudiant_id FK, UK "NULL, obligatoire si ETUDIANT"
        boolean actif "RG28"
        boolean doit_changer_mot_de_passe "RG23"
        int echecs_connexion "RG24"
        timestamptz bloque_jusqu_a "NULL"
        timestamptz cree_at "NOT NULL"
    }
    FORMATEUR_PROMOTION {
        bigint utilisateur_id PK, FK "v2 (RG26)"
        bigint promotion_id PK, FK
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
        varchar fichier_nom "v2, NULL (RG30)"
        varchar fichier_type "v2, NULL"
        bigint fichier_taille "v2, NULL, <= 10 Mo"
        varchar fichier_chemin "v2, NULL, relatif à UPLOAD_DIR"
        timestamptz fichier_depose_at "v2, NULL"
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
