# D4 — États-transitions du cycle de vie d'un exercice (bonus)

```mermaid
stateDiagram-v2
    [*] --> DEPOSE : POST /api/exercices\n(aucun autre étudiant présent)
    [*] --> EN_ATTENTE_RELECTURE : POST /api/exercices\n(1 ou 2 relecteurs tirés, RG7)
    DEPOSE --> EN_ATTENTE_RELECTURE : nouvelle présence dans la session\n(tirage, HYP-3)
    DEPOSE --> DEPOSE : PUT lien (RG14)
    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE : PUT lien (RG14)
    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE : 1re note rendue\n(note retenue provisoire, RG31)\nou 2e relecteur tiré (HYP-20)
    EN_ATTENTE_RELECTURE --> RELU : 2e note rendue\nnote retenue = moyenne (RG16 v3)
    RELU --> [*]

    note right of RELU
        Définitif (RG10) :
        second envoi → 409 RELECTURE_DEJA_RENDUE,
        PUT lien → 409 EXERCICE_DEJA_RELU
    end note
    note left of DEPOSE
        Clôture de la session (RG18) :
        l'état est figé ; DEPOSE et
        EN_ATTENTE_RELECTURE restent
        visibles « en attente » (Q11)
    end note
```

**v3 (enveloppe, #85)** : l'exercice ne passe à `RELU` que lorsque ses **deux** relecteurs ont rendu ; entre les deux, la note retenue est provisoire (RG31).

Côté API, « en attente » (Q11) recouvre `DEPOSE` et `EN_ATTENTE_RELECTURE`.
