package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.DepotExerciceRequete;
import com.k48.leonel.presence48.dto.response.ExerciceDeposeReponse;
import com.k48.leonel.presence48.service.ExerciceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

  private final ExerciceService exercices;

  public ExerciceController(ExerciceService exercices) {
    this.exercices = exercices;
  }

  /** Opération imposée (EF6) : publique sans session, identité vérifiée si l'appelant est connecté. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExerciceDeposeReponse deposer(@Valid @RequestBody DepotExerciceRequete corps) {
    return exercices.deposer(corps.sessionId(), corps.etudiantId(), corps.lien());
  }
}
