# D3 — Séquence : marquer sa présence

Codes HTTP identiques à `POST /api/presences` dans [api/contrat.yaml](../../api/contrat.yaml). L'ordre des contrôles est celui de la fiche SF-3.

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (PresenceApiService)
    participant C as PresenceController
    participant S as PresenceService
    participant T as TentativeCodeRepository
    participant SR as SessionRepository
    participant PR as PresenceRepository
    participant A as AssignationService

    E->>F: saisit le code
    F->>C: POST /api/presences {code, etudiantId}
    C->>C: @Valid (code, etudiantId non nuls)
    alt champ manquant
        C-->>F: 400 {code: "CHAMP_MANQUANT"}
    end
    C->>S: marquer(code, etudiantId)
    S->>T: findByEtudiantId
    alt bloqué (RG4)
        S-->>C: TropDeTentativesException
        C-->>F: 429 {code: "TROP_DE_TENTATIVES"}
    end
    S->>SR: findByCode(code)
    alt code inconnu
        S->>T: incrémenter échecs (blocage 2 min au 5e)
        S-->>C: CodeInconnuException
        C-->>F: 400 {code: "CODE_INCONNU"}
    else session clôturée (RG18)
        S-->>C: SessionClotureeException
        C-->>F: 409 {code: "SESSION_CLOTUREE"}
    else code expiré (RG1)
        S-->>C: CodeExpireException
        C-->>F: 410 {code: "CODE_EXPIRE"}
    else étudiant d'une autre promotion (RG19)
        S-->>C: EtudiantHorsPromotionException
        C-->>F: 400 {code: "ETUDIANT_HORS_PROMOTION"}
    else déjà présent (RG3)
        S->>PR: existsBySessionIdAndEtudiantId
        S-->>C: DejaPresentException
        C-->>F: 409 {code: "DEJA_PRESENT"}
    else cas nominal
        S->>PR: save(Presence source=ETUDIANT)
        S->>T: remettre échecs à 0
        S->>A: assignerExercicesEnAttente(sessionId)
        S-->>C: PresenceDto
        C-->>F: 201 {id, sessionId, etudiantId, source: "ETUDIANT"}
    end
    F-->>E: confirmation ou message d'erreur (champ message)
```

Les exceptions métier sont traduites en `{code, message}` par un unique `@RestControllerAdvice` (B4). Le contrôleur ne fait aucun accès à la base (B3).
