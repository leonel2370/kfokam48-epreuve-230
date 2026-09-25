# Cahier des charges — PRESENCE48 (présence et relecture par les pairs)

**Auteur :** nono leonel · matricule 230
**Version :** 2 · **Date :** 2026-09-25 (v2 : sécurité, rôles, CRUD, pièce jointe — issue #54)
**Frontend choisi :** Angular 17, parce que son architecture imposée (services injectables, `HttpClient`, intercepteurs) isole naturellement la couche d'appels API exigée par F3.

> Documents liés : [SPECIFICATIONS_FONCTIONNELLES.md](SPECIFICATIONS_FONCTIONNELLES.md) (fiches détaillées, flows, user stories) ·
> [diagrammes/](diagrammes/) · [../api/contrat.yaml](../api/contrat.yaml) · [PLAN_TECHNIQUE_EQUIPES.md](PLAN_TECHNIQUE_EQUIPES.md) · [CONTRIBUTING.md](CONTRIBUTING.md)
>
> **Convention de lecture.** Chaque décision cite sa source : `Qx` = question de `CLIENT.md`, `SUJET` = énoncé,
> `CONTRAT` = `api/contrat.yaml`. Une décision marquée **[HYP-x]** est une hypothèse prise à la place du client
> (section 7) : elle n'invente pas un besoin, elle comble un trou et reste discutable.

---

## 1. Contexte et objectif

La formation KFOKAM48 suit aujourd'hui la présence et les exercices de ses étudiants à la main. Trois problèmes en découlent :
la présence est déclarative et falsifiable, les exercices rendus ne sont relus par personne de façon traçable, et le formateur
n'a aucune vue consolidée de l'assiduité et du niveau de chaque étudiant.

PRESENCE48 est une application web qui :

- prouve la présence physique par un **code court à durée de vie limitée** affiché par le formateur ;
- collecte le **lien** de l'exercice de chaque étudiant pour une session ;
- organise une **relecture par un pair tiré au sort**, notée sur 20 et commentée ;
- donne au formateur un **tableau par promotion** : présences, exercices déposés, moyenne reçue, relectures en retard.

**Depuis la v2 (décision du PO, 25/09, #54)**, chaque personne a un **compte personnel** (connexion, déconnexion, mot de passe) avec un **rôle** qui détermine ce qu'elle voit et fait ; un administrateur crée les comptes et gère promotions, étudiants et sessions ; l'étudiant peut joindre un fichier (ou un dossier zippé) à son exercice.

Valeur attendue : fiabiliser l'assiduité, faire pratiquer la revue de code entre pairs et donner au formateur une vue lui permettant d'agir sans ressaisir.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Administrateur** *(v2)* | Se connecter (compte par défaut `admin`, mot de passe à changer à la première connexion) · créer, modifier, désactiver les comptes et leur rôle · réinitialiser un mot de passe · CRUD des promotions, rattachement des formateurs à leurs promotions · CRUD des fiches étudiants · consulter toutes les données | Rendre une relecture, marquer une présence « ETUDIANT » · supprimer physiquement un compte ou un étudiant ayant un historique (il le désactive, RG28) |
| **Formateur** | *(v2 : connecté, limité à ses promotions, RG26)* Ouvrir une session et obtenir son code · ajouter une présence à la main · voir les exercices d'une session et leur statut · clôturer une session · consulter le tableau d'une promotion | Marquer une présence « ETUDIANT » à la place de l'étudiant · noter un exercice (il voit en revanche le nom du relecteur, [HYP-9]) |
| **Étudiant** | *(v2 : se connecte avec son compte, lié à sa fiche étudiant ; la liste de noms de Q1 ne sert plus que pour les opérations imposées publiques)* Choisir son nom dans une liste (Q1) · joindre un fichier ou un .zip à son exercice *(v2)* · marquer sa présence avec un code · déposer puis remplacer le lien de son exercice · consulter la note et le commentaire reçus, sans le nom du relecteur (Q8) | Relire son propre exercice (Q5) · marquer sa présence après expiration du code (Q2, Q3) · déposer deux exercices pour la même session |
| **Relecteur** | *Ce n'est pas un acteur distinct* : c'est un **Étudiant** à qui le système a assigné une relecture. Il voit les relectures qui lui sont assignées et rend une note entière de 0 à 20 avec un commentaire | Choisir l'exercice qu'il relit (Q7) · modifier une relecture rendue (Q15, voir §7) · relire après clôture |
| **Tout utilisateur connecté** *(v2)* | Voir son profil (qui il est, son rôle) · changer son mot de passe · se déconnecter | Voir ou modifier ce que son rôle n'autorise pas (403, RG25) |
| **Système** | Valider automatiquement la présence dès qu'un code valide est soumis (RG21) · faire expirer le code · tirer le relecteur au sort · bloquer après 5 codes erronés · calculer la moyenne | — |

**Décision de modélisation :** le relecteur est un étudiant dans un état donné. Il n'y a pas de table `relecteur` : la table `relecture`
porte une clé étrangère `relecteur_id` vers `etudiant`. Conséquence : un même étudiant est à la fois auteur et relecteur au sein d'une même session.

## 2 bis. Matrice des droits par rôle *(v2)*

Les **5 opérations imposées** par le contrat restent **publiques** (décision PO, pour respecter B2 à la lettre), ainsi que la connexion et les deux listes de sélection de nom ; toutes les autres exigent une session. Voir RG22, RG25.

**Comptes de démonstration** (mots de passe conformes à RG24, à changer hors démonstration) : `admin`/`admin` (changement imposé, RG23), `formateur`/`Formateur48`, `awa`/`Etudiant48`, `paul`/`Etudiant48`… — liste complète dans le README.

| Ressource | ADMIN | FORMATEUR | ETUDIANT |
|---|---|---|---|
| Comptes utilisateurs (CRUD, rôle, désactivation, réinitialisation du mot de passe) | CRUD | — | — |
| Promotions + rattachement des formateurs | CRUD | lecture des siennes | lecture de la sienne |
| Fiches étudiants | CRUD | lecture ; création et modification dans ses promotions | lecture de sa fiche |
| Sessions | tout | CRUD dans ses promotions (suppression seulement sans présence ni exercice) | lecture des sessions de sa promotion |
| Présences | lecture | lecture ; ajout manuel ; suppression d'une présence manuelle | créer la sienne (opération imposée) ; lire les siennes |
| Exercices + pièce jointe | lecture | lecture dans ses promotions | CRUD du sien (création imposée ; remplacement, pièce jointe, suppression tant que non relu) |
| Relectures | lecture | lecture, avec le relecteur | lire et rendre celles assignées ; voir sa note sans le relecteur |
| Tableau | toutes les promotions | ses promotions | — (son récapitulatif personnel) ; `GET /api/tableau` étant imposé, il reste public **sans** session (RISQUE-1) |
| Profil, mot de passe, déconnexion | ✓ | ✓ | ✓ |

## 3. Périmètre

> v2 (#54) : l'authentification, les rôles et les CRUD **entrent** dans le périmètre ; les exclusions correspondantes de la v1 sont barrées et conservées pour l'historique.

**Inclus dans cette version :**

- sessions de cours : ouverture avec code, clôture ;
- présence : par code (source `ETUDIANT`), manuelle (source `FORMATEUR`), blocage anti-devinette ;
- exercices : dépôt, remplacement du lien, statut ;
- relecture : assignation aléatoire, notation, consultation anonymisée par l'auteur ;
- tableau récapitulatif par promotion ;
- données de démonstration (1 formateur implicite, 2 promotions, ~12 étudiants) chargées au démarrage ;
- un frontend avec trois écrans : Formateur, Étudiant, Relecteur (F2) — v3.1 : Relecteur = `/etudiant/relectures`, écran distinct de l'espace étudiant (#104) ;
- *(v2)* authentification par identifiant et mot de passe, déconnexion, profil connecté, changement de mot de passe ;
- *(v2)* rôles ADMIN, FORMATEUR, ETUDIANT et matrice d'accès (§2 bis) ; compte administrateur par défaut ;
- *(v2)* CRUD : comptes (admin), promotions et étudiants (admin, formateur sur ses promotions pour les étudiants), sessions (formateur), exercice et pièce jointe (étudiant) ;
- *(v2)* pièce jointe d'exercice : un fichier ou un dossier compressé en `.zip`, en plus du lien.

**Explicitement exclu :**

- ~~authentification et mots de passe (Q1)~~ → inclus en v2 ;
- ~~gestion (CRUD) des promotions, des étudiants et des formateurs~~ → inclus en v2 ;
- ~~plusieurs formateurs et droits différenciés~~ → inclus en v2 ;
- *(v2)* **journal d'audit** (retiré par le PO le 25/09) ;
- *(v2)* réinitialisation du mot de passe par e-mail, SSO (Google, LDAP), double authentification ;
- *(v2)* téléversement d'un dossier non compressé (un navigateur ne le transmet pas de façon fiable) : le dossier est déposé en `.zip` ;
- notifications (e-mail, SMS, push) ;
- export (PDF, Excel) du tableau ;
- réassignation manuelle d'un relecteur par le formateur *(candidat pour une version ultérieure)* ;
- contestation d'une note, double relecture (Q6) ;
- application mobile native : le web responsive suffit (ENF1) ;
- rendu graphique soigné : le CSS n'est pas noté (SUJET).

## 4. Exigences fonctionnelles

Priorité MoSCoW. **Must** = requis pour `v0.1`. Le détail de chaque exigence (flux, erreurs, endpoint) est dans les fiches SF-x de [SPECIFICATIONS_FONCTIONNELLES.md](SPECIFICATIONS_FONCTIONNELLES.md).

| Réf | Exigence | Critère d'acceptation (quand … alors …) | Priorité | RG |
|---|---|---|---|---|
| EF1 | L'utilisateur choisit sa promotion puis son nom dans une liste | Quand j'ouvre l'écran Étudiant et choisis la promotion « P1 », alors je vois la liste des étudiants de P1 et je peux m'identifier sans mot de passe | Must | RG19 |
| EF2 | Le formateur ouvre une session et obtient un code | Quand j'envoie `{titre, promotionId}` valides, alors je reçois `201` avec un `code` et un `expirationAt` égal à `ouvertureAt` + 15 min | Must | RG1 |
| EF3 | L'étudiant marque sa présence avec le code | Quand je saisis un code valide non expiré, alors je reçois `201` avec `source = ETUDIANT` et ma présence est comptée dans le tableau | Must | RG1, RG2, RG3, RG19 |
| EF4 | Les tentatives de code erronées sont limitées | Quand je saisis 5 codes inconnus d'affilée, alors la 6ᵉ tentative dans les 2 minutes est refusée avec `429 TROP_DE_TENTATIVES`, même si le code est bon | Should | RG4 |
| EF5 | Le formateur ajoute une présence à la main | Quand j'ajoute la présence d'un étudiant, alors elle apparaît avec `source = FORMATEUR`, même après expiration du code | Should | RG3, RG15 |
| EF6 | L'étudiant dépose le lien de son exercice | Quand je dépose une URL http(s) valide pour une session non clôturée, alors je reçois `201` avec un `statut` | Must | RG12, RG13, RG17 |
| EF7 *(v3)* | Le système assigne **deux** relecteurs au hasard | Quand un exercice est déposé et qu'au moins un autre étudiant est présent, alors une relecture est créée par candidat tiré, **jusqu'à deux pairs différents**, et l'exercice passe à `EN_ATTENTE_RELECTURE` ; s'il manque un relecteur, le tirage est retenté à chaque nouvelle présence | Must | RG5, RG6, RG7 |
| EF8 | Le relecteur voit les relectures qui lui sont assignées | Quand j'ouvre l'écran Relecteur, alors je vois pour chaque relecture en attente le lien de l'exercice, sans le nom de l'auteur ([HYP-10]) | Must | RG7 |
| EF9 *(v3)* | Le relecteur rend une note et un commentaire | Quand j'envoie une note entière entre 0 et 20 et un commentaire, alors je reçois `200` ; l'exercice passe à `RELU` quand **ses deux relecteurs** ont rendu | Must | RG5, RG9, RG10, RG18, RG31 |
| EF10 | Le formateur consulte le tableau d'une promotion | Quand je demande le tableau de P1, alors je reçois une ligne par étudiant : présences, exercices déposés, moyenne reçue (`null` s'il n'a aucune note), relectures en attente | Must | RG11, RG16 |
| EF11 | Le formateur voit les exercices d'une session et leur statut | Quand un exercice n'a pas été relu, alors il apparaît avec le statut « en attente » dans la vue de la session | Should | RG11 |
| EF12 | Le formateur clôture une session | Quand je clôture une session, alors plus aucun dépôt, remplacement, relecture ni présence n'est accepté pour cette session (`409 SESSION_CLOTUREE`) | Should | RG12, RG18 |
| EF13 | L'étudiant remplace le lien de son exercice | Quand mon exercice n'a pas encore été relu, alors je peux remplacer le lien ; une fois relu, je reçois `409 EXERCICE_DEJA_RELU` | Should | RG14 |
| EF14 | L'étudiant consulte la note reçue | Quand mon exercice est relu, alors je vois la note et le commentaire, jamais le nom du relecteur | Should | RG8 |
| EF15 *(v2)* | Un utilisateur se connecte et se déconnecte | Quand je saisis un identifiant et un mot de passe valides, alors j'accède aux écrans de mon rôle ; quand je me déconnecte, alors toute route protégée me renvoie 401 | Must | RG22, RG23 |
| EF16 *(v2)* | Un utilisateur voit son profil connecté | Quand j'ouvre « Mon profil », alors je vois mon nom, mon identifiant et mon rôle | Must | RG25 |
| EF17 *(v2)* | Un utilisateur change son mot de passe | Quand je donne l'ancien mot de passe et un nouveau de 8 caractères ou plus, alors seul le nouveau fonctionne ensuite | Must | RG24 |
| EF18 *(v2)* | Le compte administrateur par défaut existe | Quand l'application démarre sur une base vide, alors `admin`/`admin` permet de se connecter mais impose un changement de mot de passe avant toute autre action | Must | RG23 |
| EF19 *(v2)* | Chaque rôle n'accède qu'à ce qui lui est autorisé | Quand un étudiant appelle une route réservée au formateur, alors il reçoit 403 ACCES_REFUSE | Must | RG25, RG26 |
| EF20 *(v2)* | Les opérations imposées restent publiques | Quand j'appelle une des 5 opérations imposées sans session, alors j'obtiens les codes du contrat (jamais 401) | Must | RG22 |
| EF21 *(v2)* | L'administrateur gère les comptes | Quand je crée un compte avec un identifiant déjà pris, alors je reçois 409 LOGIN_DEJA_UTILISE ; un compte désactivé ne peut plus se connecter | Should | RG27, RG28 |
| EF22 *(v2)* | L'administrateur gère promotions et rattachements | Quand je rattache un formateur à P1, alors il voit P1 et seulement ses promotions | Should | RG26 |
| EF23 *(v2)* | Administrateur et formateur gèrent les fiches étudiants | Quand je désactive un étudiant ayant des présences, alors ses présences et notes restent dans le tableau | Should | RG28 |
| EF24 *(v2)* | Le formateur modifie ou supprime ses sessions | Quand je supprime une session ayant des présences, alors je reçois 409 SUPPRESSION_IMPOSSIBLE | Should | RG29 |
| EF25 *(v2)* | L'étudiant joint un fichier ou un .zip à son exercice | Quand je joins un PDF de 2 Mo, alors il est téléchargeable par moi, mon relecteur et le formateur ; un fichier de 11 Mo renvoie 413 | Should | RG30 |
| EF26 *(v2)* | La présence est validée automatiquement | Quand je soumets un code valide, alors ma présence est enregistrée et visible dans le tableau sans aucune action du formateur | Must | RG21 |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'écran Étudiant (présence, dépôt) est utilisable sur téléphone, à partir de 360 px de large | Chrome DevTools en mode appareil 360×640 : aucun défilement horizontal, champ du code et bouton accessibles |
| ENF2 | Le tableau répond en moins de 2 s pour une promotion de 60 étudiants et 30 sessions | Jeu de données de charge + mesure `curl -w %{time_total}` ; calcul par requêtes agrégées (pas de N+1) |
| ENF3 | Toute erreur renvoie le format `{code, message}` ; jamais de stack trace ni de page d'erreur Spring | Tests d'intégration sur chaque code d'erreur + test d'une URL inconnue et d'un JSON mal formé |
| ENF4 | Le code de présence ne se devine pas raisonnablement en 5 essais | 6 caractères parmi 32 symboles (≈ 10⁹ combinaisons) + RG4 |
| ENF5 | Démarrage chez un tiers en 3 commandes maximum ou `docker compose up`, avec données de démonstration | Procédure du README rejouée depuis un clone vierge |
| ENF6 | Le schéma est versionné et reproductible | Flyway ; `ddl-auto=validate` hors tests |
| ENF7 | Les dates sont échangées en ISO-8601 avec fuseau (UTC) | Test de sérialisation de `ouvertureAt` / `expirationAt` |
| ENF8 | Aucun secret n'est commité : toutes les informations sensibles (jetons GitHub et Sonar, mots de passe de base) vivent dans `.env` à la racine, local et ignoré | `git check-ignore .env` ; `.env.example` versionné avec des valeurs factices ; revue de `git log -p` avant chaque jalon |
| ENF9 | Maintenabilité : toute règle RGx est couverte par au moins un test qui la cite | Matrice de traçabilité (SPECIFICATIONS §6) |
| ENF10 *(v2)* | Mots de passe stockés hachés (BCrypt), jamais en clair ni dans les logs | Lecture de la table `utilisateur` ; revue de code |
| ENF11 *(v2)* | Session serveur : cookie `JSESSIONID` HttpOnly, SameSite=Strict ; déconnexion = invalidation côté serveur ; protection CSRF (cookie `XSRF-TOKEN`) sur les routes protégées | Test : après logout, l'ancien cookie renvoie 401 ; POST protégé sans jeton CSRF → 403 |
| ENF12 *(v2)* | OWASP A01 (contrôle d'accès) et A07 (authentification) : chaque route protégée est testée pour « non connecté » (401) et « mauvais rôle » (403) | Tests de sécurité par rôle |
| ENF13 *(v2)* | Pièces jointes : 10 Mo maximum, types autorisés, stockées hors du répertoire web, servies uniquement via l'API avec contrôle d'accès | Tests 413/415/403 ; volume Docker dédié |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session (`expirationAt = ouvertureAt + 15 min`) | Q2 |
| RG2 | Un étudiant ne peut marquer lui-même sa présence que tant que le code est valide. Après expiration, seule la présence manuelle du formateur est possible | Q2, Q3, Q14 |
| RG3 | Un étudiant a au plus une présence par session, quelle que soit la source → `409 DEJA_PRESENT` | CONTRAT, Q14 |
| RG4 | Après 5 codes inconnus consécutifs, l'étudiant est bloqué 2 minutes → `429 TROP_DE_TENTATIVES`. Une tentative réussie remet le compteur à zéro | Q4, [HYP-5] |
| RG5 | Un étudiant ne relit jamais son propre exercice → `403 AUTO_RELECTURE` | Q5, CONTRAT |
| RG6 *(v3)* | ~~Un exercice a au plus un relecteur~~ → Un exercice est relu par **deux pairs différents** (au plus deux relectures, jamais deux fois le même relecteur) | ~~Q6~~ → client, enveloppe étape 3 (#85) |
| RG7 | Le relecteur est tiré au hasard par le système parmi les étudiants **présents à la session de l'exercice**, auteur exclu | Q7, [HYP-3] |
| RG8 | L'auteur voit la note et le commentaire, jamais l'identité du relecteur | Q8 |
| RG9 | Une note est un entier de 0 à 20 inclus → sinon `400 NOTE_INVALIDE` | Q9, CONTRAT |
| RG10 | Une relecture rendue est définitive : un deuxième envoi renvoie `409 RELECTURE_DEJA_RENDUE` | Q15, CONTRAT (contradiction avec Q10 tranchée en §7) |
| RG11 | Un exercice non relu garde le statut « en attente » et reste visible par le formateur | Q11 |
| RG12 | Le dépôt d'un exercice est possible, y compris après l'expiration du code, jusqu'à la clôture de la session | Q12 |
| RG13 | Un étudiant dépose au plus un exercice par session → `409 EXERCICE_DEJA_DEPOSE` | CONTRAT |
| RG14 | Le lien d'un exercice peut être remplacé tant que l'exercice n'est pas `RELU` et que la session n'est pas clôturée | Q13, [HYP-4] |
| RG15 | Une présence ajoutée par le formateur porte `source = FORMATEUR` ; elle est possible jusqu'à la clôture | Q14, CONTRAT |
| RG16 *(v3)* | La **note retenue** d'un exercice est la moyenne des notes rendues par ses relecteurs ; la moyenne d'un étudiant = moyenne arithmétique des notes retenues de ses exercices (provisoires comprises), arrondie à 2 décimales, `null` s'il n'a aucune note. Elle est calculée par l'API uniquement | Q16, F3, enveloppe (#85) |
| RG17 | Un lien d'exercice est une URL absolue `http` ou `https` → sinon `400 LIEN_INVALIDE` | CONTRAT |
| RG18 | Une session clôturée n'accepte plus aucune écriture (présence, dépôt, remplacement, relecture) → `409 SESSION_CLOTUREE` | Q10, Q12, [HYP-1] |
| RG19 | Un étudiant ne peut agir que sur les sessions de sa promotion → `400 ETUDIANT_HORS_PROMOTION` | [HYP-7] |
| RG20 | Le code est unique parmi les sessions dont le code n'a pas expiré | [HYP-6] |
| RG21 *(v2)* | La présence est **validée automatiquement** : un code valide soumis par l'étudiant crée immédiatement la présence (source ETUDIANT) ; aucune validation par le formateur n'existe | PO 25/09 (confirme SF-3) |
| RG22 *(v2)* | Sont publiques : les 5 opérations imposées, la connexion, et les deux listes de sélection `GET /api/promotions` et `GET /api/promotions/{id}/etudiants` (identifiants et noms seulement, nécessaires pour appeler les opérations imposées sans session). Toute autre route exige une session, sinon 401 NON_AUTHENTIFIE | PO 25/09, B2 |
| RG23 *(v2)* | Un compte `admin` existe par défaut ; tant que son mot de passe initial n'est pas changé, toute route autre que profil, changement de mot de passe et déconnexion renvoie 403 CHANGEMENT_MOT_DE_PASSE_REQUIS | PO 25/09, [HYP-18] |
| RG24 *(v2)* | Un mot de passe fait au moins 8 caractères ; 5 échecs de connexion consécutifs bloquent le compte 2 minutes (même logique que RG4) | [HYP-17] |
| RG25 *(v2)* | Un utilisateur n'accède qu'aux ressources de son rôle (§2 bis) → sinon 403 ACCES_REFUSE | PO 25/09 |
| RG26 *(v2)* | Un formateur n'agit que sur les promotions auxquelles il est rattaché | [HYP-14] |
| RG27 *(v2)* | L'identifiant de connexion est unique → 409 LOGIN_DEJA_UTILISE | PO 25/09 |
| RG28 *(v2)* | Un compte ou un étudiant ayant un historique n'est jamais supprimé physiquement : il est désactivé ; un compte désactivé ne se connecte plus (403 COMPTE_DESACTIVE) | [HYP-16] |
| RG29 *(v2)* | Une session ayant des présences ou des exercices ne peut pas être supprimée → 409 SUPPRESSION_IMPOSSIBLE | [HYP-16] |
| RG30 *(v2)* | Une pièce jointe par exercice, 10 Mo maximum, types pdf, zip, txt, md, java, ts, png, jpg ; remplaçable tant que l'exercice n'est pas RELU ; téléchargeable par l'auteur, le relecteur assigné, le formateur de la promotion et l'administrateur | PO 25/09, [HYP-19] |
| RG31 *(v3)* | Tant qu'un seul des deux relecteurs a rendu, sa note est affichée comme note retenue **marquée provisoire** ; elle devient définitive quand les deux ont rendu (exercice `RELU`) | client, enveloppe étape 3 (#85) |

## 7. Zones d'ombre, hypothèses et contradictions

### 7.1 Contradictions relevées

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** (« le relecteur peut corriger sa note tant que la session n'est pas clôturée ») ↔ **Q15** (« une fois validée, c'est fini, il ne peut plus y revenir ») | **Q15** : relecture définitive (RG10) | 1) Le contrat imposé, qui fait foi (B2), prévoit `409 RELECTURE_DEJA_RENDUE` sur `POST /api/relectures/{id}` : un second envoi est une erreur, il n'y a donc pas de correction. 2) Q15 est la réponse la plus explicite et motivée (« plus honnête pour tout le monde »). 3) Une note modifiable après consultation par l'auteur ouvre la porte aux pressions entre pairs. **Conséquence :** aucune opération de modification de relecture. Si le client revient sur ce point, il suffira d'ajouter `PUT /api/relectures/{id}` et de lever RG10 |

### 7.2 Trous et points que la demande ne tranche pas

| Id | Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|---|
| HYP-1 | **« Fin de session » et « clôture » ne sont définies nulle part** (Q3, Q10, Q12), et **aucune opération de clôture** n'existe dans la demande ni dans le contrat | *Trou identifié* | La session a deux états, `OUVERTE` puis `CLOTUREE`. La « fin » au sens de Q3 correspond à l'expiration du code (RG2). La clôture est une action explicite du formateur : `POST /api/sessions/{id}/cloture` | Nouvelle opération dans le contrat ; RG18 |
| HYP-2 | **Identité de l'appelant sur `POST /api/relectures/{id}`** : le corps imposé `{note, commentaire}` ne dit pas qui rend la relecture, alors que `403 AUTO_RELECTURE` suppose de connaître l'appelant | *Trou identifié dans le contrat* | L'identité choisie à l'écran (Q1) est transmise dans l'en-tête `X-Etudiant-Id`. `403 AUTO_RELECTURE` si cet étudiant est l'auteur de l'exercice ; `403 RELECTEUR_NON_ASSIGNE` s'il n'est pas le relecteur assigné. Le corps imposé reste inchangé | En-tête documenté dans le contrat. **À signaler :** sans authentification (Q1), cette identité est déclarative et ne protège pas contre la triche |
| HYP-3 | **Quand le relecteur est-il tiré ?** Et que faire si personne d'autre n'est présent ? (Q7, Q11) | *Non tranché* | Tirage **au dépôt**, parmi les présents de la session auteur exclu. Si aucun candidat : l'exercice reste `DEPOSE` (sans relecteur), il est visible « en attente » (Q11) et le tirage est retenté automatiquement à chaque nouvelle présence enregistrée pour cette session | Les statuts `DEPOSE` → `EN_ATTENTE_RELECTURE` → `RELU` (D4) |
| HYP-4 | **« Tant que personne n'a commencé à le relire » (Q13)** : l'application ne peut pas savoir qu'une lecture a commencé, puisqu'il n'y a pas de brouillon | *Non mesurable tel quel* | « Commencé » est assimilé à « relecture rendue ». Le lien est remplaçable tant que le statut ≠ `RELU` | **Risque signalé :** le relecteur peut lire une version puis noter la suivante. Acceptable en V1 |
| HYP-5 | **Q4 : que compte-t-on comme erreur, et par qui ?** | Q4 | Seul `CODE_INCONNU` compte, par `etudiantId`, sur 5 échecs **consécutifs** ; blocage de 2 min à partir du 5ᵉ échec ; code HTTP `429` ajouté au contrat | Table `tentative_code`. **À signaler :** sans authentification, un étudiant bloqué peut choisir un autre nom (limite connue) |
| HYP-6 | Format et unicité du code | *Non précisé* | 6 caractères parmi `A-Z` et `2-9`, sans `O/0/I/1` ; unique parmi les codes non expirés | RG20, ENF4 |
| HYP-7 | Un étudiant d'une autre promotion peut-il utiliser le code ? | *Non précisé* | Non : `400 ETUDIANT_HORS_PROMOTION`. Le contrat n'autorise que 400/409/410 sur cette opération, d'où l'emploi de 400 | RG19 |
| HYP-8 | Un étudiant **absent** peut-il déposer un exercice ? | Q12 laisse entendre que oui (« pas de connexion le soir même ») | Oui : le dépôt ne dépend pas de la présence. Seul le **relecteur** doit être présent (Q7) | Un absent peut être noté mais ne peut pas relire |
| HYP-9 | Le formateur voit-il le nom du relecteur ? | Q8 ne concerne que l'étudiant relu | Oui, dans la vue des exercices d'une session (EF11) : il doit savoir qui est en retard (Q16, « relectures qu'il doit encore faire ») | Seul le DTO « auteur » masque le relecteur |
| HYP-10 | Le relecteur voit-il le nom de l'auteur ? | *Non précisé* | Non : il ne voit que le lien et le titre de la session. C'est symétrique de Q8 et limite les biais | DTO relecteur sans auteur |
| HYP-11 | Q16 demande « sa présence **à chaque session** », alors que le contrat renvoie un simple entier `presences` | Q16 ↔ CONTRAT | Le contrat imposé fait foi : `presences` est un total. Le détail session par session est hors `v0.1` (Could : `GET /api/sessions/{id}/presences`) | Écart signalé au client |
| HYP-12 | Plusieurs formateurs ? Qui est le formateur ? | Q1 n'en parle pas | Un seul formateur implicite, sans identification | Pas de table `formateur` |
| HYP-13 | Plusieurs sessions ouvertes en même temps pour une promotion ? | *Non précisé* | Autorisé ; le code identifie la session | RG20 |

### 7.2 bis Changement de besoin v2 (25/09, #54) — contradictions, risques et manques

| Id | Point | Décision retenue | Conséquence |
|---|---|---|---|
| — | **Q1 (« pas de mot de passe ») contredite par le PO** | La décision du PO du 25/09 remplace Q1 | Authentification ; HYP-2 ne vaut plus que pour les routes imposées publiques |
| HYP-14 | Quel formateur voit quelles promotions ? (manque détecté) | Table `formateur_promotion` gérée par l'admin | RG26 |
| HYP-15 | Lien entre un compte et une fiche étudiant (manque détecté) | `utilisateur.etudiant_id` (0 ou 1), unique | L'étudiant connecté n'a plus à choisir son nom |
| HYP-16 | Supprimer un compte, un étudiant ou une session ayant un historique détruirait présences et notes | Désactivation (RG28) ; suppression de session refusée si historique (RG29) | Colonne `actif` |
| HYP-17 | Politique de mot de passe non précisée | 8 caractères minimum, blocage 5 échecs / 2 min | RG24 |
| HYP-18 | `admin/admin` est un mot de passe connu de tous (faille) | Changement obligatoire à la première connexion | RG23 ; mot de passe haché dans la migration, jamais en clair |
| HYP-19 | « Dossier ou fichier en plus du lien » alors que le contrat impose `lien` | Pièce jointe **optionnelle**, ajoutée après le dépôt ; dossier = `.zip` | `POST /api/exercices/{id}/fichier` ; le contrat imposé ne change pas |
| RISQUE-1 | Les 5 routes imposées restent publiques : sans session, on peut usurper une identité (`etudiantId`, `X-Etudiant-Id`), ouvrir une session de cours ou lire le tableau d'une promotion | **Avec** une session, tous les contrôles s'appliquent (403 IDENTITE_DIFFERENTE, 403 ACCES_REFUSE) ; le frontend appelle toujours connecté ; à supprimer dès que la contrainte B2 disparaît (passer ces routes derrière la session) | Risque résiduel **accepté par le PO** pour garder B2 |
| — | « Validation automatique des présences » | Déjà le comportement de la v1 (SF-3) ; rendu explicite par RG21 et EF26 | Aucun état « à valider » |
| — | Journal d'audit | Retiré par le PO | Exclu (§3) |

### 7.2 ter Changement de besoin v3 (enveloppe, 25/09, #85) — ce qui devient faux et ce qui est sacrifié

| Élément | Avant | Après | Conséquence |
|---|---|---|---|
| Q6 / RG6 | un relecteur | deux pairs différents | la règle issue de Q6 est **remplacée** par la demande du client |
| RG16 | moyenne des notes rendues | moyenne des notes retenues par exercice | un exercice noté 12 et 16 compte pour 14, pas pour deux notes |
| D2 | `UNIQUE(exercice_id)` | `UNIQUE(exercice_id, relecteur_id)` | migration **V4** ajoutée ; V1 n'est pas modifiée |
| D4 | RELU dès la première note | RELU quand les deux ont rendu | une note seule est **provisoire** (RG31) |

- **Seul présent / un seul candidat (HYP-20)** : un relecteur est assigné tout de suite, le second est tiré à la présence suivante (même mécanisme que HYP-3).
- **Données existantes (HYP-21)** : les exercices `RELU` avec une seule relecture (données v1) restent `RELU` avec cette note ; la règle s'applique aux dépôts postérieurs à V4. Aucune ligne n'est supprimée par la migration.
- **Clôture (Q11)** : un exercice clôturé avec une seule note garde une note retenue provisoire, visible comme telle.
- **Sacrifice de périmètre** : ce `Must` arrive après l'échéance. Sortent du périmètre v1.0 : #63 pièce jointe (V4 lui était réservée ; elle passera en V5 si elle revient), #60/#61/#62 CRUD, #59 menus par rôle côté frontend, Should #30–#33, #35, #36. On garantit d'abord les parcours imposés, corrects avec deux relecteurs.

### 7.3 Questions du client peu utiles au développement

- **Q1** n'avait qu'une conséquence négative (pas d'authentification). *v2 : remplacée par la décision du PO (§7.2 bis).*
- Toutes les autres réponses ont servi : chaque RG ci-dessus cite sa source.

## 8. Contraintes techniques

**Imposées par le sujet :**

| # | Contrainte |
|---|---|
| B1 | Java 17+, Maven, wrapper `mvnw` commité |
| B2 | `api/contrat.yaml` respecté à la lettre (chemins, verbes, codes, format d'erreur) |
| B3 | Couches contrôleur / service / repository ; aucune entité JPA exposée, uniquement des DTO |
| B4 | Validation des entrées (`jakarta.validation`) et erreurs centralisées (`@RestControllerAdvice`) |
| B5 | Schéma versionné par **Flyway** ; `ddl-auto=validate` (`create-drop` toléré en test uniquement) |
| B6 | Un test unitaire sur une règle métier réelle et un test d'intégration sur un endpoint, sans base locale |
| F1 | Framework déclaré et justifié dans le README ; `ng build` passe |
| F2 | Trois écrans : Formateur, Étudiant, Relecteur |
| F3 | Appels API dans une couche dédiée ; états de chargement et d'erreur ; aucune règle métier dupliquée |

**Choix complémentaires :**

- Spring Boot 4.1 (Spring Security 7), Spring Data JPA, springdoc-openapi (comparaison visuelle avec le contrat) ;
- *(v2)* **Spring Security** : session serveur, BCrypt, CSRF par cookie, `@PreAuthorize` par rôle, réponses 401/403 au format `{code, message}` ;
- *(v2)* pièces jointes sur disque dans `UPLOAD_DIR` (variable de `.env`, volume Docker), jamais servies en statique ;
- **PostgreSQL 16** en exécution (Docker), **H2 en mode PostgreSQL** pour les tests (pour tourner sur un poste vierge, B6) ;
- migrations `backend/src/main/resources/db/migration/V{n}__{description}.sql`, jamais modifiées une fois poussées ;
- Angular 17 en composants standalone, structure `core/ features/ shared/ layout/` ;
- `docker-compose.yml` à la racine : postgres, backend, frontend.

## 9. Livrables

| Livrable | Emplacement |
|---|---|
| Cahier des charges (ce document) | `docs/CAHIER_DES_CHARGES.md` |
| Spécifications fonctionnelles, flows et user stories | `docs/SPECIFICATIONS_FONCTIONNELLES.md` |
| Diagrammes D1 à D4 (Mermaid) | `docs/diagrammes/` |
| Plan technique (stack justifiée, qualité Sonar, tests), conventions | `docs/PLAN_TECHNIQUE_EQUIPES.md`, `docs/CONTRIBUTING.md` |
| Planning horodaté par sprint et par équipe | `docs/PLANNING_SPRINTS.md` |
| Coordination et communication entre équipes | `docs/COORDINATION_EQUIPES.md` |
| Système de design et templates visuels | `docs/design/` |
| Backlog | Issues GitHub (source : `docs/BACKLOG.md`) |
| Contrat d'API complété | `api/contrat.yaml` |
| Backend Spring Boot + migrations + tests | `backend/` |
| Frontend Angular | `frontend/` |
| Démarrage | `docker-compose.yml`, `README.md` |
| Historique | `CHANGELOG.md`, `docs/JOURNAL.md` |

## 10. Démarche prévue

| Étape | Objectif | Jalon |
|---|---|---|
| 0 | Dépôt public ; premier commit = `.gitignore` Java + JS | — |
| 1 | Ce cahier, spécifications, diagrammes, contrat figé, issues | `[JALON] analyse` |
| 2 | Stories **Must** uniquement : une branche par issue, une PR par branche | `[JALON] v0.1` |
| 3 | Enveloppe : une issue « bug » et une issue « évolution » séparées ; mise à jour de l'analyse **avant** le code | — |
| 4 | Stories Should restantes, README testé sur clone vierge, CHANGELOG | `[JALON] v1.0` |
| 5 | Épreuve git-lab (dépôt séparé) | — |
| 6 | Soumission | — |

Le détail heure par heure (25/09, sprints S0 à S6, tâche par équipe et heure de livraison) est dans [PLANNING_SPRINTS.md](PLANNING_SPRINTS.md).

**En cas de retard :** les stories Should/Could sont repoussées (label `prio`), jamais la qualité d'une story Must, ses tests ou le journal.

**Definition of Done — un ticket est terminé quand :**

- ses critères d'acceptation sont vérifiés par un test automatisé ou une preuve jointe à la PR (curl/Swagger ou capture) ;
- les RG qu'il cite sont couvertes par un test dont le nom cite la RG ;
- le contrat, les migrations et la documentation touchés sont à jour dans la même PR ;
- la CI est verte et la PR est fusionnée dans `main` avec `Closes #n` ;
- aucune erreur ne renvoie autre chose que `{code, message}`.

---

## Annexe A — Glossaire

| Terme | Définition |
|---|---|
| Promotion | Groupe d'étudiants suivant la formation ensemble |
| Session | Une séance de cours d'une promotion, ouverte par le formateur |
| Code de présence | Chaîne courte, valide 15 min, prouvant qu'on est dans la salle |
| Présence | Fait qu'un étudiant a assisté à une session ; `source` indique qui l'a enregistrée |
| Exercice | Lien (URL) vers le travail d'un étudiant pour une session |
| Relecture | Évaluation d'un exercice par un pair : note entière /20 et commentaire |
| Relecteur | Étudiant assigné à une relecture |
| Clôture | Action du formateur qui fige une session (plus aucune écriture) |
| Tableau | Récapitulatif par étudiant d'une promotion |
| Compte utilisateur *(v2)* | Identifiant + mot de passe haché + rôle ; lié à une fiche étudiant pour le rôle ETUDIANT |
| Rôle *(v2)* | ADMIN, FORMATEUR ou ETUDIANT ; détermine les droits (§2 bis) |
| Session de connexion *(v2)* | État « connecté » conservé par le serveur (cookie JSESSIONID) ; à ne pas confondre avec une **session de cours** |
| Pièce jointe *(v2)* | Fichier ou dossier zippé joint à un exercice, en plus du lien |

## Annexe B — Dictionnaire de données

Correspond à la migration `V1__init.sql` et au diagramme [D2](diagrammes/D2-modele-donnees.md).

| Table | Colonne | Type | Contraintes | Description |
|---|---|---|---|---|
| promotion | id | BIGINT | PK | |
| | nom | VARCHAR(100) | NOT NULL, UNIQUE | ex. « P1-2026 » |
| etudiant | id | BIGINT | PK | |
| | nom | VARCHAR(150) | NOT NULL | Nom affiché dans la liste (Q1) |
| | promotion_id | BIGINT | FK → promotion, NOT NULL | |
| session | id | BIGINT | PK | |
| | titre | VARCHAR(200) | NOT NULL | |
| | promotion_id | BIGINT | FK → promotion, NOT NULL | |
| | code | VARCHAR(6) | NOT NULL | RG20 |
| | ouverture_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| | expiration_at | TIMESTAMP WITH TIME ZONE | NOT NULL | = ouverture_at + 15 min (RG1) |
| | statut | VARCHAR(20) | NOT NULL, `OUVERTE`/`CLOTUREE` | HYP-1 |
| | cloture_at | TIMESTAMP WITH TIME ZONE | NULL | |
| presence | id | BIGINT | PK | |
| | session_id | BIGINT | FK → session | |
| | etudiant_id | BIGINT | FK → etudiant | |
| | source | VARCHAR(10) | `ETUDIANT`/`FORMATEUR` | Q14 |
| | marquee_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| | | | UNIQUE (session_id, etudiant_id) | RG3 |
| tentative_code | etudiant_id | BIGINT | PK, FK → etudiant | RG4 |
| | echecs_consecutifs | INT | NOT NULL, défaut 0 | |
| | bloque_jusqu_a | TIMESTAMP WITH TIME ZONE | NULL | |
| exercice | id | BIGINT | PK | |
| | session_id | BIGINT | FK → session | |
| | auteur_id | BIGINT | FK → etudiant | |
| | lien | VARCHAR(2048) | NOT NULL | RG17 |
| | statut | VARCHAR(25) | `DEPOSE`/`EN_ATTENTE_RELECTURE`/`RELU` | D4 |
| | depose_at, modifie_at | TIMESTAMP WITH TIME ZONE | | |
| | | | UNIQUE (session_id, auteur_id) | RG13 |
| relecture | id | BIGINT | PK | |
| | exercice_id | BIGINT | FK → exercice ; UNIQUE (exercice_id, relecteur_id) *(v3, V4)* | RG6 |
| | relecteur_id | BIGINT | FK → etudiant, NOT NULL | RG7 ; ≠ auteur vérifié par le service (RG5) |
| | note | INT | NULL, CHECK 0–20 | RG9 ; NULL = pas encore rendue |
| | commentaire | TEXT | NULL | |
| | assignee_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| | rendue_at | TIMESTAMP WITH TIME ZONE | NULL | non NULL = définitive (RG10) |
| **utilisateur** *(v2, V3)* | id | BIGINT | PK | |
| | login | VARCHAR(50) | NOT NULL, UNIQUE | RG27 |
| | mot_de_passe_hash | VARCHAR(100) | NOT NULL | BCrypt (ENF10) |
| | role | VARCHAR(10) | `ADMIN`/`FORMATEUR`/`ETUDIANT` | §2 bis |
| | nom_affiche | VARCHAR(150) | NOT NULL | profil |
| | etudiant_id | BIGINT | FK → etudiant, UNIQUE, NULL | HYP-15 ; obligatoire si role = ETUDIANT |
| | actif | BOOLEAN | NOT NULL, défaut TRUE | RG28 |
| | doit_changer_mot_de_passe | BOOLEAN | NOT NULL, défaut FALSE | RG23 |
| | echecs_connexion | INT | NOT NULL, défaut 0 | RG24 |
| | bloque_jusqu_a | TIMESTAMP WITH TIME ZONE | NULL | RG24 |
| | cree_at | TIMESTAMP WITH TIME ZONE | NOT NULL | |
| **formateur_promotion** *(v2, V3)* | utilisateur_id, promotion_id | BIGINT, BIGINT | PK composite, FK | RG26 |
| **etudiant** *(v2, V3)* | actif | BOOLEAN | NOT NULL, défaut TRUE | RG28 |
| **exercice** *(v2, reporté : V5 si réintégré)* | fichier_nom, fichier_type, fichier_taille, fichier_chemin, fichier_depose_at | VARCHAR(255), VARCHAR(100), BIGINT, VARCHAR(500), TIMESTAMP WITH TIME ZONE | NULL | RG30 ; chemin relatif à `UPLOAD_DIR` |

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 2026-09-25 13h | Version initiale (étape 1) |
| 2 | 2026-09-25 15h | Changement de besoin du PO (#54) : authentification, rôles ADMIN/FORMATEUR/ETUDIANT, compte admin par défaut, CRUD par profil, pièce jointe, présence validée automatiquement rendue explicite. Ajouts : §2 bis, EF15–EF26, ENF10–ENF13, RG21–RG30, §7.2 bis, annexe B. Audit retiré. Q1 remplacée. |
| 3 | 2026-09-25 19h | **Enveloppe, étape 3 (#85)** : double relecture. RG6 remplacée (Q6 caduque), RG16 précisée (note retenue), RG31 (note provisoire), EF7/EF9, §7.2 ter (HYP-20, HYP-21, sacrifice de périmètre), dictionnaire (V4). Bug #83 corrigé sans changement d'analyse. |
| 3.1 | 2026-09-25 | **Navigation par boutons et écran relecteur distinct (#104, demande du PO)** : l'espace étudiant devient trois écrans (`/etudiant/presence`, `/etudiant/notes`, `/etudiant/relectures`) pour respecter F2 sans ambiguïté ; toute navigation interne se fait par des boutons, le lien d'un exercice par un bouton « Ouvrir l'exercice ». Aucune règle métier ni endpoint modifié. Détail : spécifications §1.2 et §1.2 bis, DESIGN_SYSTEM §3. |
