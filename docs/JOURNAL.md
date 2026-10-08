# Journal de bord — 230 — nono leonel

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que tu viens de terminer
- **Bloqué** — ce qui t'a coûté du temps, et combien
- **IA** — ce que tu lui as demandé, et **comment tu as vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges (14 EF, 20 RG, 13 hypothèses tracées), spécifications fonctionnelles (14 fiches, flows, user stories en Gherkin, catalogue des codes d'erreur, matrice de traçabilité), diagrammes D1 à D4 en Mermaid, contrat complété (9 opérations ajoutées, les 5 imposées inchangées), conventions d'équipe, modèles d'issue et de PR, backlog de 36 tickets.

**Bloqué :** contradiction Q10/Q15, tranchée en faveur de Q15 parce que le contrat imposé prévoit `409 RELECTURE_DEJA_RENDUE`. Trou dans le contrat : `POST /api/relectures/{id}` ne dit pas qui appelle, alors que `403 AUTO_RELECTURE` l'exige, d'où l'en-tête `X-Etudiant-Id` (HYP-2). Premier dépôt mal nommé (sans le « k ») et commencé par un `[JALON] v0.1` prématuré : je repars sur un dépôt neuf au bon nom. J'y avais d'abord posé un `[JALON] depart` (repris de LISEZ-MOI), mais SUJET n'en prévoit que trois (analyse, v0.1, v1.0) : je l'ai retiré par un unique `push --force-with-lease`, fait avant toute issue ou PR. Le premier commit est désormais le `.gitignore`.

**IA :** Claude a rédigé les documents à partir de SUJET, CLIENT et du contrat. Vérifications : chaque RG relue contre la question Qx qu'elle cite ; contrat validé par `redocly lint` (0 erreur) ; les 12 diagrammes rendus sans erreur par `mermaid-cli` ; D3 comparé code par code au contrat. Les propositions de l'IA sans appui dans le sujet ont été transformées en hypothèses HYP-x ou retirées (ex. équilibrage de charge des relecteurs).

---

## Étape 2 — Première version

**Fait :** backend complet sur le périmètre Must : les 5 opérations imposées (#16 session, #18 présence, #20 dépôt, #21 tirage du relecteur, #23 relecture, #25 tableau) plus la sécurité v2 (connexion, rôles, 401/403), soit 67 tests, avec le Quality Gate SonarQube vert et la CI verte à chaque PR. Frontend Angular (#13) : les 3 écrans imposés (formateur + tableau, étudiant, relecteur), 14 tests. La CI a maintenant deux jobs (backend, frontend).

**Bloqué :** environ 25 min sur des tests qui dépendaient de l'ordre d'exécution (CI rouge sur #69). La base H2 était partagée entre contextes Spring, et le post-processeur `csrf()` des tests modifie le filtre partagé : corrigé par une base par contexte (`random.uuid`). Environ 10 min sur `NODE_ENV=production`, qui faisait sauter les dépendances de développement npm. Retard global sur le frontend : re-priorisation écrite à 16h25 (#13). La connexion frontend par rôle (#59), les CRUD, la pièce jointe et le démarrage Docker (#27) passent en v1.0.

**IA :** Claude a écrit le code et les tests à partir des fiches SF et du contrat. Vérifications : chaque code HTTP et chaque code d'erreur est couvert par un test d'intégration qui compare au contrat ; l'ordre des contrôles de SF-3 a été relu contre D3 ; Sonar a signalé 1 à 3 problèmes à la plupart des PR, tous corrigés avant fusion ; un défaut non vu par l'IA au premier jet a été trouvé et testé (Jackson tronquait la note `12.5` en `12`, contraire à RG9 : note lue en décimal).

---

## Étape 3 — Enveloppe

**Fait :**
- **Bug** : j'ai traduit « un seul des deux étudiants apparaît » en perte de présence. Deux présences simultanées retentaient toutes deux le tirage du relecteur d'un exercice en attente ; la seconde violait `UNIQUE(exercice_id)` et sa transaction, présence comprise, était annulée.
  Ordre suivi : issue #83 avec la reproduction, puis test concurrent **rouge commité seul** (`[409, 201]` au lieu de `[201, 201]`), puis correctif (verrouillage de la ligne de l'exercice), test vert. PR #84.
- **Changement de besoin** (deux relecteurs, moyenne, note provisoire) : issues #85–#88. **Analyse d'abord** (PR #89 : RG6 remplacée, RG16, RG31, §7.2 ter, D2, D4, contrat 2.1). Puis **migration V4 ajoutée**, vérifiée sur une base PostgreSQL déjà remplie (PR #90), puis l'écran « Mes notes » (PR #91). Correctif et évolution sont sur deux branches et deux PR.

**Bloqué :**
- L'enveloppe n'a été vue qu'à 18h50 : je l'avais cherchée sous le nom `enveloppe` et non `EPREUVE_KFOKAM48/enveloppe.md`. L'étape 3 est donc faite **après l'échéance de 18h00**, ce que j'assume.
- Deux présences simultanées donnent maintenant deux relecteurs : le test de régression #83 a été adapté à la nouvelle règle (RG6 v3), pas supprimé.

**IA :** Claude a posé le diagnostic à partir du code (tirage retenté dans la transaction de présence). Je l'ai **prouvé par un test rouge avant toute correction** : le résultat obtenu, `[409, 201]`, correspondait exactement à la prédiction. La migration a été vérifiée sur une vraie base remplie (V3 → V4, lignes conservées, contrainte remplacée), et la requête du tableau sur PostgreSQL, pas seulement sur H2.

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :** la pièce jointe (#63, qui devait prendre la V4), les écrans CRUD (#60–#62), les menus par rôle côté frontend (#59) et les Should #30–#33, #35, #36. La double relecture est un Must qui touche la base, le contrat et le frontend à la fois ; j'ai garanti d'abord que les parcours imposés restent justes avec deux relecteurs. Écrit aussi dans le cahier, §7.2 ter.

---

## Étape 4 — Version finale

**Fait :**
- `CHANGELOG.md` au format Keep a Changelog : chaque entrée renvoie à son issue et à sa PR.
- Backlog restant **trié** avec la raison de chaque rang (`docs/BACKLOG.md`).
- README **testé depuis un clone vierge**, dans un dossier vide et avec ses seules commandes (`git clone` puis `docker compose up --build`) : 4 migrations appliquées, données de démonstration, interface servie, connexion `paul` OK.
- Puis `[JALON] v1.0` et le tag `v1.0.0`.

**Bloqué :**
- Réseau lent : le premier build Docker a pris environ 10 min (images et dépendances Maven). Les suivants utilisent le cache.
- Le dépôt de l'épreuve Git (étape 5) n'est pas faisable : `git-lab.bundle` est introuvable sur le poste.

**IA :** Claude a rédigé le CHANGELOG à partir de `git log --merges`. Je l'ai vérifié en recoupant chaque numéro de PR avec la liste des PR fusionnées renvoyée par l'API GitHub. Le README a été vérifié par une exécution réelle depuis un clone neuf, pas par relecture.

---

## Après la version 1.0 — vers la 1.1 (25/09 au 08/10)

Travail fait après l'échéance, pour finir ce qui avait été sorti du périmètre et corriger ce qui était faux. Le dépôt a donc continué d'évoluer après la soumission.

**Fait :**
- **25 et 26/09** : Swagger UI (#96), code de session masqué aux étudiants (#98), relecture fiable (#101), connexion et menus par rôle (#59). Puis, après un audit des écrans : un composant = quatre fichiers (#103), navigation par boutons et écran relecteur distinct pour F2 (#104), cinq bugs (#105 à #109), analyse et contrat remis à jour (#110, #111).
- **29/09** : fiche désactivée (#113), récapitulatif de l'étudiant (#112), comptes (#60), promotions, formateurs et fiches (#61).
- **05/10, audit de tout ce qui précède** : relecture du code fusionné par les PR #116 à #127, contre le cahier, le contrat et les règles d'équipe. Résultat : **11 défauts bloquants** (5 au backend dont 3 de sécurité, 6 dans l'écran d'administration), une Quality Gate SonarQube non relancée depuis le 25/09 (57 violations au backend, 4 au frontend), et une documentation en retard.
- **07 et 08/10, corrections** : un ticket par défaut (#128 à #141, #149), test rouge commité seul avant chaque correctif. Trois règles décidées et écrites avant d'être codées (RG32 à RG34, #140). Contrat 2.8. Écran d'administration refait en composants, vérifié dans un navigateur à chaque étape. Quality Gate revenue à 0 violation sur les deux projets. Version **1.1.0**.

**Bloqué :**
- **Tests qui passaient sur du faux.** Les écrans #60 et #61 avaient des tests verts qui simulaient des réponses que le serveur ne renvoie pas (un champ `actif` inexistant, une liste de formateurs jamais lue). À l'écran réel : toutes les fiches affichées « désactivé », et rattacher un formateur détachait tous les autres. Leçon : un test d'écran doit simuler la réponse **du contrat**, et l'écran doit être ouvert au moins une fois dans un navigateur avant la fusion.
- **Tests rouges réécrits dans le commit du correctif** (#105, #109, #60) : l'assertion gênante avait été relâchée au lieu d'être satisfaite. Depuis l'audit, un test rouge n'est plus modifié ; quand il doit l'être, la PR dit quoi et pourquoi.
- **SonarQube sur un réseau privé** : injoignable une partie du temps. Neuf PR ont été fusionnées sans analyse, avec la mention écrite dans chacune, puis l'analyse a été rattrapée (#128, puis PR #159) : elle a encore trouvé deux violations dans mon propre code.
- **Un test dépendait de l'ordre d'exécution** (tableau de démonstration) : vert en CI, rouge sur mon poste. Corrigé en lui donnant une base neuve.
- **Historique réécrit le 07/10.** Des lignes de co-auteur ajoutées automatiquement par les outils figuraient dans 13 messages de commit. Je les ai retirées (messages seulement, contenu identique, auteurs et dates inchangés), ce qui a demandé un `push --force` sur `main` : c'est le seul depuis la recréation du dépôt, et je l'assume. Une sauvegarde de l'ancien historique est conservée hors du dépôt.

**IA :** deux assistants ont servi sur cette période : Claude (25 et 26/09, puis l'audit et les corrections à partir du 05/10) et Codebuff (26 au 29/09). Ce que j'ai appris en les faisant se relire l'un l'autre :
- Un assistant produit vite du code et des tests **cohérents entre eux**, pas forcément avec le serveur : les tests de #60 et #61 validaient ses propres suppositions. La vérification qui a trouvé les défauts n'est pas une relecture, c'est l'exécution : requêtes réelles, navigateur réel, `./mvnw verify`, SonarQube.
- Chaque défaut de l'audit a été **prouvé par un test rouge avant d'être corrigé** (par exemple `Status expected:<403> but was:<200>` pour le formateur qui s'approprie un étudiant), jamais corrigé sur la seule foi du diagnostic.
- Les trois règles nouvelles (déplacement d'une fiche, fiche désactivée et tirage, fiche et compte) ne sont pas des choix de l'IA : elle a posé la question, j'ai tranché, la décision est dans le cahier §7.2 quater.
- Les annonces « tests verts » et « Quality Gate OK » des descriptions de PR ne valent que si la commande a tourné : la Quality Gate affichée du 26 au 29/09 portait sur d'anciens commits.

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
