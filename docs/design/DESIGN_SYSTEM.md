# Système de design — PRESENCE48 (shadcn/ui pour Angular via spartan/ui)

**Public :** équipe Design (propriétaire) et équipe Frontend (consommatrice).
**Périmètre de l'équipe Design :** stack design et templates visuels. Pas de code applicatif.
**Rappel du sujet :** le rendu visuel n'est pas noté. Ce système sert la **cohérence** et la **lisibilité mobile** (ENF1), pas l'esthétique.

## 1. Stack design et justification

| Élément | Choix | Pourquoi |
|---|---|---|
| Référence visuelle | **shadcn/ui**, thème *zinc* | Sobre, lisible, très répandu ; tokens publics et documentés |
| Implémentation Angular | **spartan/ui** (`@spartan-ng/brain` + `helm`) | shadcn/ui n'existe qu'en React. spartan/ui en est le portage Angular : *brain* = primitives accessibles sans style (sur Angular CDK), *helm* = styles shadcn copiés dans le projet, donc modifiables |
| Styles | **Tailwind CSS 3** | Utilisé par shadcn et spartan ; les tokens deviennent des classes (`bg-primary`) |
| Icônes | **lucide-angular** | Jeu d'icônes officiel de shadcn |
| Police | **Inter** (repli : system-ui) | Police de shadcn, très lisible en petite taille |
| Maquettes | **Templates HTML statiques** dans `docs/design/templates/` | Versionnés et diffables comme le reste ; mêmes classes Tailwind que le futur code, donc aucun écart de traduction. Figma est possible en complément, jamais comme source de vérité |

## 2. Tokens

Valeurs HSL de shadcn/ui, thème *zinc*. À copier tels quels dans `frontend/src/styles.css`.

| Token | Clair | Sombre | Usage |
|---|---|---|---|
| `--background` | `0 0% 100%` | `240 10% 3.9%` | Fond de page |
| `--foreground` | `240 10% 3.9%` | `0 0% 98%` | Texte principal |
| `--card` | `0 0% 100%` | `240 10% 3.9%` | Cartes |
| `--primary` | `240 5.9% 10%` | `0 0% 98%` | Bouton principal |
| `--primary-foreground` | `0 0% 98%` | `240 5.9% 10%` | Texte sur primaire |
| `--secondary` / `--muted` / `--accent` | `240 4.8% 95.9%` | `240 3.7% 15.9%` | Fonds secondaires |
| `--muted-foreground` | `240 3.8% 46.1%` | `240 5% 64.9%` | Texte secondaire |
| `--destructive` | `0 84.2% 60.2%` | `0 62.8% 30.6%` | Erreurs, clôture |
| `--border` / `--input` | `240 5.9% 90%` | `240 3.7% 15.9%` | Bordures, champs |
| `--ring` | `240 5.9% 10%` | `240 4.9% 83.9%` | Focus clavier |
| `--radius` | `0.5rem` | | Arrondi de base |

Couleurs **métier** (ajout PRESENCE48, pour les badges de statut) : succès `142 71% 45%` (RELU, présent), attente `38 92% 50%` (EN_ATTENTE_RELECTURE, retard), neutre = `muted` (DEPOSE).

**Typographie :** `text-sm` 14 px (tableaux, aides) · `text-base` 16 px (corps, champs : jamais moins de 16 px sur mobile, sinon iOS zoome) · `text-lg` · `text-2xl` (titres de page) · `text-4xl` mono (code de présence).
**Espacement :** multiples de 4 px (`gap-2`, `p-4`, `p-6`). **Points de rupture :** mobile d'abord à 360 px, `sm` 640, `md` 768, `lg` 1024.

## 3. Composants et correspondance avec les écrans

| Composant spartan (helm) | Écrans | Variantes et états |
|---|---|---|
| Button | tous | `default`, `secondary`, `outline`, `destructive` (clôturer), `ghost` ; états hover, focus-visible (anneau `ring`), disabled, chargement (spinner + libellé « Envoi… ») |
| Input, Label | présence, dépôt, relecture | défaut, focus, erreur (bordure `destructive` + message sous le champ, `aria-describedby`) |
| Select | identification, présence manuelle | défaut, vide (« Aucun étudiant ») |
| Card | tous | titre, description, contenu, pied |
| Table | tableau, exercices de session | en-tête collant, lignes compactes, défilement horizontal sur mobile |
| Badge | statuts | `DEPOSE` neutre · `EN_ATTENTE_RELECTURE` attente · `RELU` succès · `FORMATEUR` contour |
| Alert | tous | `destructive` pour `{code, message}` de l'API ; affiche `message`, `code` en petit pour le support |
| Skeleton | listes et tableau | état de chargement |
| Dialog (alert-dialog) | relecture, clôture | confirmation d'une action **définitive** (RG10, RG18) |
| Toast (sonner) | après succès | « Présence enregistrée », disparition automatique |

**États obligatoires de chaque écran :** chargement (Skeleton) · erreur (Alert avec le `message` de l'API, jamais un texte inventé côté client) · vide (texte explicatif et action suivante) · données.

## 4. Accessibilité (WCAG 2.1 AA)

- Contraste ≥ 4.5:1 : les tokens *zinc* le respectent ; ne jamais écrire en `muted-foreground` sur fond `muted` en dessous de 14 px.
- Chaque champ a un `<label>` ; les erreurs sont reliées par `aria-describedby` et annoncées (`role="alert"`).
- Navigation complète au clavier ; focus toujours visible (`focus-visible:ring-2`).
- Cibles tactiles ≥ 44 × 44 px sur l'écran Étudiant.
- Le statut n'est jamais porté par la couleur seule : le badge porte toujours le libellé.

## 5. Templates visuels

| Template | Écran (F2) | Contenu |
|---|---|---|
| [formateur-session.html](templates/formateur-session.html) | Formateur | Ouvrir une session, code et expiration, exercices et statuts, présence manuelle, clôture |
| [etudiant.html](templates/etudiant.html) | Étudiant (mobile 360 px) | Identité, saisie du code, dépôt, note reçue |
| [relecteur.html](templates/relecteur.html) | Relecteur | Relectures à faire, formulaire de note, confirmation « définitif » |
| [tableau.html](templates/tableau.html) | Formateur | Tableau de la promotion, moyenne « — », retards signalés |

Chaque template montre ses états chargement / erreur / vide dans une section « États ». Ouvrir le fichier dans un navigateur ; Tailwind est chargé par CDN **pour la maquette uniquement**. Le frontend l'installe par npm.

## 6. Processus Design

1. Ticket `[Maquette] …` (label `team :: design`).
2. Template HTML mis à jour et vérifié à 360 px et 1280 px.
3. PR avec captures ; relecture par un membre de l'équipe Frontend.
4. **Handoff :** tokens et classes repris tels quels ; toute évolution visuelle repasse par un template.
