-- Enveloppe, étape 3 (#85, #87) : chaque exercice est relu par deux pairs différents (RG6 v3).
-- Migration AJOUTÉE : V1 n'est pas modifiée. Aucune ligne supprimée : les relectures existantes
-- (un relecteur par exercice) satisfont la nouvelle contrainte (HYP-21).
ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;
ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);
