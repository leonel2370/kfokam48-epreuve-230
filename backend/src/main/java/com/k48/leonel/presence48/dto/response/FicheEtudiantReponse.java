package com.k48.leonel.presence48.dto.response;

/**
 * Contrat 2.8 (#134) : components/schemas/FicheEtudiant — fiche vue par ceux qui la gèrent, avec son
 * état (RG28) et l'identifiant du compte lié, null si la fiche n'a pas de compte (HYP-15).
 */
public record FicheEtudiantReponse(Long id, String nom, Long promotionId, boolean actif, String compteLogin) {
}
