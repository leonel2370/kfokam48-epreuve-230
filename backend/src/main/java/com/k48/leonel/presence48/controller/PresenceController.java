package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.PresenceRequete;
import com.k48.leonel.presence48.dto.response.PresenceReponse;
import com.k48.leonel.presence48.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PresenceController {

  private final PresenceService presences;

  public PresenceController(PresenceService presences) {
    this.presences = presences;
  }

  /** Opération imposée (EF3) : publique sans session, identité vérifiée si l'appelant est connecté. */
  @PostMapping("/api/presences")
  @ResponseStatus(HttpStatus.CREATED)
  public PresenceReponse marquer(@Valid @RequestBody PresenceRequete corps) {
    return presences.marquer(corps.code(), corps.etudiantId());
  }
}
