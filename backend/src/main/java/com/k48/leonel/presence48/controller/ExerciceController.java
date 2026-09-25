package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.DepotExerciceRequete;
import com.k48.leonel.presence48.dto.response.ExerciceAuteurReponse;
import com.k48.leonel.presence48.dto.response.ExerciceDeposeReponse;
import com.k48.leonel.presence48.service.ExerciceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExerciceController {

  private final ExerciceService exercices;

  public ExerciceController(ExerciceService exercices) {
    this.exercices = exercices;
  }

  /** Opération imposée (EF6) : publique sans session, identité vérifiée si l'appelant est connecté. */
  @PostMapping("/api/exercices")
  @ResponseStatus(HttpStatus.CREATED)
  public ExerciceDeposeReponse deposer(@Valid @RequestBody DepotExerciceRequete corps) {
    return exercices.deposer(corps.sessionId(), corps.etudiantId(), corps.lien());
  }

  /** SF-14 (complément, protégé) : note retenue et provisoire (contrat 2.1), sans les relecteurs. */
  @GetMapping("/api/etudiants/{etudiantId}/exercices")
  public List<ExerciceAuteurReponse> mesExercices(@PathVariable Long etudiantId) {
    return exercices.exercicesDe(etudiantId);
  }
}
