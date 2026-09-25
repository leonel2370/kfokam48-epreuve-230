package com.k48.leonel.presence48.dto.response;

/** Schéma RelectureRelecteur du contrat : jamais l'identité de l'auteur (HYP-10). */
public record RelectureRelecteurReponse(Long id, Long exerciceId, String sessionTitre, String lien, boolean rendue,
    Integer note, String commentaire) {
}
