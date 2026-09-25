package com.k48.leonel.presence48.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduction centralisée de toutes les erreurs au format {code, message} (B4, ENF3).
 * Le catalogue des codes est dans docs/SPECIFICATIONS_FONCTIONNELLES.md §5.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  static final String CHAMP_MANQUANT = "Un champ obligatoire est manquant ou mal formé.";

  @ExceptionHandler(MetierException.class)
  ResponseEntity<ErreurReponse> metier(MetierException e) {
    return reponse(e.getStatut(), e.getCode(), e.getMessage());
  }

  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      HandlerMethodValidationException.class,
      HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class,
      MissingRequestHeaderException.class,
      MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ErreurReponse> entreeInvalide(Exception e) {
    LOG.debug("Entrée invalide : {}", e.getMessage());
    return reponse(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT", CHAMP_MANQUANT);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErreurReponse> introuvable(NoResourceFoundException e) {
    return reponse(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE", "Adresse inconnue.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErreurReponse> methode(HttpRequestMethodNotSupportedException e) {
    return reponse(HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_AUTORISEE",
        "Cette opération n'est pas autorisée sur cette adresse.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErreurReponse> typeMedia(HttpMediaTypeNotSupportedException e) {
    return reponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FORMAT_NON_SUPPORTE",
        "Le corps de la requête doit être au format JSON.");
  }

  /** Dernier rempart des contraintes UNIQUE (courses, double clic) : les services lèvent d'abord leur code précis. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErreurReponse> integrite(DataIntegrityViolationException e) {
    LOG.warn("Contrainte d'intégrité violée : {}", e.getMostSpecificCause().getMessage());
    return reponse(HttpStatus.CONFLICT, "CONFLIT", "L'opération entre en conflit avec des données existantes.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErreurReponse> inattendue(Exception e) {
    LOG.error("Erreur inattendue", e);
    return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Une erreur inattendue est survenue.");
  }

  private static ResponseEntity<ErreurReponse> reponse(HttpStatus statut, String code, String message) {
    return ResponseEntity.status(statut).body(new ErreurReponse(code, message));
  }
}
