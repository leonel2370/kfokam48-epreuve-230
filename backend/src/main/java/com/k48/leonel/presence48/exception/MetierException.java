package com.k48.leonel.presence48.exception;

import org.springframework.http.HttpStatus;

/**
 * Violation d'une règle métier. Le service la lève ; le GlobalExceptionHandler la traduit en
 * {@link ErreurReponse} avec le statut HTTP prévu par le contrat. Aucune stack trace n'est renvoyée.
 */
public class MetierException extends RuntimeException {

  private final HttpStatus statut;
  private final String code;

  public MetierException(HttpStatus statut, String code, String message) {
    super(message);
    this.statut = statut;
    this.code = code;
  }

  public HttpStatus getStatut() {
    return statut;
  }

  public String getCode() {
    return code;
  }
}
