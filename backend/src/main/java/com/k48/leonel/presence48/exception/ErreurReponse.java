package com.k48.leonel.presence48.exception;

/**
 * Corps unique de toutes les erreurs de l'API (contrat : components/schemas/Erreur).
 *
 * @param code identifiant stable en majuscules, ex. CODE_EXPIRE
 * @param message phrase lisible, en français
 */
public record ErreurReponse(String code, String message) {
}
