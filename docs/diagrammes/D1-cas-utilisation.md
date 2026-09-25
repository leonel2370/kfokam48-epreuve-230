# D1 — Cas d'utilisation

Source : [CAHIER_DES_CHARGES.md](../CAHIER_DES_CHARGES.md) §2 et §4. Le **Relecteur** est un Étudiant assigné par le système : il hérite de l'Étudiant (généralisation), ce n'est pas une table distincte.

```mermaid
flowchart LR
    F((Formateur))
    E((Étudiant))
    R((Relecteur))
    S((Système))
    R -. "est un" .-> E

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
    S --> UC4
    S --> UC7

    UC3 -. "include" .-> UC4
    UC6 -. "include" .-> UC7
    UC3 -. "extend : exercices sans relecteur" .-> UC7
    UC5 -. "extend : exercices sans relecteur" .-> UC7
```
