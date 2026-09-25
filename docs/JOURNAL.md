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

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
