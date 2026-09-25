# D5 — Séquence : se connecter (v2)

Codes HTTP identiques à `POST /api/auth/login` et `GET /api/moi` dans [api/contrat.yaml](../../api/contrat.yaml). Fiches SF-15, SF-17, SF-18 ; règles RG22 à RG24, RG28.

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant F as Front (AuthService)
    participant C as AuthController
    participant S as AuthService (Spring Security)
    participant R as UtilisateurRepository
    participant H as HttpSession

    U->>F: saisit identifiant et mot de passe
    F->>C: POST /api/auth/login {login, motDePasse}
    C->>S: authentifier(login, motDePasse)
    S->>R: findByLogin(login)
    alt compte inconnu ou mot de passe faux
        S->>R: echecs_connexion + 1 (blocage 2 min au 5e, RG24)
        S-->>C: BadCredentials
        C-->>F: 401 {code: "IDENTIFIANTS_INVALIDES"}
    else compte désactivé (RG28)
        C-->>F: 403 {code: "COMPTE_DESACTIVE"}
    else compte bloqué (RG24)
        C-->>F: 429 {code: "TROP_DE_TENTATIVES"}
    else cas nominal
        S->>R: echecs_connexion = 0
        S->>H: créer la session (SecurityContext)
        C-->>F: 200 {id, login, nomAffiche, role, doitChangerMotDePasse} + cookies JSESSIONID (HttpOnly) et XSRF-TOKEN
        alt doitChangerMotDePasse (admin/admin, RG23)
            F->>U: écran « Changer le mot de passe »
            F->>C: GET /api/sessions (toute autre route)
            C-->>F: 403 {code: "CHANGEMENT_MOT_DE_PASSE_REQUIS"}
        else
            F->>C: GET /api/moi
            C-->>F: 200 profil → menus selon le rôle
        end
    end
    U->>F: Se déconnecter
    F->>C: POST /api/auth/logout (X-XSRF-TOKEN)
    C->>H: invalider la session
    C-->>F: 204
    F->>C: GET /api/moi (ancien cookie)
    C-->>F: 401 {code: "NON_AUTHENTIFIE"}
```
