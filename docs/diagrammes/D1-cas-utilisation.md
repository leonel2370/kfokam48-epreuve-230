# D1 — Cas d'utilisation

Source : [CAHIER_DES_CHARGES.md](../CAHIER_DES_CHARGES.md) §2, §2 bis et §4. **v2** : acteur Administrateur, cas d'accès (EF15–EF19) et d'administration (EF21–EF25). Le **Relecteur** est un Étudiant assigné par le système : il hérite de l'Étudiant (généralisation), ce n'est pas une table distincte.

```mermaid
flowchart LR
    AD((Administrateur))
    U((Utilisateur connecté))
    F((Formateur))
    E((Étudiant))
    R((Relecteur))
    S((Système))
    R -. "est un" .-> E
    AD -. "est un" .-> U
    F -. "est un" .-> U
    E -. "est un" .-> U

    subgraph PRESENCE48
        UC1([EF1 Choisir son identité])
        UC2([EF2 Ouvrir une session])
        UC3([EF3 Marquer sa présence])
        UC4([EF4 Bloquer après 5 erreurs])
        UC5([EF5 Ajouter une présence à la main])
        UC6([EF6 Déposer un exercice])
        UC13([EF13 Remplacer le lien])
        UC7([EF7 Tirer un relecteur au sort])
        UC8([EF8 Voir ses relectures])
        UC9([EF9 Rendre une relecture])
        UC14([EF14 Voir sa note])
        UC10([EF10 Consulter le tableau])
        UC11([EF11 Voir les exercices d'une session])
        UC12([EF12 Clôturer une session])
        UC15([EF15 Se connecter / se déconnecter])
        UC16([EF16 Voir son profil])
        UC17([EF17 Changer son mot de passe])
        UC21([EF21 Gérer les comptes])
        UC22([EF22 Gérer promotions et rattachements])
        UC23([EF23 Gérer les fiches étudiants])
        UC24([EF24 Modifier ou supprimer ses sessions])
        UC25([EF25 Joindre un fichier à son exercice])
    end

    F --> UC2
    F --> UC5
    F --> UC10
    F --> UC11
    F --> UC12
    E --> UC1
    E --> UC3
    E --> UC6
    E --> UC13
    E --> UC14
    R --> UC8
    R --> UC9
    U --> UC15
    U --> UC16
    U --> UC17
    AD --> UC21
    AD --> UC22
    AD --> UC23
    F --> UC23
    F --> UC24
    E --> UC25
    S --> UC4
    S --> UC7

    UC3 -. "include" .-> UC4
    UC6 -. "include" .-> UC7
    UC3 -. "extend : exercices sans relecteur" .-> UC7
    UC5 -. "extend : exercices sans relecteur" .-> UC7
```
