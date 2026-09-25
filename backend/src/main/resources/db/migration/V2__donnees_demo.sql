-- V2 : données de démonstration (ENF5). Le correcteur ouvre une application déjà peuplée.
-- Pas d'identifiants forcés : les séquences IDENTITY restent cohérentes (PostgreSQL et H2).
-- Une session CLÔTURÉE et notée alimente le tableau ; les sessions ouvertes se créent en direct
-- (le code expire 15 min après l'ouverture, RG1).

INSERT INTO promotion (nom) VALUES ('P1-2026'), ('P2-2026');

INSERT INTO etudiant (nom, promotion_id)
SELECT e.nom, p.id
FROM (VALUES ('Awa Ndiaye'), ('Paul Mbarga'), ('Lina Kamga'), ('Marc Tchoua'),
             ('Sara Ebode'), ('Yann Fotso'), ('Ines Ngono'), ('Hugo Talla')) AS e(nom)
CROSS JOIN promotion p WHERE p.nom = 'P1-2026';

INSERT INTO etudiant (nom, promotion_id)
SELECT e.nom, p.id
FROM (VALUES ('Nora Bella'), ('Eric Manga'), ('Lea Owona'), ('Theo Abena')) AS e(nom)
CROSS JOIN promotion p WHERE p.nom = 'P2-2026';

-- Session passée, clôturée
INSERT INTO session (titre, promotion_id, code, ouverture_at, expiration_at, statut, cloture_at)
SELECT 'TP Flyway et migrations', p.id, 'DEMO01',
       TIMESTAMP WITH TIME ZONE '2026-09-18 09:00:00+00', TIMESTAMP WITH TIME ZONE '2026-09-18 09:15:00+00',
       'CLOTUREE', TIMESTAMP WITH TIME ZONE '2026-09-18 18:00:00+00'
FROM promotion p WHERE p.nom = 'P1-2026';

-- Présences : 5 par code, 1 ajoutée par le formateur (Q14)
INSERT INTO presence (session_id, etudiant_id, source, marquee_at)
SELECT s.id, e.id, x.source, TIMESTAMP WITH TIME ZONE '2026-09-18 09:05:00+00'
FROM (VALUES ('Awa Ndiaye', 'ETUDIANT'), ('Paul Mbarga', 'ETUDIANT'), ('Lina Kamga', 'ETUDIANT'),
             ('Sara Ebode', 'ETUDIANT'), ('Yann Fotso', 'ETUDIANT'), ('Marc Tchoua', 'FORMATEUR')) AS x(nom, source)
JOIN etudiant e ON e.nom = x.nom
JOIN session s ON s.code = 'DEMO01';

-- Exercices : relus, en attente de relecture, et un déposé par un absent (HYP-8)
INSERT INTO exercice (session_id, auteur_id, lien, statut, depose_at)
SELECT s.id, e.id, x.lien, x.statut, TIMESTAMP WITH TIME ZONE '2026-09-18 10:00:00+00'
FROM (VALUES ('Awa Ndiaye', 'https://github.com/awa/tp-flyway', 'RELU'),
             ('Paul Mbarga', 'https://github.com/paul/tp-flyway', 'RELU'),
             ('Lina Kamga', 'https://github.com/lina/tp-flyway', 'EN_ATTENTE_RELECTURE'),
             ('Sara Ebode', 'https://github.com/sara/tp-flyway', 'RELU'),
             ('Ines Ngono', 'https://github.com/ines/tp-flyway', 'EN_ATTENTE_RELECTURE')) AS x(nom, lien, statut)
JOIN etudiant e ON e.nom = x.nom
JOIN session s ON s.code = 'DEMO01';

-- Relectures : relecteur présent et différent de l'auteur (RG5, RG7) ; deux non rendues (Q11)
INSERT INTO relecture (exercice_id, relecteur_id, note, commentaire, assignee_at, rendue_at)
SELECT ex.id, r.id, x.note, x.commentaire, TIMESTAMP WITH TIME ZONE '2026-09-18 10:00:00+00',
       CASE WHEN x.note IS NULL THEN NULL ELSE TIMESTAMP WITH TIME ZONE '2026-09-18 16:00:00+00' END
FROM (VALUES ('Awa Ndiaye', 'Paul Mbarga', 14, 'Bon découpage, penser aux tests d''intégration.'),
             ('Paul Mbarga', 'Lina Kamga', 11, 'Migration correcte, nommage à revoir.'),
             ('Sara Ebode', 'Awa Ndiaye', 16, 'Très clair.'),
             ('Lina Kamga', 'Yann Fotso', CAST(NULL AS INT), CAST(NULL AS TEXT)),
             ('Ines Ngono', 'Marc Tchoua', CAST(NULL AS INT), CAST(NULL AS TEXT))) AS x(auteur, relecteur, note, commentaire)
JOIN etudiant a ON a.nom = x.auteur
JOIN etudiant r ON r.nom = x.relecteur
JOIN session s ON s.code = 'DEMO01'
JOIN exercice ex ON ex.session_id = s.id AND ex.auteur_id = a.id;
